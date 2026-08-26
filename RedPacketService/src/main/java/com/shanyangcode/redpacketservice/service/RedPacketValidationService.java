package com.shanyangcode.redpacketservice.service;


import com.shanyangcode.redpacketservice.model.dto.RedPacketSendRequest;

/**
 * 红包发送校验服务接口
 *
 * 提供红包发送前的权限校验功能
 */
public interface RedPacketValidationService {
    /**
     * 校验红包发送权限
     *
     * 校验内容：
     * 1. 发送者状态校验（封禁/注销用户不能发送）
     * 2. 单聊校验：红包类型（只能普通红包）、接收者状态、好友关系
     * 3. 群聊校验：群成员资格、红包数量不超过群成员数
     *
     * @param request 红包发送请求
     * @throws com.shanyangcode.common.exception.BusinessException 校验失败时抛出业务异常
     */
    void validateSendPermission(RedPacketSendRequest request);
}
