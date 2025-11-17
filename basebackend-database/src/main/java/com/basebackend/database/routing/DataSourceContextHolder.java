package com.basebackend.database.routing;

import lombok.extern.slf4j.Slf4j;

/**
 * 数据源上下文持有者
 * 使用 ThreadLocal 保存当前线程的数据源类型
 *
 * @author 浮浮酱
 */
@Slf4j
public class DataSourceContextHolder {

    private static final ThreadLocal<DataSourceType> CONTEXT_HOLDER = new ThreadLocal<>();

    /**
     * 设置数据源类型
     *
     * @param dataSourceType 数据源类型
     */
    public static void setDataSourceType(DataSourceType dataSourceType) {
        if (dataSourceType == null) {
            log.warn("数据源类型为空，使用默认主库");
            dataSourceType = DataSourceType.MASTER;
        }
        log.debug("切换数据源: {}", dataSourceType);
        CONTEXT_HOLDER.set(dataSourceType);
    }

    /**
     * 获取数据源类型
     *
     * @return 数据源类型
     */
    public static DataSourceType getDataSourceType() {
        DataSourceType type = CONTEXT_HOLDER.get();
        if (type == null) {
            log.debug("未设置数据源类型，使用默认主库");
            return DataSourceType.MASTER;
        }
        return type;
    }

    /**
     * 清除数据源类型
     */
    public static void clearDataSourceType() {
        CONTEXT_HOLDER.remove();
        log.debug("清除数据源上下文");
    }
}
