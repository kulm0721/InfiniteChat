package com.shanyangcode.redpacketservice.controller;

import com.shanyangcode.common.common.BaseResponse;
import com.shanyangcode.common.common.ResultUtils;
import com.shanyangcode.redpacketservice.annotation.PreventDuplicateSubmit;
import com.shanyangcode.redpacketservice.model.dto.RedPacketReceiveRequest;
import com.shanyangcode.redpacketservice.model.dto.RedPacketSendRequest;
import com.shanyangcode.redpacketservice.model.vo.ReceiveResultVO;
import com.shanyangcode.redpacketservice.model.vo.RedPacketBasicVO;
import com.shanyangcode.redpacketservice.model.vo.RedPacketDetailVO;
import com.shanyangcode.redpacketservice.model.vo.RedPacketSendVO;
import com.shanyangcode.redpacketservice.service.RedPacketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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


    /**
     * 领取红包
     * @param request 红包领取请求
     * @return 领取结果
     */
    @PostMapping("/receive")
    public BaseResponse<ReceiveResultVO> receiveRedPacket(@RequestBody RedPacketReceiveRequest request) {
        log.info("领取红包请求，用户ID: {}, 红包ID: {}",
                request.getUserId(),
                request.getRedPacketId());

        ReceiveResultVO result = redPacketService.receiveRedPacket(request);

        log.info("红包领取处理完成，用户ID: {}, 状态: {}, 金额: {}",
                request.getUserId(),
                result.getStatus(),
                result.getAmount());

        return ResultUtils.success(result);
    }


    /**
     * 查询红包详情
     * 查询红包的基本信息和领取记录（支持分页）
     *
     * @param redPacketId 红包ID
     * @param pageNum     页码（默认1）
     * @param pageSize    每页大小（默认10）
     * @return 红包详情
     */
    @GetMapping("/")
    public BaseResponse<RedPacketDetailVO> getRedPacketDetail(
            @RequestParam("redPacketId") Long redPacketId,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {

        log.info("查询红包详情，红包ID: {}, pageNum: {}, pageSize: {}",
                redPacketId, pageNum, pageSize);

        RedPacketDetailVO detail = redPacketService.getRedPacketDetail(redPacketId, pageNum, pageSize);

        log.info("红包详情查询完成，红包ID: {}, 状态: {}, 已领取数量: {}",
                redPacketId,
                detail.getStatus(),
                detail.getReceivedCount());

        return ResultUtils.success(detail);
    }


    /**
     * 查询红包基本信息（不含领取记录）
     *
     * @param redPacketId 红包ID
     * @return 红包基本信息
     */
    @GetMapping("/basic")
    public BaseResponse<RedPacketBasicVO> getRedPacketBasicInfo(@RequestParam("redPacketId") Long redPacketId) {
        log.info("查询红包基本信息，红包ID: {}", redPacketId);

        RedPacketBasicVO basicInfo = redPacketService.getRedPacketBasicInfo(redPacketId);

        log.info("红包基本信息查询完成，红包ID: {}, 状态: {}, 已领取数量: {}",
                redPacketId,
                basicInfo.getStatus(),
                basicInfo.getReceivedCount());

        return ResultUtils.success(basicInfo);
    }
}
