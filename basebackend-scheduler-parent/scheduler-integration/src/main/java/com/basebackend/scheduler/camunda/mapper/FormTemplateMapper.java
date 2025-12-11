package com.basebackend.scheduler.camunda.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.scheduler.camunda.entity.FormTemplateEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 表单模板 Mapper
 *
 * <p>继承 MyBatis Plus 的 BaseMapper，提供标准 CRUD 操作。
 * 同时提供自定义查询方法支持实体条件查询。
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Mapper
public interface FormTemplateMapper extends BaseMapper<FormTemplateEntity> {

    /**
     * 根据实体条件分页查询表单模板
     * <p>
     * 注意：此方法为兼容旧代码保留。新代码建议使用 {@code LambdaQueryWrapper} 方式查询。
     * </p>
     *
     * @param entity 查询条件（包含分页参数 current、size、keyword 等）
     * @return 表单模板列表
     */
    @Select("<script>" +
            "SELECT * FROM camunda_form_template " +
            "<where>" +
            "  <if test='entity.tenantId != null and entity.tenantId != \"\"'>" +
            "    AND tenant_id = #{entity.tenantId}" +
            "  </if>" +
            "  <if test='entity.formType != null and entity.formType != \"\"'>" +
            "    AND form_type = #{entity.formType}" +
            "  </if>" +
            "  <if test='entity.status != null and entity.status != \"\"'>" +
            "    AND status = #{entity.status}" +
            "  </if>" +
            "  <if test='entity.keyword != null and entity.keyword != \"\"'>" +
            "    AND (name LIKE CONCAT('%', #{entity.keyword}, '%') OR description LIKE CONCAT('%', #{entity.keyword}, '%'))" +
            "  </if>" +
            "</where>" +
            "ORDER BY created_at DESC " +
            "<if test='entity.current != null and entity.size != null'>" +
            "  LIMIT #{entity.current}, #{entity.size}" +
            "</if>" +
            "</script>")
    List<FormTemplateEntity> selectList(@Param("entity") FormTemplateEntity entity);

    /**
     * 根据实体条件统计数量
     * <p>
     * 注意：此方法为兼容旧代码保留。新代码建议使用 {@code LambdaQueryWrapper} 方式查询。
     * </p>
     *
     * @param entity 查询条件
     * @return 符合条件的记录数量
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM camunda_form_template " +
            "<where>" +
            "  <if test='entity.tenantId != null and entity.tenantId != \"\"'>" +
            "    AND tenant_id = #{entity.tenantId}" +
            "  </if>" +
            "  <if test='entity.formType != null and entity.formType != \"\"'>" +
            "    AND form_type = #{entity.formType}" +
            "  </if>" +
            "  <if test='entity.status != null and entity.status != \"\"'>" +
            "    AND status = #{entity.status}" +
            "  </if>" +
            "  <if test='entity.keyword != null and entity.keyword != \"\"'>" +
            "    AND (name LIKE CONCAT('%', #{entity.keyword}, '%') OR description LIKE CONCAT('%', #{entity.keyword}, '%'))" +
            "  </if>" +
            "</where>" +
            "</script>")
    long selectCount(@Param("entity") FormTemplateEntity entity);
}
