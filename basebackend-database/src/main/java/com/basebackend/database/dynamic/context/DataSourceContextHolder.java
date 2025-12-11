package com.basebackend.database.dynamic.context;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Callable;

/**
 * 数据源上下文持有者
 * 使用栈结构支持嵌套数据源切换，支持线程池场景
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
     * 数据源上下文Guard
     * 提供try-with-resources模式，确保自动清理
     * 支持嵌套数据源切换，线程安全
     */
    public static class DataSourceContext implements AutoCloseable {
        private final String dataSourceKey;
        private final String previousDataSourceKey;

        public DataSourceContext(String dataSourceKey) {
            if (dataSourceKey == null || dataSourceKey.trim().isEmpty()) {
                throw new IllegalArgumentException("DataSource key cannot be null or empty");
            }

            this.dataSourceKey = dataSourceKey;
            this.previousDataSourceKey = getDataSourceKey();
            setDataSourceKey(dataSourceKey);

            if (log.isDebugEnabled()) {
                log.debug("Opened datasource context: {}, previous: {}",
                    dataSourceKey, previousDataSourceKey);
            }
        }

        @Override
        public void close() {
            try {
                clearDataSourceKey();
                if (log.isDebugEnabled()) {
                    log.debug("Closed datasource context: {}, restored to: {}",
                        dataSourceKey, getDataSourceKey());
                }
            } catch (Exception e) {
                log.error("Error closing datasource context", e);
            }
        }

        /**
         * 获取当前数据源键
         */
        public String getDataSourceKey() {
            return dataSourceKey;
        }

        /**
         * 获取前一个数据源键
         */
        public String getPreviousDataSourceKey() {
            return previousDataSourceKey;
        }
    }
    
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

    /**
     * 线程池清理钩子
     * 在提交到线程池之前调用，防止ThreadLocal泄漏
     *
     * @param runnable 要在线程池中执行的任务
     * @return 包装后的任务，会在执行后自动清理ThreadLocal
     */
    public static Runnable wrapForExecutor(Runnable runnable) {
        Deque<String> contextStack = CONTEXT_HOLDER.get();
        boolean hasContext = !contextStack.isEmpty();

        if (!hasContext) {
            return runnable; // 无上下文，直接返回
        }

        return () -> {
            try {
                runnable.run();
            } finally {
                // 确保在线程执行完毕后清理ThreadLocal
                clear();
            }
        };
    }

    /**
     * 线程池清理钩子（Callable版本）
     * 在提交到线程池之前调用，防止ThreadLocal泄漏
     *
     * @param callable 要在线程池中执行的任务
     * @param <T> 返回值类型
     * @return 包装后的任务，会在执行后自动清理ThreadLocal
     */
    public static <T> Callable<T> wrapForExecutor(Callable<T> callable) {
        Deque<String> contextStack = CONTEXT_HOLDER.get();
        boolean hasContext = !contextStack.isEmpty();

        if (!hasContext) {
            return callable; // 无上下文，直接返回
        }

        return () -> {
            try {
                return callable.call();
            } finally {
                // 确保在线程执行完毕后清理ThreadLocal
                clear();
            }
        };
    }

    /**
     * 创建数据源上下文
     * 推荐使用此方法而非直接调用构造器，支持try-with-resources
     *
     * @param dataSourceKey 数据源键
     * @return 数据源上下文对象
     */
    public static DataSourceContext createContext(String dataSourceKey) {
        return new DataSourceContext(dataSourceKey);
    }

    /**
     * 检查是否有活动的数据源上下文
     *
     * @return true if there is an active context, false otherwise
     */
    public static boolean hasContext() {
        return getDataSourceKey() != null;
    }

    /**
     * 获取上下文信息（用于调试）
     *
     * @return 上下文描述字符串
     */
    public static String getContextInfo() {
        String currentKey = getDataSourceKey();
        int depth = getStackDepth();
        return currentKey != null
            ? String.format("datasource=%s, depth=%d", currentKey, depth)
            : String.format("no datasource, depth=%d", depth);
    }
}
