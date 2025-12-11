package com.basebackend.featuretoggle.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 变体（用于AB测试）
 *
 * @author BaseBackend
 */
@Data
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

    public static Builder builder() {
        return new Builder();
    }

    /**
     * 创建默认变体
     */
    public static Variant defaultVariant() {
        return Variant.builder()
                .name("disabled")
                .enabled(false)
                .build();
    }

    public static class Builder {
        private String name;
        private Integer weight;
        private Boolean enabled;
        private String payload;
        private Boolean control;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder weight(Integer weight) {
            this.weight = weight;
            return this;
        }

        public Builder enabled(Boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder payload(String payload) {
            this.payload = payload;
            return this;
        }

        public Builder control(Boolean control) {
            this.control = control;
            return this;
        }

        public Variant build() {
            return new Variant(name, weight, enabled, payload, control);
        }
    }
}
