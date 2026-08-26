package com.shanyangcode.redpacketservice.service.impl;

import com.shanyangcode.common.common.BaseResponse;
import com.shanyangcode.common.constant.SessionTypeConstant;
import com.shanyangcode.common.constant.UserStateConstant;
import com.shanyangcode.common.enums.ValidationError;
import com.shanyangcode.common.exception.BusinessException;
import com.shanyangcode.common.model.dto.validation.UserStatusResponse;
import com.shanyangcode.redpacketservice.client.UserServiceClient;
import com.shanyangcode.redpacketservice.constant.RedPacketConstant;
import com.shanyangcode.redpacketservice.model.dto.RedPacketSendRequest;
import com.shanyangcode.common.model.dto.validation.GroupMemberCountResponse;
import com.shanyangcode.common.model.dto.validation.GroupMembershipResponse;
import com.shanyangcode.common.model.dto.validation.MessageValidateResponse;
import com.shanyangcode.common.model.dto.validation.SingleMessageValidateRequest;
import com.shanyangcode.redpacketservice.service.RedPacketValidationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import static com.shanyangcode.common.common.ErrorCode.NOT_FOUND_ERROR;
import static com.shanyangcode.common.enums.ValidationError.*;

/**
 * 红包发送校验服务实现
 * <p>
 * 提供红包发送前的权限校验功能
 */
@Service
@Slf4j
public class RedPacketValidationServiceImpl implements RedPacketValidationService {

    @Resource
    private UserServiceClient userServiceClient;

    @Value("${redpacket.validation.enabled:true}")
    private boolean validationEnabled;

    /**
     * 校验红包发送权限
     *
     * @param request 红包发送请求
     * @throws BusinessException 校验失败时抛出
     */
    @Override
    public void validateSendPermission(RedPacketSendRequest request) {
        if (!validationEnabled) {
            log.info("红包发送校验已禁用");
            return;
        }


        Long senderId = request.getSenderId();
        Integer sessionType = request.getSessionType();

        log.debug("开始校验红包发送权限，senderId={}, sessionType={}", senderId, sessionType);

        // 1. 校验发送者状态
        validateSenderStatus(senderId);

        // 2. 根据会话类型进行不同校验
        if (SessionTypeConstant.SIGNAL_TYPE == sessionType) {
            // 单聊校验
            validateSingleChatPermission(request);
        } else if (SessionTypeConstant.GROUP_TYPE == sessionType) {
            // 群聊校验
            validateGroupChatPermission(request);
        }

        log.debug("红包发送权限校验通过，senderId={}, sessionType={}", senderId, sessionType);
    }

    /**
     * 校验发送者状态
     */
    private void validateSenderStatus(Long senderId) {
        BaseResponse<UserStatusResponse> response = userServiceClient.getUserStatus(senderId);
        if (response == null || response.getCode() != 200 || response.getData() == null) {
            log.warn("校验服务不可用，无法查询发送者状态 ，senderId={}", senderId);
            throw new BusinessException(SERVICE_UNAVAILABLE.getCode(), SERVICE_UNAVAILABLE.getMessage());
        }

        UserStatusResponse userStatus = response.getData();
        if (userStatus.getState() == null) {
            log.warn("发送者用户不存在，senderId={}", senderId);
            throw new BusinessException(NOT_FOUND_ERROR, "发送者用户不存在");
        }
        if (userStatus.getState() == UserStateConstant.STATE_BANNED) {
            log.warn("发送者账号已被封禁，senderId={}", senderId);
            throw new BusinessException(SENDER_DISABLED.getCode(), "您的账号已被封禁，无法发送红包");
        }
        if (userStatus.getState() == UserStateConstant.STATE_CANCELLED) {
            log.warn("发送者账号已注销，senderId={}", senderId);
            throw new BusinessException(SENDER_DISABLED.getCode(), "您的账号已注销，无法发送红包");
        }
    }

    /**
     * 单聊红包校验
     * <p>
     * 校验顺序：
     * 1. 红包类型（最先校验，快速失败）
     * 2. 接收者状态
     * 3. 好友关系（复用 /api/message/validate/single 接口）
     */
    private void validateSingleChatPermission(RedPacketSendRequest request) {
        Long senderId = request.getSenderId();
        Long receiverId = request.getReceiverId();
        Long sessionId = request.getSessionId();
        Integer redPacketType = request.getBody().getRedPacketType();

        // 1. 【最先校验】单聊只能发普通红包
        if (redPacketType != RedPacketConstant.TYPE_NORMAL) {
            log.warn("单聊只能发送普通红包，senderId={}, receiverId={}, redPacketType={}",
                    senderId, receiverId, redPacketType);
            throw new BusinessException(REDPACKET_SINGLE_ONLY_NORMAL.getCode(), REDPACKET_SINGLE_ONLY_NORMAL.getMessage());
        }


        // 2. 校验接收者状态
        validateReceiverStatus(receiverId);

        // 3. 校验好友关系
        validateFriendRelation(senderId, receiverId, sessionId);
    }

    /**
     * 校验接收者状态
     */
    private void validateReceiverStatus(Long receiverId) {
        BaseResponse<UserStatusResponse> response = userServiceClient.getUserStatus(receiverId);

        if (response == null || response.getCode() != 200 || response.getData() == null) {
            log.warn("校验服务不可用，无法查询接收者状态，receiverId={}", receiverId);
            throw new BusinessException(SERVICE_UNAVAILABLE.getCode(), SERVICE_UNAVAILABLE.getMessage());
        }

        UserStatusResponse userStatus = response.getData();
        if (userStatus.getState() == null) {
            log.warn("接收者用户不存在，receiverId={}", receiverId);
            throw new BusinessException(NOT_FOUND_ERROR, "接收者用户不存在");
        }
        if (userStatus.getState() == UserStateConstant.STATE_CANCELLED) {
            log.warn("接收者账号已注销，receiverId={}", receiverId);
            throw new BusinessException(RECEIVER_DISABLED.getCode(), RECEIVER_DISABLED.getMessage());
        }
        if (userStatus.getState() == UserStateConstant.STATE_BANNED) {
            log.warn("接收者账号已被封禁，receiverId={}", receiverId);
            throw new BusinessException(RECEIVER_DISABLED.getCode(), RECEIVER_DISABLED.getMessage());
        }
    }

    /**
     * 校验好友关系（复用 /api/message/validate/single 接口）
     * <p>
     * 该接口内部包含：
     * - Redis 缓存查询
     * - 好友关系校验（正向和反向）
     * - 拉黑状态检查
     */
    private void validateFriendRelation(Long senderId, Long receiverId, Long sessionId) {
        SingleMessageValidateRequest validateRequest = new SingleMessageValidateRequest();
        validateRequest.setSenderId(senderId);
        validateRequest.setReceiverId(receiverId);
        validateRequest.setSessionId(sessionId);

        BaseResponse<MessageValidateResponse> response = userServiceClient.validateSingleMessage(validateRequest);

        if (response == null || response.getCode() != 200 || response.getData() == null) {
            log.warn("校验服务不可用，无法校验好友关系，senderId={}, receiverId={}", senderId, receiverId);
            throw new BusinessException(SERVICE_UNAVAILABLE.getCode(), ValidationError.SERVICE_UNAVAILABLE.getMessage());
        }
        MessageValidateResponse validateResponse = response.getData();
        if (!validateResponse.isAllowed()) {
            // 复用消息校验的错误码（91xxx 系列）
            Integer errorCode = validateResponse.getErrorCode();
            String rejectReason = validateResponse.getRejectReason();

            log.warn("好友关系校验失败，senderId={}, receiverId={}, rejectReason={}, errorCode={}",
                    senderId, receiverId, rejectReason, errorCode);

            // 根据拒绝原因获取错误消息
            ValidationError error = ValidationError.fromName(rejectReason);
            throw new BusinessException(validateResponse.getErrorCode(), error.getMessage());
        }
    }

    /**
     * 群聊红包校验
     */
    private void validateGroupChatPermission(RedPacketSendRequest request) {
        Long senderId = request.getSenderId();
        Long sessionId = request.getSessionId();
        Integer totalCount = request.getBody().getTotalCount();

        // 1. 校验发送者是否为群成员
        BaseResponse<GroupMembershipResponse> membershipResponse =
                userServiceClient.checkGroupMembership(senderId, sessionId);

        if (membershipResponse == null || membershipResponse.getCode() != 200 || membershipResponse.getData() == null) {
            log.warn("校验服务不可用，无法验证群成员资格，senderId={}, sessionId={}", senderId, sessionId);
            throw new BusinessException(SERVICE_UNAVAILABLE.getCode(), ValidationError.SERVICE_UNAVAILABLE.getMessage());
        }

        if (!membershipResponse.getData().getIsMember()) {
            log.warn("发送者不是群成员，senderId={}, sessionId={}", senderId, sessionId);
            throw new BusinessException(NOT_GROUP_MEMBER.getCode(), "您不是该群成员，无法发送红包");
        }

        // 2. 获取群成员数量
        BaseResponse<GroupMemberCountResponse> countResponse =
                userServiceClient.getGroupMemberCount(sessionId);

        if (countResponse == null || countResponse.getCode() != 200 || countResponse.getData() == null) {
            log.warn("校验服务不可用，无法获取群成员数量，sessionId={}", sessionId);
            throw new BusinessException(SERVICE_UNAVAILABLE.getCode(), SERVICE_UNAVAILABLE.getMessage());
        }
        Integer memberCount = countResponse.getData().getMemberCount();

        // 3. 校验红包数量不超过群成员数量
        if (totalCount > memberCount) {
            log.warn("红包数量超过群成员数量，totalCount={}, memberCount={}, sessionId={}",
                    totalCount, memberCount, sessionId);
            throw new BusinessException(ValidationError.REDPACKET_COUNT_EXCEED_MEMBERS.getCode(),
                    String.format("红包数量不能超过群成员数量（当前群成员数：%d）", memberCount));
        }
    }
}
