package com.basebackend.user.util;

import com.basebackend.common.model.Result;
import com.basebackend.service.client.DeptServiceClient;
import com.basebackend.api.model.dept.DeptBasicDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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

        var deptServiceClient = deptServiceClientProvider.getIfAvailable();
        if (deptServiceClient == null) {
            log.debug("部门服务不可用，跳过获取部门名称: deptId={}", deptId);
            return "";
        }

        try {
            Result<DeptBasicDTO> deptResult = deptServiceClient.getById(deptId);
            if (deptResult != null && deptResult.getCode() == 200 && deptResult.getData() != null) {
                return deptResult.getData().deptName();
            } else {
                log.warn("获取部门信息失败或返回空: deptId={}, message={}",
                        deptId, deptResult != null ? deptResult.getMessage() : "null");
                return "";
            }
        } catch (Exception e) {
            log.warn("调用部门服务异常（系统服务可能未启动）: deptId={}, error={}", deptId, e.getMessage());
            return "";
        }
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

        for (Long deptId : deptIds) {
            try {
                Result<DeptBasicDTO> deptResult = deptServiceClient.getById(deptId);
                if (deptResult != null && deptResult.getCode() == 200 && deptResult.getData() != null) {
                    deptNameMap.put(deptId, deptResult.getData().deptName());
                }
            } catch (Exception e) {
                log.warn("批量获取部门信息异常: deptId={}, error={}", deptId, e.getMessage());
            }
        }

        return deptNameMap;
    }
}
