package com.basebackend.security.context;

import com.basebackend.security.enums.DataScopeType;
import lombok.Data;

/**
 * 数据权限上下文持有者
 * 用于在线程中传递数据权限信息
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-26
 */
public class DataScopeContextHolder {

    private static final ThreadLocal<DataScopeContext> CONTEXT = new ThreadLocal<>();

    /**
     * 设置数据权限上下文
     */
    public static void set(DataScopeContext dataScopeContext) {
        CONTEXT.set(dataScopeContext);
    }

    /**
     * 设置数据权限类型
     */
    public static void set(DataScopeType dataScopeType) {
        CONTEXT.set(new DataScopeContext(dataScopeType));
    }

    /**
     * 获取数据权限类型
     */
    public static DataScopeType getDataScopeType() {
        DataScopeContext context = CONTEXT.get();
        return context != null ? context.getDataScopeType() : DataScopeType.ALL;
    }

    /**
     * 获取用户ID
     */
    public static Long getUserId() {
        DataScopeContext context = CONTEXT.get();
        return context != null ? context.getUserId() : null;
    }

    /**
     * 设置用户ID
     */
    public static void setUserId(Long userId) {
        DataScopeContext context = CONTEXT.get();
        if (context == null) {
            context = new DataScopeContext(DataScopeType.ALL);
        }
        context.setUserId(userId);
        CONTEXT.set(context);
    }

    /**
     * 获取部门ID
     */
    public static Long getDeptId() {
        DataScopeContext context = CONTEXT.get();
        return context != null ? context.getDeptId() : null;
    }

    /**
     * 设置部门ID
     */
    public static void setDeptId(Long deptId) {
        DataScopeContext context = CONTEXT.get();
        if (context == null) {
            context = new DataScopeContext(DataScopeType.ALL);
        }
        context.setDeptId(deptId);
        CONTEXT.set(context);
    }

    /**
     * 清除数据权限上下文
     */
    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * 检查是否设置了数据权限
     */
    public static boolean isSet() {
        return CONTEXT.get() != null;
    }

    /**
     * 数据权限上下文
     */
    @Data
    public static class DataScopeContext {
        private DataScopeType dataScopeType;
        private Long userId;
        private Long deptId;

        public DataScopeContext() {}

        public DataScopeContext(DataScopeType dataScopeType) {
            this.dataScopeType = dataScopeType;
        }

        public DataScopeContext(DataScopeType dataScopeType, Long userId, Long deptId) {
            this.dataScopeType = dataScopeType;
            this.userId = userId;
            this.deptId = deptId;
        }
    }
}
