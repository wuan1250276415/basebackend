package com.basebackend.admin.dto.messaging;

import lombok.Data;

/**
 * 死信处理DTO
 */
@Data
public class DeadLetterHandleDTO {

    private Long id;

    private String action; // REDELIVER, DISCARD
}
