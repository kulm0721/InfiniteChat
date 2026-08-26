package com.shanyangcode.redpacketservice.service;

import com.shanyangcode.redpacketservice.model.dto.RedPacketReceiveRequest;
import com.shanyangcode.redpacketservice.model.dto.RedPacketSendRequest;
import com.shanyangcode.redpacketservice.model.vo.ReceiveResultVO;
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


    /**
     * 领取红包
     *
     * @param request 红包领取请求
     * @return 领取结果
     */
    ReceiveResultVO receiveRedPacket(RedPacketReceiveRequest request);


    /**
     * 处理红包领取记录（Kafka 消费者调用）
     *
     * @param userId        用户 ID
     * @param redPacketId   红包 ID
     * @param receivedAmount 领取金额
     * @param receiveTime   领取时间
     */
    void handleRedPacketReceive(Long userId, Long redPacketId, Long receivedAmount, Long receiveTime);

    /**
     * 处理红包领取完成（Kafka 消费者调用）
     *
     * @param redPacketId 红包 ID
     */
    void handleRedPacketCompleted(Long redPacketId);
}
