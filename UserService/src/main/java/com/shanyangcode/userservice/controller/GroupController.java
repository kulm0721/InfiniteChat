package com.shanyangcode.userservice.controller;

import com.shanyangcode.common.common.BaseResponse;
import com.shanyangcode.common.common.ErrorCode;
import com.shanyangcode.common.common.ResultUtils;
import com.shanyangcode.common.exception.BusinessException;
import com.shanyangcode.userservice.model.dto.request.CreateGroupRequest;
import com.shanyangcode.userservice.model.dto.response.CreateGroupResponse;
import com.shanyangcode.userservice.service.SessionService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("api/group")
public class GroupController {
    private final SessionService sessionService;

    public GroupController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    public BaseResponse<?> createGroup(@Valid @RequestBody CreateGroupRequest request) {
        try {
            CreateGroupResponse response = sessionService.createGroup(request);
            return ResultUtils.success(response);
        } catch (BusinessException e) {
            log.error("创建群聊失败，原因：{}", e.getMessage());
            return ResultUtils.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("创建群聊失败，原因：{}", e.getMessage(), e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR);
        }
    }
}
