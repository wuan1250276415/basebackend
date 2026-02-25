package com.basebackend.service.client;

import com.basebackend.api.model.dept.DeptBasicDTO;
import com.basebackend.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.List;

/**
 * 部门服务客户端
 *
 * @author Claude Code
 * @since 2025-11-08
 */
@HttpExchange("/api/system/depts")
public interface DeptServiceClient {

    @GetExchange("/tree")
    @Operation(summary = "获取部门树")
    Result<List<DeptBasicDTO>> getDeptTree();

    @GetExchange
    @Operation(summary = "获取部门列表")
    Result<List<DeptBasicDTO>> getDeptList();

    @GetExchange("/{id}")
    @Operation(summary = "根据ID获取部门信息")
    Result<DeptBasicDTO> getById(@PathVariable("id") Long id);

    @GetExchange("/{id}/children")
    @Operation(summary = "获取子部门列表")
    Result<List<DeptBasicDTO>> getChildrenByDeptId(@PathVariable("id") Long id);

    @GetExchange("/{id}/children-ids")
    @Operation(summary = "获取子部门ID列表")
    Result<List<Long>> getChildrenDeptIds(@PathVariable("id") Long id);

    @GetExchange("/by-name")
    @Operation(summary = "根据部门名称获取部门")
    Result<DeptBasicDTO> getByDeptName(@RequestParam("deptName") String deptName);

    @GetExchange("/by-code")
    @Operation(summary = "根据部门编码获取部门")
    Result<DeptBasicDTO> getByDeptCode(@RequestParam("deptCode") String deptCode);

    @GetExchange("/batch")
    @Operation(summary = "批量获取部门信息")
    Result<List<DeptBasicDTO>> getBatchByIds(@RequestParam("deptIds") String deptIds);

    @GetExchange("/by-parent")
    @Operation(summary = "根据父部门ID获取子部门")
    Result<List<DeptBasicDTO>> getByParentId(@RequestParam("parentId") Long parentId);

    @GetExchange("/check-dept-name")
    @Operation(summary = "检查部门名称唯一性")
    Result<Boolean> checkDeptNameUnique(@RequestParam("deptName") String deptName,
                                        @RequestParam(value = "parentId", required = false) Long parentId,
                                        @RequestParam(value = "deptId", required = false) Long deptId);
}
