package com.shanyangcode.userservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shanyangcode.common.common.ErrorCode;
import com.shanyangcode.common.constant.SessionTypeConstant;
import com.shanyangcode.common.exception.ThrowUtils;
import com.shanyangcode.userservice.mapper.SessionMapper;
import com.shanyangcode.userservice.mapper.UserSessionMapper;
import com.shanyangcode.userservice.model.entity.Session;
import com.shanyangcode.userservice.model.entity.UserSession;
import com.shanyangcode.userservice.service.UserSessionService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserSessionServiceImpl extends ServiceImpl<UserSessionMapper, UserSession> implements UserSessionService {
    private final SessionMapper sessionMapper;

    public UserSessionServiceImpl(SessionMapper sessionMapper) {
        this.sessionMapper = sessionMapper;
    }

    @Override
    public List<Long> getUserIdBySessionId(Long SessionId) {
        QueryWrapper<UserSession> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("session_id", SessionId).eq("status", 0);
        List<UserSession> userSessions = this.list(queryWrapper);
        return userSessions.stream().map(UserSession::getUserId).collect(Collectors.toList());
    }

    @Override
    public List<Long> getSessionIdsByUserId(Long userId){
        QueryWrapper<UserSession> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId).eq("status", 0);
        List<UserSession> userSessions = this.list(queryWrapper);
        return userSessions.stream().map(UserSession::getSessionId).collect(Collectors.toList());
    }

    @Override
    public int getGroupMemberCount(Long sessionId) {
        ThrowUtils.throwIf(sessionId == null || sessionId <= 0,
                ErrorCode.PARAMS_ERROR, "会话ID不能为空");
        LambdaQueryWrapper<Session> sessionWrapper = new LambdaQueryWrapper<>();
        sessionWrapper.eq(Session::getSessionId, sessionId)
                .eq(Session::getStatus, 0);
        Session session = sessionMapper.selectOne(sessionWrapper);
        ThrowUtils.throwIf(session == null, ErrorCode.NOT_FOUND_ERROR, "会话不存在或已解散");
        ThrowUtils.throwIf(session.getType() != SessionTypeConstant.GROUP_TYPE,
                ErrorCode.PARAMS_ERROR, "该会话不是群聊");

        QueryWrapper<UserSession> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("session_id", sessionId);
        queryWrapper.eq("status", 0); // 仅统计正常成员
        return Math.toIntExact(this.count(queryWrapper));
    }
}
