package com.basebackend.database.routing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * 动态数据源路由
 * 根据上下文中的数据源类型，动态切换到对应的数据源
 *
 * @author 浮浮酱
 */
@Slf4j
public class DynamicDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        DataSourceType dataSourceType = DataSourceContextHolder.getDataSourceType();
        log.debug("当前数据源: {}", dataSourceType);
        return dataSourceType;
    }

    @Override
    protected Object resolveSpecifiedLookupKey(Object lookupKey) {
        // 如果是字符串，转换为枚举
        if (lookupKey instanceof String) {
            try {
                return DataSourceType.valueOf(((String) lookupKey).toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("无效的数据源类型: {}, 使用默认主库", lookupKey);
                return DataSourceType.MASTER;
            }
        }
        return lookupKey;
    }
}
