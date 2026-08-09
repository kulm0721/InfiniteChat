package com.shanyangcode.userservice.controller;

import com.shanyangcode.userservice.service.UserSessionService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserSessionController {
    @Resource
    private UserSessionService userSessionService;

    @GetMapping("/get/receivers")
    public List<Long> getUserIdBySessionId(@RequestParam("sessionId") Long sessionId) {
        return userSessionService.getUserIdBySessionId(sessionId);
    }

    @GetMapping("/get/sessions")
    List<Long> getSessionIdsByUserId(@RequestParam("userId")Long userId){
        return userSessionService.getSessionIdsByUserId(userId);
    }
}
