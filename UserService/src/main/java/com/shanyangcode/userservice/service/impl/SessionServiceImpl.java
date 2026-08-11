package com.shanyangcode.userservice.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shanyangcode.userservice.mapper.SessionMapper;
import com.shanyangcode.userservice.model.entity.Session;
import com.shanyangcode.userservice.service.SessionService;
import org.springframework.stereotype.Service;

@Service
public class SessionServiceImpl extends ServiceImpl<SessionMapper, Session> implements SessionService {

}
