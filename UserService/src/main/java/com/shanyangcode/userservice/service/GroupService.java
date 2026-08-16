package com.shanyangcode.userservice.service;

import com.shanyangcode.userservice.model.dto.request.InviteGroupRequest;
import com.shanyangcode.userservice.model.dto.response.InviteGroupResponse;

/**
 * 群组服务接口
 *
 * 功能说明：
 * - 处理群组邀请相关业务
 */
public interface GroupService {
    InviteGroupResponse inviteGroup(InviteGroupRequest request);
}
