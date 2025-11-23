package com.basebackend.database.dynamic.context;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 数据源上下文持有者
 * 使用栈结构支持嵌套数据源切换
 * 
 * @author basebackend
 */
@Slf4j
public class DataSourceContextHolder {
    
    /**
     * 使用栈来支持嵌套的数据源切换
     */
    private static final ThreadLocal<Deque<String>> CONTEXT_HOLDER = ThreadLocal.withInitial(ArrayDeque::new);
    
    /**
     * 设置当前数据源
     * 
     * @param dataSourceKey 数据源键
     */
    public static void setDataSourceKey(String dataSourceKey) {
        if (dataSourceKey == null || dataSourceKey.trim().isEmpty()) {
            log.warn("Attempting to set null or empty datasource key, ignoring");
            return;
        }
        
        Deque<String> stack = CONTEXT_HOLDER.get();
        stack.push(dataSourceKey);
        
        log.debug("Set datasource to: {}, stack depth: {}", dataSourceKey, stack.size());
    }
    
    /**
     * 获取当前数据源键
     * 
     * @return 数据源键，如果栈为空则返回 null
     */
    public static String getDataSourceKey() {
        Deque<String> stack = CONTEXT_HOLDER.get();
        return stack.isEmpty() ? null : stack.peek();
    }
    
    /**
     * 清除当前数据源（弹出栈顶）
     * 用于方法执行完成后恢复到上一个数据源
     */
    public static void clearDataSourceKey() {
        Deque<String> stack = CONTEXT_HOLDER.get();
        if (!stack.isEmpty()) {
            String removed = stack.pop();
            log.debug("Cleared datasource: {}, remaining stack depth: {}", removed, stack.size());
        }
        
        // 如果栈为空，清理 ThreadLocal
        if (stack.isEmpty()) {
            CONTEXT_HOLDER.remove();
        }
    }
    
    /**
     * 完全清空数据源上下文
     * 用于线程结束时清理
     */
    public static void clear() {
        Deque<String> stack = CONTEXT_HOLDER.get();
        if (!stack.isEmpty()) {
            log.debug("Clearing all datasource context, stack depth was: {}", stack.size());
            stack.clear();
        }
        CONTEXT_HOLDER.remove();
    }
    
    /**
     * 获取当前栈深度（用于调试）
     * 
     * @return 栈深度
     */
    public static int getStackDepth() {
        return CONTEXT_HOLDER.get().size();
    }
}
