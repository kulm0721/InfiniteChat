package com.shanyangcode.redpacketservice.service;

import com.shanyangcode.redpacketservice.model.dto.RedPacketSendRequest;
import com.shanyangcode.redpacketservice.model.vo.RedPacketSendVO;

public interface RedPacketService {

    /**
     * 发送红包
     *
     * @param request 红包发送请求
     * @return 红包发送结果（包含红包 ID 和消息 ID）
     */
    RedPacketSendVO sendRedPacket(RedPacketSendRequest request);

    /**
     * 处理红包过期（Kafka 消费者调用）
     *
     * @param redPacketId 红包 ID
     */
    void handleRedPacketExpiration(Long redPacketId);
}
