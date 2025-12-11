package com.basebackend.generator.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.basebackend.database.entity.BaseEntity;
import com.basebackend.generator.handler.PasswordEncryptTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据源配置实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "gen_datasource", autoResultMap = true)
public class GenDataSource extends BaseEntity {

    /**
     * 数据源名称
     */
    @TableField("name")
    private String name;

    /**
     * 数据库类型
     */
    @TableField("db_type")
    private String dbType;

    /**
     * 主机地址
     */
    @TableField("host")
    private String host;

    /**
     * 端口
     */
    @TableField("port")
    private Integer port;

    /**
     * 数据库名
     */
    @TableField("database_name")
    private String databaseName;

    /**
     * 用户名
     */
    @TableField("username")
    private String username;

    /**
     * 密码（自动加密存储，查询时自动解密）
     */
    @TableField(value = "password", typeHandler = PasswordEncryptTypeHandler.class)
    private String password;

    /**
     * 连接参数JSON
     */
    @TableField("connection_params")
    private String connectionParams;

    /**
     * 状态：0-禁用，1-启用
     */
    @TableField("status")
    private Integer status;
}
