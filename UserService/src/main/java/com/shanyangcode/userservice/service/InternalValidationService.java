package com.shanyangcode.userservice.service;

import com.shanyangcode.common.model.dto.validation.*;

/**
 * 内部校验服务接口
 *
 * 提供供其他微服务 Feign 调用的校验方法
 */
public interface InternalValidationService {

    /**
     * 查询用户状态
     *
     * @param userId 用户 ID
     * @return 用户状态信息
     */
    UserStatusResponse getUserStatus(Long userId);

    /**
     * 获取群成员数量
     *
     * @param sessionId 会话 ID（群聊 ID）
     * @return 群成员数量信息
     */
    GroupMemberCountResponse getGroupMemberCount(Long sessionId);

    /**
     * 验证群成员资格
     *
     * @param userId    用户 ID
     * @param sessionId 会话 ID（群聊 ID）
     * @return 群成员资格信息
     */
    GroupMembershipResponse checkGroupMembership(Long userId, Long sessionId);

    /**
     * 校验单聊消息发送权限
     *
     * 校验内容：
     * 1. 发送者与接收者是否为正常好友关系
     * 2. 发送者是否被接收者拉黑
     * 3. 好友关系是否已删除
     *
     * @param request 校验请求参数
     * @return 校验结果
     */
    MessageValidateResponse validateSingleMessage(SingleMessageValidateRequest request);
}
