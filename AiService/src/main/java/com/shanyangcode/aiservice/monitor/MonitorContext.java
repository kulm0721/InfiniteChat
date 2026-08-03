package com.shanyangcode.aiservice.monitor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
public class MonitorContext implements Serializable {

    private Long sessionId;

    private Long userId;

    @Serial
    private static final long serialVersionUID = 1L;
 }
