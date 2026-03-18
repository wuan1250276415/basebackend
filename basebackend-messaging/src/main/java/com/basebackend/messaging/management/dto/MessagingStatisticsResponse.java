package com.basebackend.messaging.management.dto;

public record MessagingStatisticsResponse(
        long total,
        long pending,
        long sent,
        long consumed,
        long failed,
        long deadLetter,
        String successRate
) {
}
