package com.basebackend.service.client.fallback;

import com.basebackend.api.model.dept.DeptBasicDTO;
import com.basebackend.common.model.Result;
import com.basebackend.service.client.DeptServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 部门服务客户端降级实现
 *
 * @author Claude Code
 * @since 2025-11-08
 */
@Component
public class DeptServiceClientFallback implements DeptServiceClient {

    private static final Logger log = LoggerFactory.getLogger(DeptServiceClientFallback.class);

    @Override
    public Result<List<DeptBasicDTO>> getDeptTree() {
        log.error("[服务降级] 获取部门树失败");
        return Result.success("部门服务降级，返回空列表", Collections.emptyList());
    }

    @Override
    public Result<List<DeptBasicDTO>> getDeptList() {
        log.error("[服务降级] 获取部门列表失败");
        return Result.success("部门服务降级，返回空列表", Collections.emptyList());
    }

    @Override
    public Result<DeptBasicDTO> getById(Long id) {
        log.error("[服务降级] 获取部门信息失败: deptId={}", id);
        return Result.error("部门服务不可用，获取部门信息失败");
    }

    @Override
    public Result<List<DeptBasicDTO>> getChildrenByDeptId(Long id) {
        log.error("[服务降级] 获取子部门列表失败: deptId={}", id);
        return Result.success("部门服务降级，返回空列表", Collections.emptyList());
    }

    @Override
    public Result<List<Long>> getChildrenDeptIds(Long id) {
        log.error("[服务降级] 获取子部门ID列表失败: deptId={}", id);
        return Result.success("部门服务降级，返回空列表", Collections.emptyList());
    }

    @Override
    public Result<DeptBasicDTO> getByDeptName(String deptName) {
        log.error("[服务降级] 根据部门名称获取部门失败: deptName={}", deptName);
        return Result.error("部门服务不可用");
    }

    @Override
    public Result<DeptBasicDTO> getByDeptCode(String deptCode) {
        log.error("[服务降级] 根据部门编码获取部门失败: deptCode={}", deptCode);
        return Result.error("部门服务不可用");
    }

    @Override
    public Result<List<DeptBasicDTO>> getBatchByIds(String deptIds) {
        log.error("[服务降级] 批量获取部门信息失败: deptIds={}", deptIds);
        return Result.success("部门服务降级，返回空列表", Collections.emptyList());
    }

    @Override
    public Result<List<DeptBasicDTO>> getByParentId(Long parentId) {
        log.error("[服务降级] 根据父部门ID获取子部门失败: parentId={}", parentId);
        return Result.success("部门服务降级，返回空列表", Collections.emptyList());
    }

    @Override
    public Result<Boolean> checkDeptNameUnique(String deptName, Long parentId, Long deptId) {
        log.error("[服务降级] 检查部门名称唯一性失败: deptName={}", deptName);
        return Result.success("部门服务降级", false);
    }
}
