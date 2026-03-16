package com.basebackend.chat.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 创建群请求
 */
@Data
public class CreateGroupRequest {

    @NotEmpty(message = "群名称不能为空")
    private String name;

    private String avatar;

    private String description;

    /** 初始成员ID列表（不含自己） */
    private List<Long> memberIds;
}
