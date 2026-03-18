package com.basebackend.user.util;

import com.basebackend.common.model.Result;
import com.basebackend.service.client.DeptServiceClient;
import com.basebackend.api.model.dept.DeptBasicDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 部门信息获取工具类
 * <p>
 * 统一封装部门服务调用逻辑，避免重复代码。
 * 支持单个和批量获取部门名称，失败时返回默认值不影响主流程。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeptInfoHelper {

    private final ObjectProvider<DeptServiceClient> deptServiceClientProvider;

    /**
     * 获取部门名称
     * <p>
     * 安全调用部门服务，失败时返回空字符串不影响主流程
     * </p>
     *
     * @param deptId 部门ID
     * @return 部门名称，获取失败返回空字符串
     */
    public String getDeptName(Long deptId) {
        if (deptId == null) {
            return "";
        }
        return getDeptNameBatch(Set.of(deptId)).getOrDefault(deptId, "");
    }

    /**
     * 批量获取部门名称
     * <p>
     * 安全调用部门服务，失败时对应部门返回空字符串
     * </p>
     *
     * @param deptIds 部门ID集合
     * @return 部门ID到部门名称的映射
     */
    public Map<Long, String> getDeptNameBatch(Set<Long> deptIds) {
        Map<Long, String> deptNameMap = new HashMap<>();
        if (deptIds == null || deptIds.isEmpty()) {
            return deptNameMap;
        }

        var deptServiceClient = deptServiceClientProvider.getIfAvailable();
        if (deptServiceClient == null) {
            log.debug("部门服务不可用，跳过批量获取部门名称");
            return deptNameMap;
        }

        try {
            String deptIdsParam = deptIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            Result<List<DeptBasicDTO>> deptResult = deptServiceClient.getBatchByIds(deptIdsParam);
            if (deptResult != null && deptResult.getCode() == 200 && deptResult.getData() != null) {
                for (DeptBasicDTO dept : deptResult.getData()) {
                    if (dept != null && dept.id() != null) {
                        deptNameMap.put(dept.id(), dept.deptName());
                    }
                }
            } else {
                log.warn("批量获取部门信息失败或返回空: deptIds={}, message={}",
                        deptIdsParam, deptResult != null ? deptResult.getMessage() : "null");
            }
        } catch (Exception e) {
            log.warn("批量获取部门信息异常: deptIds={}, error={}", deptIds, e.getMessage());
        }

        return deptNameMap;
    }
}
