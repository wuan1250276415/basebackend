package com.basebackend.feign.fallback;

import com.basebackend.common.model.Result;
import com.basebackend.feign.client.DeptFeignClient;
import com.basebackend.feign.dto.dept.DeptBasicDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 部门服务 Feign 降级处理工厂
 *
 * @author Claude Code
 * @since 2025-11-08
 */
@Slf4j
@Component
public class DeptFeignFallbackFactory implements FallbackFactory<DeptFeignClient> {

    @Override
    public DeptFeignClient create(Throwable cause) {
        return new DeptFeignClient() {

            @Override
            public Result<List<DeptBasicDTO>> getDeptTree() {
                log.error("[Feign降级] 获取部门树失败: error={}", cause.getMessage(), cause);
                return Result.success("部门服务暂时不可用，返回空列表", Collections.emptyList());
            }

            @Override
            public Result<List<DeptBasicDTO>> getDeptList() {
                log.error("[Feign降级] 获取部门列表失败: error={}", cause.getMessage(), cause);
                return Result.success("部门服务暂时不可用，返回空列表", Collections.emptyList());
            }

            @Override
            public Result<DeptBasicDTO> getById(Long id) {
                log.error("[Feign降级] 根据ID查询部门失败: deptId={}, error={}", id, cause.getMessage(), cause);
                return Result.error("部门服务暂时不可用，请稍后重试");
            }

            @Override
            public Result<List<DeptBasicDTO>> getChildrenByDeptId(Long id) {
                log.error("[Feign降级] 获取子部门列表失败: deptId={}, error={}", id, cause.getMessage(), cause);
                return Result.success("部门服务暂时不可用，返回空列表", Collections.emptyList());
            }

            @Override
            public Result<List<Long>> getChildrenDeptIds(Long id) {
                log.error("[Feign降级] 获取子部门ID列表失败: deptId={}, error={}", id, cause.getMessage(), cause);
                return Result.success("部门服务暂时不可用，返回空列表", Collections.emptyList());
            }

            @Override
            public Result<DeptBasicDTO> getByDeptName(String deptName) {
                log.error("[Feign降级] 根据部门名称查询失败: deptName={}, error={}", deptName, cause.getMessage(), cause);
                return Result.error("部门服务暂时不可用，请稍后重试");
            }

            @Override
            public Result<DeptBasicDTO> getByDeptCode(String deptCode) {
                log.error("[Feign降级] 根据部门编码查询失败: deptCode={}, error={}", deptCode, cause.getMessage(), cause);
                return Result.error("部门服务暂时不可用，请稍后重试");
            }

            @Override
            public Result<List<DeptBasicDTO>> getBatchByIds(String deptIds) {
                log.error("[Feign降级] 批量查询部门失败: deptIds={}, error={}", deptIds, cause.getMessage(), cause);
                return Result.success("部门服务暂时不可用，返回空列表", Collections.emptyList());
            }

            @Override
            public Result<List<DeptBasicDTO>> getByParentId(Long parentId) {
                log.error("[Feign降级] 根据父部门ID查询失败: parentId={}, error={}", parentId, cause.getMessage(), cause);
                return Result.success("部门服务暂时不可用，返回空列表", Collections.emptyList());
            }

            @Override
            public Result<Boolean> checkDeptNameUnique(String deptName, Long parentId, Long deptId) {
                log.error("[Feign降级] 检查部门名称唯一性失败: deptName={}, error={}", deptName, cause.getMessage(), cause);
                // 降级时返回不唯一，避免误操作
                return Result.success("部门服务暂时不可用，建议稍后重试", false);
            }
        };
    }
}
