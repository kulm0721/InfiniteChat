package com.shanyangcode.userservice.model.dto.response;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class GroupMemberCountResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final int count;

    public GroupMemberCountResponse(int count) {
        this.count = count;
    }
}
