package com.basebackend.admin.dto.messaging;

/**
 * 死信处理DTO
 */
public record DeadLetterHandleDTO(
    Long id,
    String action // REDELIVER, DISCARD
) {}
