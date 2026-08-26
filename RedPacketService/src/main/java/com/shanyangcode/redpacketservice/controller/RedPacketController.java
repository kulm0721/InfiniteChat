package com.shanyangcode.redpacketservice.controller;

import com.shanyangcode.common.common.BaseResponse;
import com.shanyangcode.common.common.ResultUtils;
import com.shanyangcode.redpacketservice.annotation.PreventDuplicateSubmit;
import com.shanyangcode.redpacketservice.model.dto.RedPacketSendRequest;
import com.shanyangcode.redpacketservice.model.vo.RedPacketSendVO;
import com.shanyangcode.redpacketservice.service.RedPacketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat/redPacket")
@Slf4j
public class RedPacketController {
    @Autowired
    private RedPacketService redPacketService;

    /**
     * 发送红包
     * @param request 红包发送请求
     * @return 红包发送结果（包含红包ID和消息ID）
     */
    @PostMapping("/send")
    @PreventDuplicateSubmit(expireSeconds = 3)
    public BaseResponse<RedPacketSendVO> sendRedPacket(@RequestBody RedPacketSendRequest request) {
        RedPacketSendVO result = redPacketService.sendRedPacket(request);
        log.info("红包发送成功，红包ID: {}, 消息ID: {}", result.getRedPacketId(), result.getMessageId());
        return ResultUtils.success(result);
    }
}
