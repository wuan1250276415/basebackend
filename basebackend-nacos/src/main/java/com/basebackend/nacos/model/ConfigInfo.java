package com.basebackend.nacos.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.io.Serializable;

/**
 * Nacos配置信息模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigInfo implements Serializable {

    /**
     * 配置ID（数据库ID）
     */
    private Long id;

    /**
     * 配置Data ID
     */
    private String dataId;

    /**
     * 配置分组
     */
    private String group;

    /**
     * 命名空间
     */
    private String namespace;

    /**
     * 配置内容
     */
    private String content;

    /**
     * 配置类型（yaml/properties/json/xml/text）
     */
    private String type;

    /**
     * 环境（dev/test/prod等）
     */
    private String environment;

    /**
     * 租户ID
     */
    private String tenantId;

    /**
     * 应用ID
     */
    private Long appId;

    /**
     * 配置版本
     */
    private Integer version;

    /**
     * 配置状态（draft/published/archived）
     */
    private String status;

    /**
     * 是否关键配置（true需要手动发布，false自动发布）
     */
    private Boolean isCritical;

    /**
     * 发布类型（auto/manual/gray）
     */
    private String publishType;

    /**
     * 配置描述
     */
    private String description;

    /**
     * MD5值
     */
    private String md5;

    /**
     * 获取服务名
     * 从Data ID中提取服务名，格式：{serviceName}-{env}.yml
     */
    public String getServiceName() {
        if (!StringUtils.hasText(dataId)) {
            return null;
        }

        // 如果dataId包含"-"分隔符，取第一部分作为服务名
        if (dataId.contains("-")) {
            return dataId.split("-")[0];
        }

        // 如果dataId是纯文本，直接返回
        return dataId;
    }
}
