package com.shanyangcode.offlinedataservice.client;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.shanyangcode.common.model.dto.MessageBody;
import com.shanyangcode.common.model.vo.MessageResponse;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@AllArgsConstructor
@Slf4j
@SuppressWarnings("BusyWait")
public class CanalClient implements CommandLineRunner {

    private static final int MAX_RETRY_TIMES = 5; // 发生错误后最大重试次数
    private static final long INITIAL_RETRY_DELAY = 1000; // 初始重试延迟1秒
    private static final long MAX_RETRY_DELAY = 60000; // 最大重试延迟60秒
    private static final long HEARTBEAT_INTERVAL = 30000; // 30秒发送一次心跳
    private static final long IDLE_CHECK_INTERVAL = 5000; // 5秒检查一次空闲状态
    // 需要监听的表名集合
    private static final Set<String> MONITOR_TABLES = Set.of("infinitechat.message");
    @Resource
    private CanalConnector canalConnector;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void run(String... args) {
        new Thread(this::process).start();
    }

    public void process() {
        log.info("====== Canal 消费线程已启动 ======");

        int batchSize = 1000;                          // 每批拉取条数
        int retryTimes = 0;                            // 已重试次数
        long retryDelay = INITIAL_RETRY_DELAY;         // 本次重试延迟
        long lastActiveTime = System.currentTimeMillis(); // 最近活跃时间

        while (true) {
            try {
                // 检查连接状态
                if (!canalConnector.checkValid()) {
                    reconnectCanal();
                    retryTimes = 0;
                    retryDelay = INITIAL_RETRY_DELAY;
                    lastActiveTime = System.currentTimeMillis();
                }

                // 检查是否需要发送心跳
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastActiveTime > HEARTBEAT_INTERVAL) {
                    sendHeartBeat();
                    lastActiveTime = currentTime;
                    continue;
                }

                Message message = canalConnector.getWithoutAck(batchSize);
                long batchId = message.getId();
                int size = message.getEntries().size();

                if (batchId == -1 || size == 0) {
                    Thread.sleep(IDLE_CHECK_INTERVAL);
                    continue;
                }
                lastActiveTime = System.currentTimeMillis();
                try {
                    handleMessage(message.getEntries());
                    canalConnector.ack(batchId);
                    retryTimes = 0;
                    retryDelay = INITIAL_RETRY_DELAY;
                } catch (Exception e) {
                    log.error("处理消息内容出错，尝试回滚", e);
                    safeRollback(batchId);
                    throw e;
                }
            } catch (Exception e) {
                log.error("处理canal消息出错", e);
                if (retryTimes++ >= MAX_RETRY_TIMES) {
                    log.error("达到最大重试次数{}，等待后重新尝试", MAX_RETRY_TIMES);
                    retryTimes = 0;
                    try {
                        Thread.sleep(MAX_RETRY_DELAY);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                    continue;
                }
                long sleepTime = Math.min(retryDelay * 2, MAX_RETRY_DELAY);
                log.warn("{}秒后尝试第{}次重连...", sleepTime / 1000, retryTimes);
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                retryDelay = sleepTime;
            }

        }

    }

    private void reconnectCanal() {
        try {
            canalConnector.disconnect();
            canalConnector.connect();
            canalConnector.subscribe();
            log.info("成功重新连接到Canal服务器");
        } catch (Exception e) {
            log.error("连接Canal服务器失败", e);
            throw e;
        }
    }

    private void sendHeartBeat() {
        try {
            canalConnector.ack(-1);
            log.debug("发送心跳保持连接");
        } catch (Exception e) {
            log.error("发送心跳失败", e);
            try {
                if (canalConnector.checkValid()) {
                    canalConnector.disconnect();
                }
            } catch (Exception ex) {
                log.error("断开连接出错", ex);
            }
        }
    }

    private void handleMessage(List<CanalEntry.Entry> entries) {
        for (CanalEntry.Entry entry : entries) {
            if (entry.getEntryType() == CanalEntry.EntryType.TRANSACTIONBEGIN || entry.getEntryType() == CanalEntry.EntryType.TRANSACTIONEND) {
                continue;
            }

            CanalEntry.RowChange rowChange;

            try {
                rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
            } catch (Exception e) {
                throw new RuntimeException("解析binlog事件错误", e);
            }
            String schemaName = entry.getHeader().getSchemaName();          //库名
            String tableName = entry.getHeader().getTableName();            //表名
            String fullTableName = schemaName + "." + tableName;            //库民.表名

            log.info("====== 收到变更: fullTableName={} ======", fullTableName);

            if (!MONITOR_TABLES.contains(fullTableName)) {
                continue;
            }

            System.out.println("表名 ：" + tableName);
            CanalEntry.EventType eventType = rowChange.getEventType();

            log.info("======> binlog[{}:{}], name[{},{}], eventType: {}", entry.getHeader().getLogfileName(), entry.getHeader().getLogfileOffset(), schemaName, tableName, eventType);

            for (CanalEntry.RowData rowData : rowChange.getRowDatasList()) {
                if (rowChange.getEventType() == CanalEntry.EventType.INSERT) {
                    handleInsert(rowData.getAfterColumnsList(), tableName);
                }
            }
        }
    }

    private void handleInsert(List<CanalEntry.Column> columns, String tableName) {
        Map<String, String> map = new HashMap<>();
        for (CanalEntry.Column column : columns) {
            map.put(column.getName(), column.getValue());
        }
        log.info("表名：{}，数据：{}", tableName, map);

        //1.构建完整的消息对象
        MessageResponse messageResponse = bulidMessageFromMap(map);
        log.info("消息体：{}", messageResponse);
    }

    private MessageResponse bulidMessageFromMap(Map<String, String> map) {
        MessageResponse messageResponse = new MessageResponse();
        messageResponse.setSessionId(Long.valueOf(map.get("session_id")));
        messageResponse.setSenderId(Long.valueOf(map.get("sender_id")));
        messageResponse.setMessageId(Long.valueOf(map.get("message_id")));
        messageResponse.setType(Integer.valueOf(map.get("type")));
        messageResponse.setSessionType(Integer.valueOf(map.get("session_type")));
        messageResponse.setCreatedTime(map.get("created_time"));

        MessageBody body = new MessageBody();
        body.setContent(map.get("content"));
        if (StringUtils.isEmpty(map.get("reply_id"))) {
            body.setReplyId(null);
        } else {
            body.setReplyId(Long.valueOf(map.get("reply_id")));
        }
        messageResponse.setBody(body);
        return messageResponse;
    }

    private void safeRollback(long batchId) {
        try {
            canalConnector.rollback(batchId);
        } catch (Exception ex) {
            log.error("回滚canal消息出错", ex);
            try {
                if (canalConnector.checkValid()) {
                    canalConnector.disconnect();
                }
            } catch (Exception ex2) {
                log.error("断开连接错误", ex2);
            }
        }
    }
}
