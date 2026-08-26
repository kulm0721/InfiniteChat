package com.shanyangcode.userservice.controller;

import com.shanyangcode.common.common.BaseResponse;
import com.shanyangcode.common.common.ErrorCode;
import com.shanyangcode.common.common.ResultUtils;
import com.shanyangcode.common.model.dto.validation.*;
import com.shanyangcode.common.model.vo.UserInfosResponse;
import com.shanyangcode.userservice.service.InternalValidationService;
import com.shanyangcode.userservice.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 内部校验控制器
 * <p>
 * 提供供其他微服务 Feign 调用的校验接口
 * 路径前缀 /internal 表示内部接口，不对外暴露
 */
@Slf4j
@RestController
@RequestMapping("/api/internal")
public class InternalValidationController {

    @Resource
    private InternalValidationService internalValidationService;

    @Resource
    private UserService userService;


    /**
     * 查询用户状态
     * <p>
     * GET /internal/user/status?userId={userId}
     *
     * @param userId 用户 ID
     * @return 用户状态信息 { userId, state, stateName }
     * state: 0=正常, 1=封禁, 2=注销, null=用户不存在
     */
    @GetMapping("/user/status")
    public BaseResponse<UserStatusResponse> getUserStatus(@RequestParam Long userId) {
        log.debug("内部接口: 查询用户状态, userId={}", userId);
        UserStatusResponse response = internalValidationService.getUserStatus(userId);
        return ResultUtils.success(response);
    }

    /**
     * 获取群成员数量
     * <p>
     * GET /internal/group/memberCount?sessionId={sessionId}
     *
     * @param sessionId 会话 ID（群聊 ID）
     * @return 群成员数量信息 { sessionId, memberCount }
     */
    @GetMapping("/group/memberCount")
    public BaseResponse<GroupMemberCountResponse> getGroupMemberCount(@RequestParam Long sessionId) {
        log.debug("内部接口: 获取群成员数量, sessionId={}", sessionId);
        GroupMemberCountResponse response = internalValidationService.getGroupMemberCount(sessionId);
        return ResultUtils.success(response);
    }

    /**
     * 验证群成员资格
     * <p>
     * GET /internal/group/isMember?userId={userId}&sessionId={sessionId}
     *
     * @param userId    用户 ID
     * @param sessionId 会话 ID（群聊 ID）
     * @return 群成员资格信息 { userId, sessionId, isMember, role }
     * role: 0=群主, 1=管理员, 2=普通成员, null=非成员
     */
    @GetMapping("/group/isMember")
    public BaseResponse<GroupMembershipResponse> checkGroupMembership(
            @RequestParam Long userId,
            @RequestParam Long sessionId) {
        log.debug("内部接口: 验证群成员资格, userId={}, sessionId={}", userId, sessionId);
        GroupMembershipResponse response = internalValidationService.checkGroupMembership(userId, sessionId);
        return ResultUtils.success(response);
    }

    /**
     * 单聊消息发送权限校验
     * <p>
     * 校验好友关系，查询后写入 Redis 缓存
     *
     * @param request 校验请求
     * @return 校验结果
     */
    @PostMapping("/validation/single-message")
    public BaseResponse<MessageValidateResponse> validateSingleMessage(
            @RequestBody SingleMessageValidateRequest request) {
        log.debug("单聊消息校验请求: senderId={}, receiverId={}, sessionId={}",
                request.getSenderId(), request.getReceiverId(), request.getSessionId());

        MessageValidateResponse response = internalValidationService.validateSingleMessage(request);
        return ResultUtils.success(response);
    }


    /**
     * 批量获取用户基本信息
     * <p>
     * GET /internal/users/batch?userIds=1,2,3
     *
     * @param userIds 用户 ID 列表
     * @return Map<userId, UserInfosResponse>
     */
    @GetMapping("/users/batch")
    public BaseResponse<?> batchGetUserInfos(
            @RequestParam("userIds") List<Long> userIds) {
        log.debug("内部接口: 批量获取用户信息, userIds={}", userIds);

        // 参数校验
        if (userIds == null || userIds.isEmpty()) {
            return ResultUtils.success(new HashMap<>());
        }

        // 限制单次查询数量，防止滥用
        if (userIds.size() > 100) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR, "单次查询用户数量不能超过100");
        }

        Map<Long, UserInfosResponse> result = userService.getUserInfos(userIds);
        return ResultUtils.success(result);
    }


}
