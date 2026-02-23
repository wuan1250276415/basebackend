package com.basebackend.common.dto;

import lombok.Data;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 分页查询基类
 * <p>
 * 所有需要分页的查询请求都应继承此类。
 * 提供分页参数和排序参数的统一管理。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * public class UserQueryDTO extends PageQuery {
 *     private String username;
 *     private Integer status;
 * }
 *
 * // 控制器中使用
 * @GetMapping("/users")
 * public Result<PageResult<UserVO>> listUsers(UserQueryDTO query) {
 *     return userService.page(query);
 * }
 * }</pre>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Data
public class PageQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 默认页码
     */
    public static final int DEFAULT_PAGE = 1;

    /**
     * 默认每页大小
     */
    public static final int DEFAULT_SIZE = 10;

    /**
     * 最大每页大小
     */
    public static final int MAX_SIZE = 1000;

    /**
     * 当前页码（从1开始）
     */
    private Integer pageNum;

    /**
     * 每页大小
     */
    private Integer pageSize;

    /**
     * 排序字段列表
     */
    private List<SortItem> sorts;

    /**
     * 获取当前页码
     * <p>
     * 如果未设置或设置为小于1的值，返回默认值1。
     * </p>
     *
     * @return 当前页码
     */
    public Integer getPageNum() {
        if (pageNum == null || pageNum < 1) {
            return DEFAULT_PAGE;
        }
        return pageNum;
    }

    /**
     * 获取每页大小
     * <p>
     * 如果未设置或设置为小于1的值，返回默认值10。
     * 如果设置值超过最大限制，返回最大限制值。
     * </p>
     *
     * @return 每页大小
     */
    public Integer getPageSize() {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_SIZE;
        }
        if (pageSize > MAX_SIZE) {
            return MAX_SIZE;
        }
        return pageSize;
    }

    /**
     * 获取偏移量
     * <p>
     * 用于数据库 OFFSET 计算。
     * </p>
     *
     * @return 偏移量
     */
    public long getOffset() {
        return (long) (getPageNum() - 1) * getPageSize();
    }

    /**
     * 获取排序字段列表
     *
     * @return 排序字段列表，不会返回 null
     */
    public List<SortItem> getSorts() {
        if (sorts == null) {
            sorts = new ArrayList<>();
        }
        return sorts;
    }

    /**
     * 添加排序字段
     *
     * @param field 字段名
     * @param asc   是否升序
     * @return 当前对象（支持链式调用）
     */
    public PageQuery addSort(String field, boolean asc) {
        getSorts().add(new SortItem(field, asc));
        return this;
    }

    /**
     * 添加升序排序字段
     *
     * @param field 字段名
     * @return 当前对象（支持链式调用）
     */
    public PageQuery orderByAsc(String field) {
        return addSort(field, true);
    }

    /**
     * 添加降序排序字段
     *
     * @param field 字段名
     * @return 当前对象（支持链式调用）
     */
    public PageQuery orderByDesc(String field) {
        return addSort(field, false);
    }

    /**
     * 排序项
     */
    @Data
    public static class SortItem implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 排序字段名
         */
        private String field;

        /**
         * 是否升序
         */
        private Boolean asc;

        public SortItem() {
        }

        public SortItem(String field, Boolean asc) {
            this.field = field;
            this.asc = asc;
        }

        /**
         * 获取是否升序
         *
         * @return 是否升序，默认返回 true
         */
        public Boolean getAsc() {
            return asc == null || asc;
        }
    }
}
