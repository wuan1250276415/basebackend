package com.basebackend.logging.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * 
 * 使用示例：
 * @OperationLog(operation = "创建用户", businessType = BusinessType.INSERT)
 * public void createUser(User user) { ... }
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /**
     * 操作名称
     */
    String operation() default "";

    /**
     * 业务类型
     */
    BusinessType businessType() default BusinessType.OTHER;

    /**
     * 是否保存请求参数
     */
    boolean saveRequestData() default true;

    /**
     * 是否保存响应参数
     */
    boolean saveResponseData() default false;

    /**
     * 业务类型枚举
     */
    enum BusinessType {
        /**
         * 其他
         */
        OTHER,
        /**
         * 新增
         */
        INSERT,
        /**
         * 修改
         */
        UPDATE,
        /**
         * 删除
         */
        DELETE,
        /**
         * 查询
         */
        SELECT,
        /**
         * 导出
         */
        EXPORT,
        /**
         * 导入
         */
        IMPORT,
        /**
         * 授权
         */
        GRANT,
        /**
         * 强退
         */
        FORCE_LOGOUT,
        /**
         * 清空数据
         */
        CLEAN
    }
}
