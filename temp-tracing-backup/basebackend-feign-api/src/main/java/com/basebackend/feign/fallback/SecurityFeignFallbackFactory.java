package com.basebackend.feign.fallback;

import com.basebackend.common.model.Result;
import com.basebackend.feign.dto.security.User2FADTO;
import com.basebackend.feign.dto.security.UserDeviceDTO;
import com.basebackend.feign.dto.security.UserOperationLogDTO;
import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * SecurityFeign 降级工厂
 *
 * @author BaseBackend Team
 * @since 2025-11-14
 */
@Slf4j
@Component
public class SecurityFeignFallbackFactory implements FallbackFactory<SecurityFeignClient> {

    @Override
    public SecurityFeignClient create(Throwable cause) {
        log.error("SecurityFeignClient 调用失败: {}", cause.getMessage(), cause);

        return new SecurityFeignClient() {
            @Override
            public Result<List<UserDeviceDTO>> getCurrentUserDevices() {
                return Result.success(Collections.emptyList());
            }

            @Override
            public Result<Void> removeDevice(Long deviceId) {
                log.warn("移除设备降级: deviceId={}", deviceId);
                return Result.error("服务暂不可用");
            }

            @Override
            public Result<Void> trustDevice(Long deviceId) {
                log.warn("信任设备降级: deviceId={}", deviceId);
                return Result.error("服务暂不可用");
            }

            @Override
            public Result<List<UserOperationLogDTO>> getOperationLogs(Integer limit) {
                return Result.success(Collections.emptyList());
            }

            @Override
            public Result<User2FADTO> get2FAConfig() {
                User2FADTO dto = new User2FADTO();
                dto.setEnabled(0);
                return Result.success(dto);
            }

            @Override
            public Result<Void> enable2FA(String type, String verifyCode) {
                log.warn("启用2FA降级: type={}", type);
                return Result.error("服务暂不可用");
            }

            @Override
            public Result<Void> disable2FA(String verifyCode) {
                log.warn("禁用2FA降级");
                return Result.error("服务暂不可用");
            }
        };
    }
}
