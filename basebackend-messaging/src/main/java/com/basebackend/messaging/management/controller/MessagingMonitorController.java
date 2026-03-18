package com.basebackend.messaging.management.controller;

import com.basebackend.common.model.Result;
import com.basebackend.messaging.management.dto.MessagingStatisticsResponse;
import com.basebackend.messaging.management.dto.QueueMonitorResponse;
import com.basebackend.messaging.management.service.MessagingMonitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/messaging/monitor")
@RequiredArgsConstructor
public class MessagingMonitorController {

    private final MessagingMonitorService messagingMonitorService;

    @GetMapping("/statistics")
    public Result<MessagingStatisticsResponse> getStatistics() {
        try {
            return Result.success(messagingMonitorService.getStatistics());
        } catch (Exception e) {
            log.error("获取消息统计失败", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/queue")
    public Result<QueueMonitorResponse> getQueueMonitor() {
        try {
            return Result.success(messagingMonitorService.getQueueMonitor());
        } catch (Exception e) {
            log.error("获取队列监控失败", e);
            return Result.error(e.getMessage());
        }
    }
}
