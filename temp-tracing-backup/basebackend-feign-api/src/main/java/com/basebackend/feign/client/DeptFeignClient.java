package com.basebackend.feign.client;

import com.basebackend.common.model.Result;
import com.basebackend.feign.constant.FeignServiceConstants;
import com.basebackend.feign.dto.dept.DeptBasicDTO;
import com.basebackend.feign.fallback.DeptFeignFallbackFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 部门服务 Feign 客户端
 *
 * @author Claude Code
 * @since 2025-11-08
 */
@FeignClient(
        name = FeignServiceConstants.ADMIN_SERVICE,
        contextId = "deptFeignClient",
        path = "/api/admin/depts",
        fallbackFactory = DeptFeignFallbackFactory.class
)
public interface DeptFeignClient {

    /**
     * 获取部门树
     *
     * @return 部门树
     */
    @GetMapping("/tree")
    @Operation(summary = "获取部门树", description = "获取部门树形结构")
    Result<List<DeptBasicDTO>> getDeptTree();

    /**
     * 获取部门列表
     *
     * @return 部门列表
     */
    @GetMapping
    @Operation(summary = "获取部门列表", description = "获取部门列表")
    Result<List<DeptBasicDTO>> getDeptList();

    /**
     * 根据部门ID获取部门信息
     *
     * @param id 部门ID
     * @return 部门信息
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询部门", description = "根据ID查询部门详情")
    Result<DeptBasicDTO> getById(@Parameter(description = "部门ID") @PathVariable("id") Long id);

    /**
     * 根据部门ID获取子部门列表
     *
     * @param id 部门ID
     * @return 子部门列表
     */
    @GetMapping("/{id}/children")
    @Operation(summary = "获取子部门", description = "根据部门ID获取子部门列表")
    Result<List<DeptBasicDTO>> getChildrenByDeptId(@Parameter(description = "部门ID") @PathVariable("id") Long id);

    /**
     * 根据部门ID获取所有子部门ID列表（包括自己）
     *
     * @param id 部门ID
     * @return 子部门ID列表
     */
    @GetMapping("/{id}/children-ids")
    @Operation(summary = "获取子部门ID列表", description = "根据部门ID获取所有子部门ID列表")
    Result<List<Long>> getChildrenDeptIds(@Parameter(description = "部门ID") @PathVariable("id") Long id);

    /**
     * 根据部门名称获取部门信息
     *
     * @param deptName 部门名称
     * @return 部门信息
     */
    @GetMapping("/by-name")
    @Operation(summary = "根据部门名称查询", description = "根据部门名称查询部门信息")
    Result<DeptBasicDTO> getByDeptName(@Parameter(description = "部门名称") @RequestParam("deptName") String deptName);

    /**
     * 根据部门编码获取部门信息
     *
     * @param deptCode 部门编码
     * @return 部门信息
     */
    @GetMapping("/by-code")
    @Operation(summary = "根据部门编码查询", description = "根据部门编码查询部门信息")
    Result<DeptBasicDTO> getByDeptCode(@Parameter(description = "部门编码") @RequestParam("deptCode") String deptCode);

    /**
     * 批量获取部门信息
     *
     * @param deptIds 部门ID列表（逗号分隔）
     * @return 部门信息列表
     */
    @GetMapping("/batch")
    @Operation(summary = "批量查询部门", description = "根据部门ID列表批量查询部门信息")
    Result<List<DeptBasicDTO>> getBatchByIds(@Parameter(description = "部门ID列表") @RequestParam("deptIds") String deptIds);

    /**
     * 根据父部门ID获取直接子部门列表
     *
     * @param parentId 父部门ID
     * @return 子部门列表
     */
    @GetMapping("/by-parent")
    @Operation(summary = "根据父部门ID查询", description = "根据父部门ID获取直接子部门列表")
    Result<List<DeptBasicDTO>> getByParentId(@Parameter(description = "父部门ID") @RequestParam("parentId") Long parentId);

    /**
     * 检查部门名称是否唯一
     *
     * @param deptName 部门名称
     * @param parentId 父部门ID
     * @param deptId   部门ID（更新时传入）
     * @return true-唯一，false-不唯一
     */
    @GetMapping("/check-dept-name")
    @Operation(summary = "检查部门名称唯一性", description = "检查部门名称是否唯一")
    Result<Boolean> checkDeptNameUnique(
            @Parameter(description = "部门名称") @RequestParam("deptName") String deptName,
            @Parameter(description = "父部门ID") @RequestParam("parentId") Long parentId,
            @Parameter(description = "部门ID") @RequestParam(value = "deptId", required = false) Long deptId
    );
}
