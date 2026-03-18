package com.basebackend.messaging.management.dto;

public record QueueMonitorResponse(
        long queueCount,
        long totalMessages,
        long readyMessages,
        long unackedMessages,
        long consumerCount,
        double messageRate,
        double ackRate
) {
}
