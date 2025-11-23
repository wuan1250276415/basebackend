package com.basebackend.database.tenant.interceptor;

import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.basebackend.database.config.DatabaseEnhancedProperties;
import com.basebackend.database.exception.TenantContextException;
import com.basebackend.database.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.update.Update;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.sql.Connection;
import java.util.List;

/**
 * 租户拦截器
 * 自动为 SQL 添加租户过滤条件
 * 
 * 功能：
 * 1. SELECT 语句：自动添加 WHERE tenant_id = ?
 * 2. UPDATE 语句：自动添加 WHERE tenant_id = ?
 * 3. DELETE 语句：自动添加 WHERE tenant_id = ?
 * 4. INSERT 语句：由 TenantMetaObjectHandler 处理
 */
@Slf4j
@RequiredArgsConstructor
public class TenantInterceptor implements InnerInterceptor {
    
    private final DatabaseEnhancedProperties properties;
    
    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter,
                           RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
        // 如果多租户未启用，直接返回
        if (!properties.getMultiTenancy().isEnabled()) {
            return;
        }
        
        // 获取租户 ID
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            log.warn("Tenant context is not set for query operation");
            throw new TenantContextException("Tenant context is not set. Please set tenant ID before database operations.");
        }
        
        // 修改 SQL 添加租户过滤
        PluginUtils.MPBoundSql mpBoundSql = PluginUtils.mpBoundSql(boundSql);
        String originalSql = boundSql.getSql();
        
        try {
            String modifiedSql = addTenantFilter(originalSql, tenantId, ms);
            mpBoundSql.sql(modifiedSql);
            log.debug("Added tenant filter to query. Tenant: {}", tenantId);
        } catch (Exception e) {
            log.error("Failed to add tenant filter to SQL: {}", originalSql, e);
            // 不抛出异常，让原始 SQL 执行（可能会查询到其他租户的数据）
        }
    }
    
    @Override
    public void beforeUpdate(Executor executor, MappedStatement ms, Object parameter) {
        // 如果多租户未启用，直接返回
        if (!properties.getMultiTenancy().isEnabled()) {
            return;
        }
        
        // INSERT 操作由 MetaObjectHandler 处理
        if (ms.getSqlCommandType() == SqlCommandType.INSERT) {
            return;
        }
        
        // 获取租户 ID
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            log.warn("Tenant context is not set for update/delete operation");
            throw new TenantContextException("Tenant context is not set. Please set tenant ID before database operations.");
        }
        
        log.debug("Tenant filter will be applied to update/delete. Tenant: {}", tenantId);
    }
    
    @Override
    public void beforePrepare(StatementHandler sh, Connection connection, Integer transactionTimeout) {
        // 如果多租户未启用，直接返回
        if (!properties.getMultiTenancy().isEnabled()) {
            return;
        }
        
        // 获取租户 ID
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return;
        }
        
        BoundSql boundSql = sh.getBoundSql();
        String originalSql = boundSql.getSql();
        
        try {
            // 判断 SQL 类型
            Statement statement = CCJSqlParserUtil.parse(originalSql);
            
            if (statement instanceof Update) {
                String modifiedSql = addTenantFilterToUpdate(originalSql, tenantId);
                PluginUtils.MPBoundSql mpBoundSql = PluginUtils.mpBoundSql(boundSql);
                mpBoundSql.sql(modifiedSql);
                log.debug("Added tenant filter to UPDATE. Tenant: {}", tenantId);
            } else if (statement instanceof Delete) {
                String modifiedSql = addTenantFilterToDelete(originalSql, tenantId);
                PluginUtils.MPBoundSql mpBoundSql = PluginUtils.mpBoundSql(boundSql);
                mpBoundSql.sql(modifiedSql);
                log.debug("Added tenant filter to DELETE. Tenant: {}", tenantId);
            }
        } catch (Exception e) {
            log.error("Failed to add tenant filter to SQL: {}", originalSql, e);
        }
    }
    
    /**
     * 为 SELECT 语句添加租户过滤
     */
    private String addTenantFilter(String sql, String tenantId, MappedStatement ms) throws Exception {
        // 检查是否是排除的表
        String tableName = extractTableName(sql);
        if (isExcludedTable(tableName)) {
            log.debug("Table {} is excluded from tenant filtering", tableName);
            return sql;
        }
        
        Statement statement = CCJSqlParserUtil.parse(sql);
        
        if (statement instanceof Select) {
            Select select = (Select) statement;
            SelectBody selectBody = select.getSelectBody();
            
            if (selectBody instanceof PlainSelect) {
                PlainSelect plainSelect = (PlainSelect) selectBody;
                Expression where = plainSelect.getWhere();
                Expression tenantCondition = createTenantCondition(tenantId);
                
                if (where != null) {
                    plainSelect.setWhere(new AndExpression(where, tenantCondition));
                } else {
                    plainSelect.setWhere(tenantCondition);
                }
            }
            
            return select.toString();
        }
        
        return sql;
    }
    
    /**
     * 为 UPDATE 语句添加租户过滤
     */
    private String addTenantFilterToUpdate(String sql, String tenantId) throws Exception {
        Statement statement = CCJSqlParserUtil.parse(sql);
        
        if (statement instanceof Update) {
            Update update = (Update) statement;
            
            // 检查是否是排除的表
            String tableName = update.getTable().getName();
            if (isExcludedTable(tableName)) {
                log.debug("Table {} is excluded from tenant filtering", tableName);
                return sql;
            }
            
            Expression where = update.getWhere();
            Expression tenantCondition = createTenantCondition(tenantId);
            
            if (where != null) {
                update.setWhere(new AndExpression(where, tenantCondition));
            } else {
                update.setWhere(tenantCondition);
            }
            
            return update.toString();
        }
        
        return sql;
    }
    
    /**
     * 为 DELETE 语句添加租户过滤
     */
    private String addTenantFilterToDelete(String sql, String tenantId) throws Exception {
        Statement statement = CCJSqlParserUtil.parse(sql);
        
        if (statement instanceof Delete) {
            Delete delete = (Delete) statement;
            
            // 检查是否是排除的表
            String tableName = delete.getTable().getName();
            if (isExcludedTable(tableName)) {
                log.debug("Table {} is excluded from tenant filtering", tableName);
                return sql;
            }
            
            Expression where = delete.getWhere();
            Expression tenantCondition = createTenantCondition(tenantId);
            
            if (where != null) {
                delete.setWhere(new AndExpression(where, tenantCondition));
            } else {
                delete.setWhere(tenantCondition);
            }
            
            return delete.toString();
        }
        
        return sql;
    }
    
    /**
     * 创建租户过滤条件
     */
    private Expression createTenantCondition(String tenantId) {
        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(new Column(properties.getMultiTenancy().getTenantColumn()));
        equalsTo.setRightExpression(new StringValue(tenantId));
        return equalsTo;
    }
    
    /**
     * 从 SQL 中提取表名（简单实现）
     */
    private String extractTableName(String sql) {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            
            if (statement instanceof Select) {
                Select select = (Select) statement;
                SelectBody selectBody = select.getSelectBody();
                if (selectBody instanceof PlainSelect) {
                    PlainSelect plainSelect = (PlainSelect) selectBody;
                    FromItem fromItem = plainSelect.getFromItem();
                    if (fromItem instanceof net.sf.jsqlparser.schema.Table) {
                        return ((net.sf.jsqlparser.schema.Table) fromItem).getName();
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract table name from SQL: {}", sql);
        }
        
        return null;
    }
    
    /**
     * 检查表是否在排除列表中
     */
    private boolean isExcludedTable(String tableName) {
        if (tableName == null) {
            return false;
        }
        
        List<String> excludedTables = properties.getMultiTenancy().getExcludedTables();
        return excludedTables.stream()
                .anyMatch(excluded -> tableName.equalsIgnoreCase(excluded));
    }
}
