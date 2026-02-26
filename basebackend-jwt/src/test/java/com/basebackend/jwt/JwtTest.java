package com.basebackend.jwt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JwtProperties + JwtUserDetails 单元测试
 */
class JwtTest {

    // ========== JwtProperties ==========

    @Nested
    @DisplayName("JwtProperties 默认值")
    class PropertiesDefaults {

        @Test
        @DisplayName("默认配置值正确")
        void shouldHaveCorrectDefaults() {
            var props = new JwtProperties();
            assertThat(props.getIssuer()).isEqualTo("basebackend");
            assertThat(props.getExpiration()).isEqualTo(86400000L);
            assertThat(props.getAccessTokenExpiration()).isEqualTo(1800000L);
            assertThat(props.getRefreshTokenExpiration()).isEqualTo(604800000L);
            assertThat(props.getExpiringSoonThreshold()).isEqualTo(3600000L);
            assertThat(props.isIncludePermissions()).isFalse();
            assertThat(props.getMaxDevicesPerUser()).isEqualTo(5);
            assertThat(props.isAuditEnabled()).isTrue();
            assertThat(props.isAuditPublishSpringEvents()).isFalse();
        }

        @Test
        @DisplayName("access < refresh < expiration 时间关系合理")
        void shouldHaveReasonableTimeRelationship() {
            var props = new JwtProperties();
            assertThat(props.getAccessTokenExpiration())
                    .isLessThan(props.getRefreshTokenExpiration());
        }
    }

    @Nested
    @DisplayName("JwtProperties 校验")
    class PropertiesValidation {

        @Test
        @DisplayName("secret 为空时抛异常")
        void shouldThrowWhenSecretBlank() {
            var props = new JwtProperties();
            props.setSecret(null);
            assertThatThrownBy(props::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("must be configured");
        }

        @Test
        @DisplayName("secret 太短时抛异常")
        void shouldThrowWhenSecretTooShort() {
            var props = new JwtProperties();
            props.setSecret("short");
            assertThatThrownBy(props::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("at least 32 characters");
        }

        @Test
        @DisplayName("secret 长度足够时通过校验")
        void shouldPassWhenSecretLongEnough() {
            var props = new JwtProperties();
            props.setSecret("a".repeat(32));
            props.validate(); // should not throw
        }

        @Test
        @DisplayName("多密钥模式缺少 activeKeyId 时抛异常")
        void shouldThrowWhenActiveKeyIdMissing() {
            var props = new JwtProperties();
            props.setSecret("a".repeat(32));
            props.setKeys(Map.of("key1", "b".repeat(32)));
            props.setActiveKeyId(null);
            assertThatThrownBy(props::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("active-key-id");
        }

        @Test
        @DisplayName("多密钥模式 activeKeyId 不在 keys 中时抛异常")
        void shouldThrowWhenActiveKeyIdNotInKeys() {
            var props = new JwtProperties();
            props.setSecret("a".repeat(32));
            props.setKeys(Map.of("key1", "b".repeat(32)));
            props.setActiveKeyId("key999");
            assertThatThrownBy(props::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("active-key-id");
        }

        @Test
        @DisplayName("多密钥模式密钥太短时抛异常")
        void shouldThrowWhenKeyTooShort() {
            var props = new JwtProperties();
            props.setSecret("a".repeat(32));
            props.setKeys(Map.of("key1", "short"));
            props.setActiveKeyId("key1");
            assertThatThrownBy(props::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("key1");
        }

        @Test
        @DisplayName("多密钥模式正确配置通过校验")
        void shouldPassWithValidMultiKeyConfig() {
            var props = new JwtProperties();
            props.setSecret("a".repeat(32));
            props.setKeys(Map.of("key1", "b".repeat(32), "key2", "c".repeat(32)));
            props.setActiveKeyId("key1");
            props.validate(); // should not throw
        }
    }

    // ========== JwtUserDetails ==========

    @Nested
    @DisplayName("JwtUserDetails")
    class UserDetailsTest {

        @Test
        @DisplayName("Builder 构建正确")
        void shouldBuildCorrectly() {
            var details = JwtUserDetails.builder()
                    .userId(1L).username("admin").deptId(10L)
                    .roles(List.of("admin", "user"))
                    .permissions(List.of("sys:user:list"))
                    .tenantId("tenant-1")
                    .deviceId("device-abc")
                    .build();

            assertThat(details.getUserId()).isEqualTo(1L);
            assertThat(details.getId()).isEqualTo(1L); // 兼容方法
            assertThat(details.getUsername()).isEqualTo("admin");
            assertThat(details.getDeptId()).isEqualTo(10L);
            assertThat(details.getTenantId()).isEqualTo("tenant-1");
            assertThat(details.getDeviceId()).isEqualTo("device-abc");
        }

        @Test
        @DisplayName("getRoles 返回不可变列表")
        void shouldReturnUnmodifiableRoles() {
            var details = JwtUserDetails.builder()
                    .roles(List.of("admin"))
                    .build();
            assertThat(details.getRoles()).containsExactly("admin");
            assertThatThrownBy(() -> details.getRoles().add("hacker"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("getPermissions 返回不可变列表")
        void shouldReturnUnmodifiablePermissions() {
            var details = JwtUserDetails.builder()
                    .permissions(List.of("sys:user:list"))
                    .build();
            assertThat(details.getPermissions()).containsExactly("sys:user:list");
            assertThatThrownBy(() -> details.getPermissions().add("hacker"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("roles/permissions 为 null 时返回空列表")
        void shouldReturnEmptyListForNull() {
            var details = JwtUserDetails.builder().build();
            assertThat(details.getRoles()).isEmpty();
            assertThat(details.getPermissions()).isEmpty();
        }
    }
}
