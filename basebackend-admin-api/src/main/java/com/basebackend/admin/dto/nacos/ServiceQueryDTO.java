package com.basebackend.admin.dto.nacos;

import lombok.Data;

/**
 * 服务查询DTO
 */
@Data
public class ServiceQueryDTO {

    /**
     * 服务名（模糊查询）
     */
    private String serviceName;

    /**
     * 分组名
     */
    private String groupName;

    /**
     * 命名空间
     */
    private String namespace;

    /**
     * 服务状态
     */
    private String status;

    /**
     * 页码
     */
    private Integer pageNum;

    /**
     * 每页大小
     */
    private Integer pageSize;
}
