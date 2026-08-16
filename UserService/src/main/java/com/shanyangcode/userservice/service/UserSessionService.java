package com.shanyangcode.userservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shanyangcode.userservice.model.entity.UserSession;

import java.util.List;

public interface UserSessionService extends IService<UserSession> {
    List<Long> getUserIdBySessionId(Long SessionId);
    List<Long> getSessionIdsByUserId(Long UserId);

    /**
     * 获取群聊正常成员数量
     *
     * @param sessionId 会话ID
     * @return 正常成员数量
     */
    int getGroupMemberCount(Long sessionId);
}
