package com.basebackend.common.model;

import lombok.Data;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分页响应对象
 * <p>
 * 用于封装分页查询的响应数据，包含分页元信息和数据列表。
 * 支持与 {@link com.basebackend.common.dto.PageQuery} 配合使用。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 从 MyBatis-Plus Page 对象创建
 * PageResult<UserVO> result = PageResult.of(page.getRecords(), page.getTotal(),
 *                                            page.getCurrent(), page.getSize());
 *
 * // 创建空分页结果
 * PageResult<UserVO> empty = PageResult.empty();
 *
 * // 数据转换
 * PageResult<UserVO> voResult = result.map(user -> convertToVO(user));
 * }</pre>
 *
 * @param <T> 数据类型
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前页码（从1开始）
     */
    private Long current;

    /**
     * 每页大小
     */
    private Long size;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 总页数
     */
    private Long pages;

    /**
     * 数据列表
     */
    private List<T> records;

    public PageResult() {
        this.records = Collections.emptyList();
    }

    public PageResult(Long current, Long size, Long total, List<T> records) {
        this.current = current;
        this.size = size;
        this.total = total;
        this.records = records != null ? records : Collections.emptyList();
        this.pages = calculatePages(total, size);
    }

    // ========== 静态工厂方法 ==========

    /**
     * 创建分页结果
     *
     * @param records 数据列表
     * @param total   总记录数
     * @param current 当前页码
     * @param size    每页大小
     * @param <T>     数据类型
     * @return 分页结果
     */
    public static <T> PageResult<T> of(List<T> records, Long total, Long current, Long size) {
        return new PageResult<>(current, size, total, records);
    }

    /**
     * 创建分页结果（整型参数）
     *
     * @param records 数据列表
     * @param total   总记录数
     * @param current 当前页码
     * @param size    每页大小
     * @param <T>     数据类型
     * @return 分页结果
     */
    public static <T> PageResult<T> of(List<T> records, long total, int current, int size) {
        return new PageResult<>((long) current, (long) size, total, records);
    }

    /**
     * 创建空的分页结果
     *
     * @param <T> 数据类型
     * @return 空分页结果
     */
    public static <T> PageResult<T> empty() {
        return new PageResult<>(1L, 10L, 0L, Collections.emptyList());
    }

    /**
     * 创建空的分页结果（指定分页参数）
     *
     * @param current 当前页码
     * @param size    每页大小
     * @param <T>     数据类型
     * @return 空分页结果
     */
    public static <T> PageResult<T> empty(long current, long size) {
        return new PageResult<>(current, size, 0L, Collections.emptyList());
    }

    // ========== 便捷方法 ==========

    /**
     * 数据转换
     * <p>
     * 将当前分页结果中的数据转换为另一种类型。
     * </p>
     *
     * @param converter 转换函数
     * @param <R>       目标类型
     * @return 转换后的分页结果
     */
    public <R> PageResult<R> map(Function<T, R> converter) {
        List<R> convertedRecords = this.records.stream()
                .map(converter)
                .collect(Collectors.toList());
        return new PageResult<>(this.current, this.size, this.total, convertedRecords);
    }

    /**
     * 判断是否有数据
     *
     * @return 是否有数据
     */
    public boolean hasRecords() {
        return records != null && !records.isEmpty();
    }

    /**
     * 判断是否为空
     *
     * @return 是否为空
     */
    public boolean isEmpty() {
        return !hasRecords();
    }

    /**
     * 判断是否有下一页
     *
     * @return 是否有下一页
     */
    public boolean hasNext() {
        return current != null && pages != null && current < pages;
    }

    /**
     * 判断是否有上一页
     *
     * @return 是否有上一页
     */
    public boolean hasPrevious() {
        return current != null && current > 1;
    }

    /**
     * 判断是否为第一页
     *
     * @return 是否为第一页
     */
    public boolean isFirst() {
        return current != null && current == 1;
    }

    /**
     * 判断是否为最后一页
     *
     * @return 是否为最后一页
     */
    public boolean isLast() {
        return current != null && pages != null && current.equals(pages);
    }

    /**
     * 获取记录数量
     *
     * @return 当前页记录数量
     */
    public int getRecordCount() {
        return records != null ? records.size() : 0;
    }

    // ========== 私有方法 ==========

    /**
     * 计算总页数
     */
    private static Long calculatePages(Long total, Long size) {
        if (total == null || size == null || size <= 0) {
            return 0L;
        }
        return (total + size - 1) / size;
    }
}
