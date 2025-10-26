package com.basebackend.generator.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 字段类型映射实体
 */
@Data
@TableName("gen_type_mapping")
public class GenTypeMapping implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 数据库类型
     */
    @TableField("db_type")
    private String dbType;

    /**
     * 数据库字段类型
     */
    @TableField("column_type")
    private String columnType;

    /**
     * Java类型
     */
    @TableField("java_type")
    private String javaType;

    /**
     * TypeScript类型
     */
    @TableField("ts_type")
    private String tsType;

    /**
     * Java导入包
     */
    @TableField("import_package")
    private String importPackage;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;
}
