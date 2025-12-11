package com.basebackend.scheduler.performance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.io.Serializable;
import java.util.List;

/**
 * 游标分页查询优化
 *
 * <p>解决深度分页性能问题，适用于大数据量分页查询场景。
 * 传统 LIMIT offset, size 在 offset 很大时性能极差，原因是：
 * 1. 数据库需要扫描 offset + size 条记录
 * 2. 跳过大量记录浪费 IO
 *
 * <p>游标分页原理：
 * - 使用稳定的排序字段（如 ID、时间戳）作为游标
 * - 每次查询时使用游标过滤，不再需要扫描前面的记录
 * - 查询复杂度从 O(offset + size) 降低到 O(size)
 *
 * <p>使用场景：
 * - 大数据量分页（总记录数 > 10万）
 * - 深度分页（页码 > 100）
 * - 实时性要求高的查询
 * - 无限滚动加载
 *
 * <p>优点：
 * <ul>
 *   <li>性能稳定：查询时间与页码无关</li>
 *   <li>内存友好：不需要跳过大量记录</li>
 *   <li>实时性好：避免"幻读"问题</li>
 * </ul>
 *
 * <p>限制：
 * <ul>
 *   <li>需要稳定的排序字段</li>
 *   <li>不能跳转到指定页码</li>
 *   <li>数据更新时游标可能失效</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
public class CursorPageQuery<T> {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CursorPageQuery.class);

    /**
     * 页大小
     */
    private int size;

    /**
     * 游标值（上一批数据的最后一个值）
     * 第一次查询时为 null
     */
    private String cursor;

    /**
     * 是否还有更多数据
     */
    private boolean hasMore;

    /**
     * 当前页数据列表
     */
    private List<T> data;

    /**
     * 总记录数（可选，用于统计）
     */
    private Long total;

    /**
     * 下一页游标
     */
    private String nextCursor;

    // ========== Getters and Setters ==========

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getCursor() {
        return cursor;
    }

    public void setCursor(String cursor) {
        this.cursor = cursor;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public String getNextCursor() {
        return nextCursor;
    }

    public void setNextCursor(String nextCursor) {
        this.nextCursor = nextCursor;
    }

    /**
     * 执行游标分页查询
     *
     * @param queryFunction 查询函数，接收 Page 和 cursor 参数
     * @param <R> 返回类型
     * @return 游标分页结果
     */
    public <R extends IPage<T>> R execute(java.util.function.BiFunction<Page<T>, String, R> queryFunction) {
        long startTime = System.currentTimeMillis();

        Page<T> page = new Page<>(1, size);
        R result = queryFunction.apply(page, cursor);

        this.data = result.getRecords();
        this.total = result.getTotal();
        this.hasMore = result.getRecords().size() == size;

        // 计算下一页游标
        if (!result.getRecords().isEmpty()) {
            T lastRecord = result.getRecords().get(result.getRecords().size() - 1);
            this.nextCursor = extractCursorValue(lastRecord);
        } else {
            this.nextCursor = null;
        }

        long duration = System.currentTimeMillis() - startTime;
        log.debug("Cursor pagination executed [size={}, cursor={}, hasMore={}, duration={}ms]",
                size, cursor, hasMore, duration);

        return result;
    }

    /**
     * 提取游标值
     *
     * @param record 记录
     * @return 游标值
     */
    private String extractCursorValue(T record) {
        // TODO: 根据具体实体类型提取游标值
        // 常见策略：
        // 1. 使用主键（ID）
        // 2. 使用创建时间 + 主键组合
        // 3. 使用业务唯一标识

        // 示例：假设实体有 getId() 方法
        if (record instanceof BaseEntity) {
            BaseEntity entity = (BaseEntity) record;
            return entity.getId().toString();
        }

        // 默认实现：返回 toString()
        log.warn("Unable to extract cursor value from record: {}", record.getClass().getName());
        return record.toString();
    }

    /**
     * 创建游标分页查询请求
     *
     * @param size 页大小
     * @param cursor 游标
     * @return 请求对象
     */
    public static <T> CursorPageQuery<T> of(int size, String cursor) {
        CursorPageQuery<T> query = new CursorPageQuery<>();
        query.setSize(size);
        query.setCursor(cursor);
        return query;
    }

    /**
     * 创建第一次查询请求（无游标）
     *
     * @param size 页大小
     * @return 请求对象
     */
    public static <T> CursorPageQuery<T> firstPage(int size) {
        return of(size, null);
    }

    /**
     * 基础实体接口
     */
    public interface BaseEntity extends Serializable {
        Serializable getId();
    }

    /**
     * 游标分页查询结果包装类
     */
    public static class CursorPageResult<T> implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 数据列表
         */
        private List<T> data;

        /**
         * 下一页游标
         */
        private String nextCursor;

        /**
         * 是否还有更多数据
         */
        private boolean hasMore;

        /**
         * 当前页大小
         */
        private int size;

        /**
         * 总记录数（可选）
         */
        private Long total;

        // ========== Getters and Setters ==========

        public List<T> getData() {
            return data;
        }

        public void setData(List<T> data) {
            this.data = data;
        }

        public String getNextCursor() {
            return nextCursor;
        }

        public void setNextCursor(String nextCursor) {
            this.nextCursor = nextCursor;
        }

        public boolean isHasMore() {
            return hasMore;
        }

        public void setHasMore(boolean hasMore) {
            this.hasMore = hasMore;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public Long getTotal() {
            return total;
        }

        public void setTotal(Long total) {
            this.total = total;
        }

        public static <T> CursorPageResult<T> of(List<T> data, String nextCursor, boolean hasMore, int size) {
            CursorPageResult<T> result = new CursorPageResult<>();
            result.setData(data);
            result.setNextCursor(nextCursor);
            result.setHasMore(hasMore);
            result.setSize(size);
            return result;
        }

        public static <T> CursorPageResult<T> of(List<T> data, String nextCursor, boolean hasMore, int size, Long total) {
            CursorPageResult<T> result = of(data, nextCursor, hasMore, size);
            result.setTotal(total);
            return result;
        }
    }
}
