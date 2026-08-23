package com.shanyangcode.userservice.model.dto.response;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class KickGroupMembersResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private List<String> successIds;
}
