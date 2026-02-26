package com.basebackend.api.model.dept;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 部门基础信息 DTO
 *
 * @author Claude Code
 * @since 2025-11-08
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "部门基础信息")
public record DeptBasicDTO(
        Long id,
        Long parentId,
        String deptName,
        String deptCode,
        Long leaderId,
        String leaderName,
        String phone,
        String email,
        Integer sort,
        Integer orderNum,
        Integer status,
        String remark,
        List<DeptBasicDTO> children,
        LocalDateTime createTime,
        LocalDateTime updateTime
) implements Serializable {
}
