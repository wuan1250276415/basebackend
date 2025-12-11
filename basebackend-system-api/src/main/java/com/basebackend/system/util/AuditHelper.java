package com.basebackend.system.util;

import com.basebackend.common.context.UserContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 审计字段填充工具类
 * 用于统一设置实体的创建人、更新人等审计字段
 * 
 * @author BaseBackend Team
 * @since 2024-12-07
 */
@Slf4j
@Component
public class AuditHelper {

    /**
     * 系统操作用户ID（用于无法获取当前用户的场景，如定时任务）
     */
    private static final Long SYSTEM_USER_ID = 0L;

    /**
     * 获取当前操作用户ID
     * 如果无法获取当前用户，返回系统用户ID
     *
     * @return 用户ID
     */
    public Long getCurrentUserId() {
        try {
            Long userId = UserContextHolder.getUserId();
            return userId != null ? userId : SYSTEM_USER_ID;
        } catch (Exception e) {
            log.debug("无法获取当前用户ID，使用系统用户ID: {}", e.getMessage());
            return SYSTEM_USER_ID;
        }
    }

    /**
     * 获取当前时间
     *
     * @return 当前时间
     */
    public LocalDateTime getCurrentTime() {
        return LocalDateTime.now();
    }

    /**
     * 静态方法：获取当前用户ID（用于不方便注入的场景）
     *
     * @return 用户ID
     */
    public static Long getOperatorId() {
        try {
            Long userId = UserContextHolder.getUserId();
            return userId != null ? userId : SYSTEM_USER_ID;
        } catch (Exception e) {
            return SYSTEM_USER_ID;
        }
    }
}
