package com.basebackend.security.context;

import com.basebackend.security.annotation.DataScope;

/**
 * 数据权限上下文持有者
 * 
 * 用于在线程中传递数据权限信息
 */
public class DataScopeContextHolder {

    private static final ThreadLocal<DataScope.DataScopeType> CONTEXT = new ThreadLocal<>();

    /**
     * 设置数据权限类型
     */
    public static void set(DataScope.DataScopeType dataScopeType) {
        CONTEXT.set(dataScopeType);
    }

    /**
     * 获取数据权限类型
     */
    public static DataScope.DataScopeType get() {
        return CONTEXT.get();
    }

    /**
     * 清除数据权限类型
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
}
