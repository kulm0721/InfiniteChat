package com.shanyangcode.userservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shanyangcode.userservice.model.dto.request.CreateGroupRequest;
import com.shanyangcode.userservice.model.dto.response.CreateGroupResponse;
import com.shanyangcode.userservice.model.entity.Session;

public interface SessionService extends IService<Session> {

    /**
     *
     * @param request 创建群聊请求参数
     * @return 创建结果(包含sessionId,群名,失败成员列表)
     */
    CreateGroupResponse createGroup(CreateGroupRequest request);
}
