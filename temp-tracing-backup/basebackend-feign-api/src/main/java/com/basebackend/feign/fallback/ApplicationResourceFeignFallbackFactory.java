package com.basebackend.feign.fallback;

import com.basebackend.admin.dto.ApplicationResourceDTO;
import com.basebackend.common.model.Result;
import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * ApplicationResourceFeign 降级工厂
 *
 * @author BaseBackend Team
 * @since 2025-11-14
 */
@Slf4j
@Component
public class ApplicationResourceFeignFallbackFactory implements FallbackFactory<ApplicationResourceFeignClient> {

    @Override
    public ApplicationResourceFeignClient create(Throwable cause) {
        log.error("ApplicationResourceFeignClient 调用失败: {}", cause.getMessage(), cause);

        return new ApplicationResourceFeignClient() {
            @Override
            public Result<List<ApplicationResourceDTO>> getResourceTree(Long appId) {
                log.warn("查询应用资源树降级: appId={}", appId);
                return Result.success(Collections.emptyList());
            }

            @Override
            public Result<List<ApplicationResourceDTO>> getUserResourceTree(Long appId, Long userId) {
                log.warn("查询用户资源树降级: appId={}, userId={}", appId, userId);
                return Result.success(Collections.emptyList());
            }

            @Override
            public Result<ApplicationResourceDTO> getResourceById(Long id) {
                log.warn("查询资源详情降级: id={}", id);
                return Result.error("服务暂不可用");
            }
        };
    }
}
