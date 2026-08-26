package com.shanyangcode.redpacketservice.client;

import com.shanyangcode.common.common.BaseResponse;
import com.shanyangcode.common.common.ResultUtils;
import com.shanyangcode.common.enums.ValidationError;
import com.shanyangcode.common.model.dto.validation.UserStatusResponse;
import com.shanyangcode.common.model.vo.UserInfosResponse;
import com.shanyangcode.common.model.dto.validation.GroupMemberCountResponse;
import com.shanyangcode.common.model.dto.validation.GroupMembershipResponse;
import com.shanyangcode.common.model.dto.validation.MessageValidateResponse;
import com.shanyangcode.common.model.dto.validation.SingleMessageValidateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * UserServiceClient 降级工厂
 *
 * 采用严格模式：服务不可用时返回错误响应，调用方应拒绝操作
 * 遵循「宁可误拦不可漏过」原则
 */
@Component
@Slf4j
public class UserServiceClientFallbackFactory implements FallbackFactory<UserServiceClient> {

    @Override
    public UserServiceClient create(Throwable cause) {
        return new com.shanyangcode.redpacketservice.client.UserServiceClient() {

            @Override
            public BaseResponse<UserStatusResponse> getUserStatus(Long userId) {
                log.error("UserService 不可用，无法查询用户状态。userId={}, 原因: {}", userId, cause.getMessage());
                return new BaseResponse<>(
                        ValidationError.SERVICE_UNAVAILABLE.getCode(),
                        null,
                        "校验服务暂时不可用，请稍后重试"
                );
            }

            @Override
            public BaseResponse<GroupMemberCountResponse> getGroupMemberCount(Long sessionId) {
                log.error("UserService 不可用，无法获取群成员数量。sessionId={}, 原因: {}", sessionId, cause.getMessage());
                return new BaseResponse<>(
                        ValidationError.SERVICE_UNAVAILABLE.getCode(),
                        null,
                        "校验服务暂时不可用，请稍后重试"
                );
            }

            @Override
            public BaseResponse<GroupMembershipResponse> checkGroupMembership(Long userId, Long sessionId) {
                log.error("UserService 不可用，无法验证群成员资格。userId={}, sessionId={}, 原因: {}",
                        userId, sessionId, cause.getMessage());
                return new BaseResponse<>(
                        ValidationError.SERVICE_UNAVAILABLE.getCode(),
                        null,
                        "校验服务暂时不可用，请稍后重试"
                );
            }

            @Override
            public BaseResponse<MessageValidateResponse> validateSingleMessage(SingleMessageValidateRequest request) {
                log.error("UserService 不可用，无法校验单聊消息权限。senderId={}, receiverId={}, 原因: {}",
                        request.getSenderId(), request.getReceiverId(), cause.getMessage());
                return new BaseResponse<>(
                        ValidationError.SERVICE_UNAVAILABLE.getCode(),
                        null,
                        "校验服务暂时不可用，请稍后重试"
                );
            }

            @Override
            public BaseResponse<Map<Long, UserInfosResponse>> batchGetUserInfos(List<Long> userIds) {
                // 宽松降级：返回空 Map，让业务层展示 null 而非报错
                // 用户信息为空不影响核心业务（红包详情仍可返回，只是昵称/头像为空）
                log.warn("UserService 不可用，无法批量获取用户信息。userIds={}, 原因: {}",
                        userIds, cause.getMessage());
                return ResultUtils.success(new HashMap<>());
            }
        };
    }
}
