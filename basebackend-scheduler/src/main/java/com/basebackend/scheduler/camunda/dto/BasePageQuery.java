package com.basebackend.scheduler.camunda.dto;

import com.basebackend.scheduler.camunda.config.PaginationConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 分页查询基类
 *
 * <p>所有分页查询 DTO 的公共父类，包含标准的分页字段和验证规则。
 * 子类继承后只需定义各自的过滤条件字段。
 *
 * <p>设计原则：
 * <ul>
 *   <li>统一分页字段定义，避免重复代码</li>
 *   <li>集中管理分页验证规则和 Swagger 文档</li>
 *   <li>提供兼容性方法支持不同的命名习惯</li>
 *   <li>所有分页配置值从 {@link PaginationConstants} 获取</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@Schema(name = "BasePageQuery", description = "分页查询基类")
public class BasePageQuery {

    /**
     * 当前页码，从 1 开始
     */
    @Schema(
        description = "当前页码，从 1 开始",
        defaultValue = "1",
        example = "1",
        minimum = "1"
    )
    @Min(value = 1, message = "当前页码必须大于等于 1")
    private int current = 1;

    /**
     * 每页大小
     *
     * <p>默认值和最大值从 {@link PaginationConstants} 获取，
     * 确保与项目分页策略一致。
     */
    @Schema(
        description = "每页大小",
        defaultValue = "10",
        example = "10",
        minimum = "1",
        maximum = "200"
    )
    @Min(value = 1, message = "每页大小必须大于等于 1")
    @Max(
        value = 200,
        message = "每页大小不能超过 200"
    )
    private int size = 10;

    /**
     * 获取页码（兼容命名）
     *
     * <p>某些 Service 方法使用 pageNum 命名，提供此方法以兼容。
     *
     * @return 当前页码
     */
    public int getPageNum() {
        return current;
    }

    /**
     * 获取页大小（兼容命名）
     *
     * <p>某些 Service 方法使用 pageSize 命名，提供此方法以兼容。
     *
     * @return 每页大小
     */
    public int getPageSize() {
        return size;
    }

    /**
     * 设置页码（兼容 pageNum 参数名）
     *
     * @param pageNum 页码
     */
    public void setPageNum(int pageNum) {
        this.current = pageNum;
    }

    /**
     * 设置每页大小（兼容 pageSize 参数名）
     *
     * @param pageSize 每页大小
     */
    public void setPageSize(int pageSize) {
        this.size = pageSize;
    }
}
