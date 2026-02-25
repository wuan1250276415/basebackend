package com.basebackend.admin.dto.nacos;

/**
 * 服务查询DTO
 */
public record ServiceQueryDTO(
    /** 服务名（模糊查询） */
    String serviceName,
    /** 分组名 */
    String groupName,
    /** 命名空间 */
    String namespace,
    /** 服务状态 */
    String status,
    /** 页码 */
    Integer pageNum,
    /** 每页大小 */
    Integer pageSize
) {}
