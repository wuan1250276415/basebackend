package com.basebackend.database.routing;

/**
 * 数据源类型枚举
 *
 * @author 浮浮酱
 */
public enum DataSourceType {
    /**
     * 主库（Master）- 处理写操作
     */
    MASTER,

    /**
     * 从库（Slave）- 处理读操作
     */
    SLAVE
}
