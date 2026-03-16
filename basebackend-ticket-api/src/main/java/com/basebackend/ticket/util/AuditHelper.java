package com.basebackend.ticket.util;

import com.basebackend.common.context.UserContext;
import com.basebackend.common.context.UserContextHolder;
import com.basebackend.database.entity.BaseEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 审计字段填充工具
 */
@Component
public class AuditHelper {

    /**
     * 获取当前操作人ID
     */
    public Long getCurrentUserId() {
        UserContext ctx = UserContextHolder.get();
        return ctx != null ? ctx.getUserId() : 0L;
    }

    /**
     * 填充创建审计字段
     */
    public void setCreateAuditFields(BaseEntity entity) {
        Long userId = getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        entity.setCreateBy(userId);
        entity.setUpdateBy(userId);
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
    }

    /**
     * 填充更新审计字段
     */
    public void setUpdateAuditFields(BaseEntity entity) {
        entity.setUpdateBy(getCurrentUserId());
        entity.setUpdateTime(LocalDateTime.now());
    }

    /**
     * 获取当前操作人ID（静态方法）
     */
    public static Long getOperatorId() {
        UserContext ctx = UserContextHolder.get();
        return ctx != null ? ctx.getUserId() : 0L;
    }
}
