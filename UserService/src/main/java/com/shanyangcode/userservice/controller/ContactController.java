package com.shanyangcode.userservice.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.shanyangcode.common.common.BaseResponse;
import com.shanyangcode.common.common.ErrorCode;
import com.shanyangcode.common.common.ResultUtils;
import com.shanyangcode.common.exception.BusinessException;
import com.shanyangcode.userservice.model.dto.ApplyFriendDTO;
import com.shanyangcode.userservice.model.dto.FriendDTO;
import com.shanyangcode.userservice.model.dto.PageRequest;
import com.shanyangcode.userservice.model.dto.request.AddFriendRequest;
import com.shanyangcode.userservice.model.vo.FriendDetailVO;
import com.shanyangcode.userservice.model.vo.PageResponse;
import com.shanyangcode.userservice.service.ApplyFriendService;
import com.shanyangcode.userservice.service.FriendService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

@Slf4j
@RestController
@RequestMapping("/api/contact")
public class ContactController {
    private final FriendService friendService;

    private final ApplyFriendService applyFriendService;

    public ContactController(FriendService friendService,
                             ApplyFriendService applyFriendService) {
        this.friendService = friendService;
        this.applyFriendService = applyFriendService;
    }

    /**
     * 搜索用户（手机号或邮箱）
     *
     * @param userId  用户ID
     * @param keyword 搜索关键字（手机号或邮箱）
     * @return 用户详情
     */
    @GetMapping("/{userId}/user/search")
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

    /**
     * 获取联系人列表
     *
     * @param userId      用户ID
     * @param pageRequest 分页参数
     * @param key         查询关键字
     * @return 联系人列表（分页）
     */
    @GetMapping("/{userId}/friend")
    public BaseResponse<?> getFriends(
            @PathVariable("userId") String userId,
            PageRequest pageRequest,
            @RequestParam(value = "key", defaultValue = "") String key) {
        try {
            IPage<FriendDTO> friendsPage = friendService.getFriends(userId, pageRequest, key);

            // 使用 PageResponse 统一返回格式
            return ResultUtils.success(PageResponse.of(friendsPage));
        } catch (BusinessException e) {
            log.error("获取好友列表失败，用户ID：{}，原因：{}", userId, e.getMessage());
            return ResultUtils.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("获取好友列表失败，用户ID：{}，原因：{}", userId, e.getMessage(), e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR);
        }
    }

    /**
     * 发送好友申请
     *
     * @param userId        发送者用户ID
     * @param receiveuserId 接收者用户ID
     * @param request       申请信息
     * @return 是否成功
     */
    @PostMapping("/{userId}/friend/{receiveuserId}")
    public BaseResponse<?> sendFriendRequest(
            @PathVariable("userId") String userId,
            @PathVariable("receiveuserId") String receiveuserId,
            @Valid @RequestBody AddFriendRequest request) {
        try {
            Long senderId = Long.valueOf(userId);
            Long receiverId = Long.valueOf(receiveuserId);
            Long applyFriendId = applyFriendService.sendFriendRequest(senderId, receiverId, request.getMsg());
            return ResultUtils.success(applyFriendId != null);

        } catch (NumberFormatException e) {
            log.error("发送好友申请失败，用户ID格式错误，发送者：{}，接收者：{}", userId, receiveuserId);
            return ResultUtils.error(ErrorCode.PARAMS_ERROR, "用户ID格式错误");
        } catch (BusinessException e) {
            log.error("发送好友申请失败，发送者：{}，接收者：{}，原因：{}", userId, receiveuserId, e.getMessage());
            return ResultUtils.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("发送好友申请失败，发送者：{}，接收者：{}，原因：{}", userId, receiveuserId, e.getMessage(), e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR);
        }
    }


    /**
     * 获取好友申请列表
     *
     * @param userId      用户ID
     * @param pageRequest 分页参数
     * @return 申请列表（分页）
     */
    @GetMapping("/{userId}/apply")
    public BaseResponse<?> getApplyList(
            @PathVariable Long userId,
            PageRequest pageRequest) {
        try {
            //查询分页数据
            IPage<ApplyFriendDTO> applyFriendDTOPage = applyFriendService.getReceivedRequestsWithUserInfo(
                    userId, pageRequest);

            // 使用 PageResponse 统一返回格式
            return ResultUtils.success(PageResponse.of(applyFriendDTOPage));
        } catch (BusinessException e) {
            log.error("获取好友申请列表失败，用户ID：{}，原因：{}", userId, e.getMessage());
            return ResultUtils.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("获取好友申请列表失败，用户ID：{}，原因：{}", userId, e.getMessage(), e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR);
        }
    }

    /**
     * 获取未读好友申请数量
     *
     * @param userId 用户ID
     * @return 未读好友申请数量
     */
    @GetMapping("/{userId}/applyCount")
    public BaseResponse<?> getUnreadApplyCount(@PathVariable Long userId) {
        try {
            int count = applyFriendService.getUnreadCount(userId);
            HashMap<String, Integer> map = new HashMap<>();
            map.put("count", count);
            return ResultUtils.success(map);
        } catch (BusinessException e) {
            log.error("获取未读好友申请数量失败，用户ID：{}，原因：{}", userId, e.getMessage());
            return ResultUtils.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("获取未读好友申请数量失败，用户ID：{}，原因：{}", userId, e.getMessage(), e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR);
        }
    }
}
