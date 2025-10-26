package com.basebackend.generator.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 生成历史实体
 */
@Data
@TableName("gen_history")
public class GenHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 项目ID
     */
    @TableField("project_id")
    private Long projectId;

    /**
     * 数据源ID
     */
    @TableField("datasource_id")
    private Long datasourceId;

    /**
     * 生成的表名
     */
    @TableField("table_names")
    private String tableNames;

    /**
     * 模板分组ID
     */
    @TableField("template_group_id")
    private Long templateGroupId;

    /**
     * 生成类型
     */
    @TableField("generate_type")
    private String generateType;

    /**
     * 文件路径
     */
    @TableField("file_path")
    private String filePath;

    /**
     * 文件数量
     */
    @TableField("file_count")
    private Integer fileCount;

    /**
     * 状态
     */
    @TableField("status")
    private String status;

    /**
     * 错误信息
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 生成配置JSON
     */
    @TableField("generate_config")
    private String generateConfig;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 创建人
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private Long createBy;
}
