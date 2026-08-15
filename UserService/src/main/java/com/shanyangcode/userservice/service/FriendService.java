package com.shanyangcode.userservice.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.shanyangcode.userservice.model.dto.FriendDTO;
import com.shanyangcode.userservice.model.dto.PageRequest;
import com.shanyangcode.userservice.model.entity.Friend;
import com.shanyangcode.userservice.model.vo.FriendDetailVO;


public interface FriendService extends IService<Friend> {
    /**
     * 根据关键字搜索用户（自动识别手机号或邮箱）
     *
     * @param userId  当前用户ID
     * @param keyword 搜索关键字（手机号或邮箱）
     * @return FriendDetailVO 对象
     */
    FriendDetailVO searchUserByKeyword(String userId, String keyword);


    /**
     * 获取好友的详细信息
     *
     * @param userId   当前用户Id
     * @param friendId 好友Id
     * @return FriendDetailVO 对象
     */
    FriendDetailVO getFriendDetails(String userId, String friendId);

    /**
     * 获取用户的好友列表
     * <p>
     * 支持分页和关键字搜索
     *
     * @param userId      用户ID
     * @param pageRequest 分页参数
     * @param key         搜索关键字
     * @return 分页的好友DTO列表
     */
    IPage<FriendDTO> getFriends(String userId, PageRequest pageRequest, String key);


    /**
     * 删除好友关系
     * <p>
     * 处理流程：
     * 1. 删除双向好友关系
     * 2. 删除相关的好友申请记录
     * 3. 删除会话和用户会话关系
     *
     * @param userId   当前用户ID
     * @param friendId 好友ID
     * @return 删除是否成功
     */
    boolean deleteFriend(Long userId, Long friendId);


    /**
     * 拉黑好友
     *
     * @param userId   当前用户ID
     * @param friendId 好友ID
     * @return 更新是否成功
     */
    boolean blockFriend(Long userId, Long friendId);


    /**
     * 取消拉黑好友
     *
     * @param userId   当前用户ID
     * @param friendId 好友ID
     * @return 更新是否成功
     */
    boolean unblockFriend(Long userId, Long friendId);
}
