package com.shanyangcode.userservice.model.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class GroupKickNotificationDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<Long> memberIds;
    private Long operatorId;
}
