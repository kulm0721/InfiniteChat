package com.shanyangcode.userservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
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
}
