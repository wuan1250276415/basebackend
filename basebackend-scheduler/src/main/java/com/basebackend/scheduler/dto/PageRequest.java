package com.basebackend.scheduler.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 分页请求基类
 * <p>
 * 提供标准的分页查询参数，包含页码、每页大小等信息。
 * 所有需要分页的查询接口都应继承此类。
 * </p>
 * <p>
 * 分页参数的默认值和上限可通过配置文件调整（scheduler.page.*）。
 * </p>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-24
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PageRequest extends BaseRequest {

    private static final long serialVersionUID = 1L;

    /**
     * 页码（从1开始）
     * <p>
     * 注意：实际的默认值和上限由PageProperties配置类管理
     * </p>
     */
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码必须大于等于1")
    private Integer pageNo = 1;

    /**
     * 每页大小
     * <p>
     * 注意：实际的默认值和上限由PageProperties配置类管理
     * </p>
     */
    @NotNull(message = "每页大小不能为空")
    @Min(value = 1, message = "每页大小必须大于等于1")
    @Max(value = 200, message = "每页大小不能超过200")
    private Integer pageSize = 20;

    /**
     * 排序字段（可选）
     * <p>
     * 例如：createTime, updateTime等
     * </p>
     */
    private String sortField;

    /**
     * 排序方式（可选）
     * <p>
     * ASC: 升序, DESC: 降序
     * </p>
     */
    private String sortOrder;

    /**
     * 计算偏移量（用于数据库查询）
     *
     * @return 偏移量
     */
    public int getOffset() {
        return (pageNo - 1) * pageSize;
    }

    /**
     * 获取限制数量（用于数据库查询）
     *
     * @return 限制数量
     */
    public int getLimit() {
        return pageSize;
    }

    /**
     * 静态工厂方法 - 创建分页请求
     *
     * @param pageNo   页码
     * @param pageSize 每页大小
     * @return PageRequest实例
     */
    public static PageRequest of(Integer pageNo, Integer pageSize) {
        PageRequest request = new PageRequest();
        request.setPageNo(pageNo);
        request.setPageSize(pageSize);
        return request;
    }

    /**
     * 静态工厂方法 - 创建带排序的分页请求
     *
     * @param pageNo    页码
     * @param pageSize  每页大小
     * @param sortField 排序字段
     * @param sortOrder 排序方式
     * @return PageRequest实例
     */
    public static PageRequest of(Integer pageNo, Integer pageSize, String sortField, String sortOrder) {
        PageRequest request = of(pageNo, pageSize);
        request.setSortField(sortField);
        request.setSortOrder(sortOrder);
        return request;
    }
}
