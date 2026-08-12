package com.shanyangcode.userservice.controller;

import com.shanyangcode.common.common.BaseResponse;
import com.shanyangcode.common.common.ErrorCode;
import com.shanyangcode.common.common.ResultUtils;
import com.shanyangcode.common.exception.BusinessException;
import com.shanyangcode.userservice.model.vo.FriendDetailVO;
import com.shanyangcode.userservice.service.FriendService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/contact")
public class ContactController {
    private final FriendService friendService;

    public ContactController(FriendService friendService) {
        this.friendService = friendService;
    }

    /**
     * 搜索用户（手机号或邮箱）
     *
     * @param userId  用户ID
     * @param keyword 搜索关键字（手机号或邮箱）
     * @return 用户详情
     */
    @GetMapping("/｛userId｝/user/search")
    public BaseResponse<?> searchUser(
            @PathVariable String userId,
            @RequestParam(value = "keyword") String keyword) {
        try {
            FriendDetailVO friendDetailVO = friendService.searchUserByKeyword(userId, keyword);
            return ResultUtils.success(friendDetailVO);
        } catch (BusinessException e) {
            log.error("搜索用户失败，用户ID：{}，关键字：{}，原因：{}", userId, keyword, e.getMessage());
            return ResultUtils.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("搜索用户失败，用户ID：{}，关键字：{}，原因：{}", userId, keyword, e.getMessage(), e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR);
        }
    }
}
