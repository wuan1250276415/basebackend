package com.basebackend.scheduler.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Collections;
import java.util.List;

/**
 * 分页响应类
 * <p>
 * 包含分页数据列表和总记录数，用于前端渲染分页组件。
 * </p>
 *
 * @param <T> 列表元素类型
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-24
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PageResponse<T> extends BaseResponse<List<T>> {

    private static final long serialVersionUID = 1L;

    /**
     * 总记录数
     */
    private long total;

    /**
     * 总页数
     */
    private int totalPages;

    /**
     * 当前页码
     */
    private int pageNo;

    /**
     * 每页大小
     */
    private int pageSize;

    /**
     * 是否有下一页
     */
    private boolean hasNext;

    /**
     * 是否有上一页
     */
    private boolean hasPrevious;

    /**
     * 创建成功的分页响应
     *
     * @param records  当前页数据列表
     * @param total    总记录数
     * @param pageNo   当前页码
     * @param pageSize 每页大小
     * @param <T>      列表元素类型
     * @return 分页响应对象
     */
    public static <T> PageResponse<T> success(List<T> records, long total, int pageNo, int pageSize) {
        PageResponse<T> response = new PageResponse<>();
        response.setSuccess(true);
        response.setCode("200");
        response.setMessage("查询成功");
        response.setTimestamp(java.time.LocalDateTime.now());
        response.setData(records == null ? Collections.emptyList() : records);
        response.setTotal(total);
        response.setPageNo(pageNo);
        response.setPageSize(pageSize);

        // 计算总页数
        int totalPages = (int) Math.ceil((double) total / pageSize);
        response.setTotalPages(totalPages);

        // 计算是否有上一页/下一页
        response.setHasPrevious(pageNo > 1);
        response.setHasNext(pageNo < totalPages);

        return response;
    }

    /**
     * 创建空的分页响应（无数据）
     *
     * @param <T> 列表元素类型
     * @return 空分页响应对象
     */
    public static <T> PageResponse<T> empty() {
        return success(Collections.emptyList(), 0, 1, 20);
    }
}
