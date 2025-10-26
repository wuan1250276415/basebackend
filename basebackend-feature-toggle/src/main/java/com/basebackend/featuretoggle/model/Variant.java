package com.basebackend.featuretoggle.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 变体（用于AB测试）
 *
 * @author BaseBackend
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Variant {

    /**
     * 变体名称（A/B/C等）
     */
    private String name;

    /**
     * 变体权重/比例
     */
    private Integer weight;

    /**
     * 变体是否启用
     */
    private Boolean enabled;

    /**
     * 变体负载数据（可选）
     */
    private String payload;

    /**
     * 是否为控制组
     */
    private Boolean control;

    /**
     * 创建默认变体
     */
    public static Variant defaultVariant() {
        return Variant.builder()
                .name("disabled")
                .enabled(false)
                .build();
    }
}
