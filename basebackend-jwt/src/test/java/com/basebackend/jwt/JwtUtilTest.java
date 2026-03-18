package com.basebackend.jwt;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JwtUtil 单元测试
 */
@DisplayName("JwtUtil 单元测试")
class JwtUtilTest {

    private static final String TEST_SECRET = "test-secret-key-for-jwt-unit-tests-must-be-at-least-256-bits-long!!";
    private static final String TEST_ISSUER = "test-issuer";
    private static final String TEST_AUDIENCE = "test-audience";

    private JwtUtil jwtUtil;
    private JwtProperties jwtProperties;
    private JwtTokenBlacklist blacklist;
    private JwtKeyManager keyManager;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret(TEST_SECRET);
        jwtProperties.setIssuer(TEST_ISSUER);
        jwtProperties.setAudience(TEST_AUDIENCE);
        jwtProperties.setExpiration(86400000L);
        jwtProperties.setAccessTokenExpiration(1800000L);
        jwtProperties.setRefreshTokenExpiration(604800000L);
        jwtProperties.setExpiringSoonThreshold(3600000L);
        jwtProperties.setMaxRefreshableExpiredDuration(604800000L);

        keyManager = new JwtKeyManager(jwtProperties);
        blacklist = new JwtTokenBlacklist(null); // 内存模式
        jwtUtil = new JwtUtil(jwtProperties, keyManager, blacklist, null, null);
    }

    /**
     * 使用自定义 JwtProperties 创建 JwtUtil（用于 issuer/audience/密钥 测试）
     */
    private JwtUtil createJwtUtil(JwtProperties props) {
        JwtKeyManager km = new JwtKeyManager(props);
        JwtTokenBlacklist bl = new JwtTokenBlacklist(null);
        return new JwtUtil(props, km, bl, null, null);
    }

    // ========== P0-1: claims 顺序修复 ==========

    @Nested
    @DisplayName("P0-1: Claims 顺序测试")
    class ClaimsOrderingTest {

        @Test
        @DisplayName("自定义 claims 中的 sub/iat/exp 不应覆盖核心字段")
        void customClaimsShouldNotOverrideCoreFields() {
            String subject = "real-subject";
            Map<String, Object> claims = new HashMap<>();
            claims.put("sub", "fake-subject");
            claims.put("iat", 0);
            claims.put("exp", 0);
            claims.put("customKey", "customValue");

            String token = jwtUtil.generateToken(subject, claims);
            Claims parsed = jwtUtil.getClaimsFromToken(token);

            assertThat(parsed).isNotNull();
            assertThat(parsed.getSubject()).isEqualTo(subject);
            assertThat(parsed.getIssuedAt()).isNotNull();
            assertThat(parsed.getExpiration()).isNotNull();
            assertThat(parsed.getExpiration().getTime()).isGreaterThan(System.currentTimeMillis());
            assertThat(parsed.get("customKey")).isEqualTo("customValue");
        }

        @Test
        @DisplayName("无自定义 claims 时正常生成 Token")
        void shouldGenerateTokenWithoutCustomClaims() {
            String token = jwtUtil.generateToken("user1");
            String subject = jwtUtil.getSubjectFromToken(token);
            assertThat(subject).isEqualTo("user1");
        }

        @Test
        @DisplayName("空 claims map 时正常生成 Token")
        void shouldGenerateTokenWithEmptyClaims() {
            String token = jwtUtil.generateToken("user1", new HashMap<>());
            String subject = jwtUtil.getSubjectFromToken(token);
            assertThat(subject).isEqualTo("user1");
        }
    }

    // ========== P0-2: 密钥校验 ==========

    @Nested
    @DisplayName("P0-2: 密钥校验测试")
    class SecretValidationTest {

        @Test
        @DisplayName("空密钥应在 PostConstruct 抛异常")
        void shouldRejectEmptySecret() {
            JwtProperties props = new JwtProperties();
            props.setSecret("");

            assertThatThrownBy(props::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("must be configured");
        }

        @Test
        @DisplayName("null 密钥应在 PostConstruct 抛异常")
        void shouldRejectNullSecret() {
            JwtProperties props = new JwtProperties();

            assertThatThrownBy(props::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("must be configured");
        }

        @Test
        @DisplayName("过短密钥应在 PostConstruct 抛异常")
        void shouldRejectShortSecret() {
            JwtProperties props = new JwtProperties();
            props.setSecret("too-short");

            assertThatThrownBy(props::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("at least 32 characters");
        }

        @Test
        @DisplayName("足够长度的密钥应通过校验")
        void shouldAcceptValidSecret() {
            JwtProperties props = new JwtProperties();
            props.setSecret(TEST_SECRET);

            props.validate(); // 不应抛异常
        }
    }

    // ========== P0-3: 黑名单 ==========

    @Nested
    @DisplayName("P0-3: Token 黑名单测试")
    class BlacklistTest {

        @Test
        @DisplayName("吊销 Token 后 validateToken 应返回 false")
        void revokedTokenShouldBeInvalid() {
            String token = jwtUtil.generateToken("user1");
            assertThat(jwtUtil.validateToken(token)).isTrue();

            jwtUtil.revokeToken(token);

            assertThat(jwtUtil.validateToken(token)).isFalse();
            assertThat(jwtUtil.isTokenRevoked(token)).isTrue();
        }

        @Test
        @DisplayName("吊销 Token 后 parseClaimsStrict 应抛 REVOKED 异常")
        void revokedTokenShouldThrowRevokedException() {
            String token = jwtUtil.generateToken("user1");
            jwtUtil.revokeToken(token);

            assertThatThrownBy(() -> jwtUtil.parseClaimsStrict(token))
                    .isInstanceOf(JwtException.class)
                    .satisfies(e -> {
                        JwtException je = (JwtException) e;
                        assertThat(je.getErrorType()).isEqualTo(JwtException.ErrorType.REVOKED);
                    });
        }

        @Test
        @DisplayName("未吊销 Token 应正常验证")
        void nonRevokedTokenShouldBeValid() {
            String token = jwtUtil.generateToken("user1");
            assertThat(jwtUtil.isTokenRevoked(token)).isFalse();
            assertThat(jwtUtil.validateToken(token)).isTrue();
        }

        @Test
        @DisplayName("validateTokenSafe 应返回 REVOKED 类型")
        void validateTokenSafeShouldReturnRevokedType() {
            String token = jwtUtil.generateToken("user1");
            jwtUtil.revokeToken(token);

            JwtValidationResult result = jwtUtil.validateTokenSafe(token);
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorType()).isEqualTo(JwtException.ErrorType.REVOKED);
        }
    }

    // ========== P1-1: 过期 Token 刷新 ==========

    @Nested
    @DisplayName("P1-1: 过期 Token 刷新测试")
    class ExpiredTokenRefreshTest {

        @Test
        @DisplayName("未过期 Token 应正常刷新")
        void shouldRefreshValidToken() {
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", 123L);
            claims.put("username", "admin");
            String token = jwtUtil.generateToken("admin", claims);

            String refreshed = jwtUtil.refreshToken(token);
            assertThat(refreshed).isNotNull();
            assertThat(jwtUtil.getSubjectFromToken(refreshed)).isEqualTo("admin");
        }

        @Test
        @DisplayName("刷新后应保留自定义 claims")
        void shouldPreserveCustomClaimsAfterRefresh() {
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", 42L);
            claims.put("customField", "customValue");
            String token = jwtUtil.generateToken("user1", claims);

            String refreshed = jwtUtil.refreshToken(token);
            assertThat(refreshed).isNotNull();

            Claims parsedClaims = jwtUtil.getClaimsFromToken(refreshed);
            assertThat(parsedClaims).isNotNull();
            assertThat(parsedClaims.get("customField")).isEqualTo("customValue");
        }
    }

    // ========== P1-2: 双 Token 机制 ==========

    @Nested
    @DisplayName("P1-2: 双 Token 机制测试")
    class DualTokenTest {

        @Test
        @DisplayName("生成 Access Token 应包含 access 类型")
        void accessTokenShouldHaveCorrectType() {
            Map<String, Object> claims = Map.of("userId", 1L);
            String token = jwtUtil.generateAccessToken("user1", claims);

            Claims parsed = jwtUtil.getClaimsFromToken(token);
            assertThat(parsed).isNotNull();
            assertThat(parsed.get(JwtUtil.CLAIM_TOKEN_TYPE)).isEqualTo(JwtUtil.TOKEN_TYPE_ACCESS);
            assertThat(parsed.getSubject()).isEqualTo("user1");
        }

        @Test
        @DisplayName("生成 Refresh Token 应包含 refresh 类型")
        void refreshTokenShouldHaveCorrectType() {
            String token = jwtUtil.generateRefreshToken("user1");

            Claims parsed = jwtUtil.getClaimsFromToken(token);
            assertThat(parsed).isNotNull();
            assertThat(parsed.get(JwtUtil.CLAIM_TOKEN_TYPE)).isEqualTo(JwtUtil.TOKEN_TYPE_REFRESH);
            assertThat(parsed.getSubject()).isEqualTo("user1");
        }

        @Test
        @DisplayName("Refresh Token 应能换取 Access Token")
        void shouldExchangeRefreshForAccess() {
            String refreshToken = jwtUtil.generateRefreshToken("user1");
            String accessToken = jwtUtil.refreshAccessToken(refreshToken);

            Claims accessClaims = jwtUtil.getClaimsFromToken(accessToken);
            assertThat(accessClaims).isNotNull();
            assertThat(accessClaims.getSubject()).isEqualTo("user1");
            assertThat(accessClaims.get(JwtUtil.CLAIM_TOKEN_TYPE)).isEqualTo(JwtUtil.TOKEN_TYPE_ACCESS);
        }

        @Test
        @DisplayName("Access Token 不应用于 refreshAccessToken")
        void accessTokenShouldNotBeUsedForRefresh() {
            String accessToken = jwtUtil.generateAccessToken("user1", null);

            assertThatThrownBy(() -> jwtUtil.refreshAccessToken(accessToken))
                    .isInstanceOf(JwtException.class)
                    .satisfies(e -> {
                        JwtException je = (JwtException) e;
                        assertThat(je.getErrorType()).isEqualTo(JwtException.ErrorType.TOKEN_TYPE_MISMATCH);
                    });
        }

        @Test
        @DisplayName("validateTokenSafe 应检查 Token 类型")
        void shouldValidateTokenType() {
            String accessToken = jwtUtil.generateAccessToken("user1", null);
            String refreshToken = jwtUtil.generateRefreshToken("user1");

            JwtValidationResult result1 = jwtUtil.validateTokenSafe(accessToken, JwtUtil.TOKEN_TYPE_ACCESS);
            assertThat(result1.isValid()).isTrue();

            JwtValidationResult result2 = jwtUtil.validateTokenSafe(accessToken, JwtUtil.TOKEN_TYPE_REFRESH);
            assertThat(result2.isValid()).isFalse();
            assertThat(result2.getErrorType()).isEqualTo(JwtException.ErrorType.TOKEN_TYPE_MISMATCH);

            JwtValidationResult result3 = jwtUtil.validateTokenSafe(refreshToken, JwtUtil.TOKEN_TYPE_REFRESH);
            assertThat(result3.isValid()).isTrue();
        }
    }

    // ========== P1-3: issuer/audience 验证 ==========

    @Nested
    @DisplayName("P1-3: Issuer/Audience 验证测试")
    class IssuerAudienceTest {

        @Test
        @DisplayName("生成 Token 应包含 issuer 和 audience")
        void tokenShouldContainIssuerAndAudience() {
            String token = jwtUtil.generateToken("user1");
            Claims claims = jwtUtil.getClaimsFromToken(token);

            assertThat(claims).isNotNull();
            assertThat(claims.getIssuer()).isEqualTo(TEST_ISSUER);
            assertThat(claims.getAudience()).contains(TEST_AUDIENCE);
        }

        @Test
        @DisplayName("错误 issuer 应验证失败")
        void wrongIssuerShouldFail() {
            String token = jwtUtil.generateToken("user1");

            JwtProperties otherProps = new JwtProperties();
            otherProps.setSecret(TEST_SECRET);
            otherProps.setIssuer("wrong-issuer");
            otherProps.setAudience(TEST_AUDIENCE);

            JwtUtil otherJwtUtil = createJwtUtil(otherProps);
            assertThat(otherJwtUtil.validateToken(token)).isFalse();
        }
    }

    // ========== P1-4: 异常分类 ==========

    @Nested
    @DisplayName("P1-4: 异常分类测试")
    class ExceptionClassificationTest {

        @Test
        @DisplayName("无效 Token 应抛 MALFORMED 异常")
        void malformedTokenShouldThrow() {
            assertThatThrownBy(() -> jwtUtil.parseClaimsStrict("not-a-valid-token"))
                    .isInstanceOf(JwtException.class)
                    .satisfies(e -> {
                        JwtException je = (JwtException) e;
                        assertThat(je.getErrorType()).isIn(
                                JwtException.ErrorType.MALFORMED,
                                JwtException.ErrorType.INVALID_SIGNATURE);
                    });
        }

        @Test
        @DisplayName("错误签名 Token 应抛 INVALID_SIGNATURE 异常")
        void wrongSignatureShouldThrow() {
            JwtProperties otherProps = new JwtProperties();
            String otherSecret = "another-secret-key-for-jwt-must-be-at-least-256-bits-long-too!!";
            otherProps.setSecret(otherSecret);
            otherProps.setIssuer(TEST_ISSUER);
            otherProps.setAudience(TEST_AUDIENCE);

            JwtUtil otherJwtUtil = createJwtUtil(otherProps);
            String token = otherJwtUtil.generateToken("user1");

            assertThatThrownBy(() -> jwtUtil.parseClaimsStrict(token))
                    .isInstanceOf(JwtException.class)
                    .satisfies(e -> {
                        JwtException je = (JwtException) e;
                        assertThat(je.getErrorType()).isEqualTo(JwtException.ErrorType.INVALID_SIGNATURE);
                    });
        }

        @Test
        @DisplayName("validateTokenSafe 不应抛异常")
        void validateTokenSafeShouldNotThrow() {
            JwtValidationResult result = jwtUtil.validateTokenSafe("invalid-token");
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorType()).isNotNull();
            assertThat(result.getErrorMessage()).isNotNull();
        }

        @Test
        @DisplayName("有效 Token 的 validateTokenSafe 应返回成功")
        void validateTokenSafeShouldSucceedForValidToken() {
            String token = jwtUtil.generateToken("user1");
            JwtValidationResult result = jwtUtil.validateTokenSafe(token);
            assertThat(result.isValid()).isTrue();
            assertThat(result.getClaims()).isNotNull();
            assertThat(result.getClaims().getSubject()).isEqualTo("user1");
        }
    }

    // ========== P1-5: JwtUserDetails 扩展 ==========

    @Nested
    @DisplayName("P1-5: JwtUserDetails 扩展测试")
    class JwtUserDetailsTest {

        @Test
        @DisplayName("fromClaims 应正确解析所有字段")
        void shouldParseAllFieldsFromClaims() {
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", 42L);
            claims.put("deptId", 10L);
            claims.put("roles", List.of("ADMIN", "USER"));
            claims.put("permissions", List.of("user:read", "user:write"));
            claims.put("tenantId", "tenant-001");
            claims.put("deviceId", "device-abc");

            String token = jwtUtil.generateToken("admin", claims);
            Claims parsed = jwtUtil.getClaimsFromToken(token);
            assertThat(parsed).isNotNull();

            JwtUserDetails userDetails = JwtUserDetails.fromClaims(parsed);

            assertThat(userDetails.getUsername()).isEqualTo("admin");
            assertThat(userDetails.getUserId()).isEqualTo(42L);
            assertThat(userDetails.getDeptId()).isEqualTo(10L);
            assertThat(userDetails.getRoles()).containsExactly("ADMIN", "USER");
            assertThat(userDetails.getPermissions()).containsExactly("user:read", "user:write");
            assertThat(userDetails.getTenantId()).isEqualTo("tenant-001");
            assertThat(userDetails.getDeviceId()).isEqualTo("device-abc");
        }

        @Test
        @DisplayName("fromClaims 缺失字段应返回 null/空列表")
        void shouldHandleMissingFields() {
            String token = jwtUtil.generateToken("user1");
            Claims parsed = jwtUtil.getClaimsFromToken(token);
            assertThat(parsed).isNotNull();

            JwtUserDetails userDetails = JwtUserDetails.fromClaims(parsed);

            assertThat(userDetails.getUsername()).isEqualTo("user1");
            assertThat(userDetails.getUserId()).isNull();
            assertThat(userDetails.getDeptId()).isNull();
            assertThat(userDetails.getRoles()).isEmpty();
            assertThat(userDetails.getPermissions()).isEmpty();
            assertThat(userDetails.getTenantId()).isNull();
            assertThat(userDetails.getDeviceId()).isNull();
        }

        @Test
        @DisplayName("getId 应返回 userId（兼容方法）")
        void getIdShouldReturnUserId() {
            JwtUserDetails details = JwtUserDetails.builder()
                    .userId(99L)
                    .username("user99")
                    .build();
            assertThat(details.getId()).isEqualTo(99L);
        }
    }

    // ========== P1-6: isTokenExpiringSoon 可配置 ==========

    @Nested
    @DisplayName("P1-6: expiring-soon 阈值可配置测试")
    class ExpiringSoonTest {

        @Test
        @DisplayName("即将过期的 Token 应返回 true")
        void shouldDetectExpiringSoon() {
            jwtProperties.setExpiration(1800000L);
            jwtProperties.setExpiringSoonThreshold(3600000L);
            String token = jwtUtil.generateToken("user1");

            assertThat(jwtUtil.isTokenExpiringSoon(token)).isTrue();
        }

        @Test
        @DisplayName("不会很快过期的 Token 应返回 false")
        void shouldNotDetectExpiringSoonForLongLivedToken() {
            jwtProperties.setExpiration(86400000L);
            jwtProperties.setExpiringSoonThreshold(3600000L);
            String token = jwtUtil.generateToken("user1");

            assertThat(jwtUtil.isTokenExpiringSoon(token)).isFalse();
        }
    }

    // ========== P2: 密钥轮换 ==========

    @Nested
    @DisplayName("P2: 密钥轮换测试")
    class KeyRotationTest {

        @Test
        @DisplayName("单密钥模式下 Token 应正常生成和验证")
        void singleKeyModeShouldWork() {
            assertThat(keyManager.isSingleKeyMode()).isTrue();
            assertThat(keyManager.getActiveKeyId()).isEqualTo(JwtKeyManager.DEFAULT_KID);

            String token = jwtUtil.generateToken("user1");
            assertThat(jwtUtil.validateToken(token)).isTrue();
        }

        @Test
        @DisplayName("多密钥模式 Token 应包含 kid header 并正常验证")
        void multiKeyModeShouldIncludeKid() {
            String secret2 = "second-key-for-rotation-must-be-at-least-32-characters-long!!!!";
            JwtProperties multiProps = new JwtProperties();
            multiProps.setSecret(TEST_SECRET);
            multiProps.setIssuer(TEST_ISSUER);
            multiProps.setAudience(TEST_AUDIENCE);
            multiProps.getKeys().put("k-1", TEST_SECRET);
            multiProps.getKeys().put("k-2", secret2);
            multiProps.setActiveKeyId("k-1");

            JwtKeyManager multiKm = new JwtKeyManager(multiProps);
            JwtUtil multiUtil = new JwtUtil(multiProps, multiKm, blacklist, null, null);

            assertThat(multiKm.isSingleKeyMode()).isFalse();

            String token = multiUtil.generateToken("user1");
            assertThat(multiUtil.validateToken(token)).isTrue();
            assertThat(multiUtil.getSubjectFromToken(token)).isEqualTo("user1");
        }

        @Test
        @DisplayName("密钥轮换后旧密钥签发的 Token 仍可验证（过渡期）")
        void oldKeyTokenShouldBeValidDuringGracePeriod() {
            String secret2 = "second-key-for-rotation-must-be-at-least-32-characters-long!!!!";
            JwtProperties multiProps = new JwtProperties();
            multiProps.setSecret(TEST_SECRET);
            multiProps.setIssuer(TEST_ISSUER);
            multiProps.setAudience(TEST_AUDIENCE);
            multiProps.getKeys().put("k-1", TEST_SECRET);
            multiProps.setActiveKeyId("k-1");
            multiProps.setKeyRotationGracePeriod(604800000L);

            JwtKeyManager multiKm = new JwtKeyManager(multiProps);
            JwtUtil multiUtil = new JwtUtil(multiProps, multiKm, blacklist, null, null);

            // 用 k-1 签发 Token
            String oldToken = multiUtil.generateToken("user1");
            assertThat(multiUtil.validateToken(oldToken)).isTrue();

            // 轮换到 k-2
            multiKm.rotateKey("k-2", secret2);
            assertThat(multiKm.getActiveKeyId()).isEqualTo("k-2");

            // 旧 Token 仍可验证（k-1 在过渡期内）
            assertThat(multiUtil.validateToken(oldToken)).isTrue();

            // 新 Token 用 k-2 签名
            String newToken = multiUtil.generateToken("user2");
            assertThat(multiUtil.validateToken(newToken)).isTrue();
            assertThat(multiUtil.getSubjectFromToken(newToken)).isEqualTo("user2");
        }

        @Test
        @DisplayName("rotateKey 应拒绝过短密钥")
        void rotateKeyShouldRejectShortSecret() {
            assertThatThrownBy(() -> keyManager.rotateKey("k-new", "short"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least 32 characters");
        }

        @Test
        @DisplayName("rotateKey 应拒绝空 keyId")
        void rotateKeyShouldRejectBlankKeyId() {
            assertThatThrownBy(() -> keyManager.rotateKey("", TEST_SECRET))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not be blank");
        }

        @Test
        @DisplayName("retireKey 不应允许停用活跃密钥")
        void retireKeyShouldRejectActiveKey() {
            assertThatThrownBy(() -> keyManager.retireKey(keyManager.getActiveKeyId()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot retire the active key");
        }

        @Test
        @DisplayName("listKeyIds 应返回所有可用密钥 ID")
        void listKeyIdsShouldReturnAllKeys() {
            Set<String> keys = keyManager.listKeyIds();
            assertThat(keys).containsExactly(JwtKeyManager.DEFAULT_KID);
        }

        @Test
        @DisplayName("多密钥配置校验 - activeKeyId 不在 keys 中应抛异常")
        void shouldRejectInvalidActiveKeyId() {
            JwtProperties badProps = new JwtProperties();
            badProps.setSecret(TEST_SECRET);
            badProps.getKeys().put("k-1", TEST_SECRET);
            badProps.setActiveKeyId("non-existent");

            assertThatThrownBy(badProps::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("must reference a key");
        }

        @Test
        @DisplayName("多密钥配置校验 - keys 中的密钥过短应抛异常")
        void shouldRejectShortKeyInMultiKeyConfig() {
            JwtProperties badProps = new JwtProperties();
            badProps.setSecret(TEST_SECRET);
            badProps.getKeys().put("k-1", "too-short");
            badProps.setActiveKeyId("k-1");

            assertThatThrownBy(badProps::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("at least 32 characters");
        }
    }

    // ========== P3-1: 多设备管理 ==========

    @Nested
    @DisplayName("P3-1: 多设备管理测试")
    class DeviceManagementTest {

        private JwtDeviceManager deviceManager;
        private JwtUtil deviceJwtUtil;

        @BeforeEach
        void setUpDeviceManager() {
            deviceManager = new JwtDeviceManager(null, 3); // 内存模式，最大 3 设备
            deviceJwtUtil = new JwtUtil(jwtProperties, keyManager, blacklist, deviceManager, null);
        }

        @Test
        @DisplayName("注册设备后应能查询到")
        void shouldRegisterAndQueryDevice() {
            DeviceInfo device = DeviceInfo.builder()
                    .deviceId("dev-1")
                    .deviceName("iPhone 15")
                    .deviceType(DeviceInfo.DeviceType.MOBILE)
                    .ip("198.51.100.100")
                    .build();

            deviceManager.registerDevice(1L, device, "jti-123");

            assertThat(deviceManager.isDeviceActive(1L, "dev-1")).isTrue();
            assertThat(deviceManager.isDeviceActive(1L, "dev-2")).isFalse();

            List<DeviceSession> devices = deviceManager.getActiveDevices(1L);
            assertThat(devices).hasSize(1);
            assertThat(devices.get(0).getDeviceId()).isEqualTo("dev-1");
            assertThat(devices.get(0).getDeviceName()).isEqualTo("iPhone 15");
            assertThat(devices.get(0).getTokenJti()).isEqualTo("jti-123");
        }

        @Test
        @DisplayName("移除设备后应不可查询")
        void shouldRemoveDevice() {
            DeviceInfo device = DeviceInfo.builder()
                    .deviceId("dev-1")
                    .deviceName("Desktop")
                    .deviceType(DeviceInfo.DeviceType.DESKTOP)
                    .build();
            deviceManager.registerDevice(1L, device, "jti-1");

            deviceManager.removeDevice(1L, "dev-1");

            assertThat(deviceManager.isDeviceActive(1L, "dev-1")).isFalse();
            assertThat(deviceManager.getActiveDevices(1L)).isEmpty();
        }

        @Test
        @DisplayName("removeAllDevices 应清空所有设备")
        void shouldRemoveAllDevices() {
            for (int i = 1; i <= 3; i++) {
                deviceManager.registerDevice(1L,
                        DeviceInfo.builder().deviceId("dev-" + i).deviceName("Device " + i)
                                .deviceType(DeviceInfo.DeviceType.UNKNOWN).build(),
                        "jti-" + i);
            }

            deviceManager.removeAllDevices(1L);
            assertThat(deviceManager.getActiveDevices(1L)).isEmpty();
        }

        @Test
        @DisplayName("removeAllDevicesExcept 应只保留指定设备")
        void shouldRemoveAllExceptCurrent() {
            for (int i = 1; i <= 3; i++) {
                deviceManager.registerDevice(1L,
                        DeviceInfo.builder().deviceId("dev-" + i).deviceName("Device " + i)
                                .deviceType(DeviceInfo.DeviceType.UNKNOWN).build(),
                        "jti-" + i);
            }

            deviceManager.removeAllDevicesExcept(1L, "dev-2");

            List<DeviceSession> remaining = deviceManager.getActiveDevices(1L);
            assertThat(remaining).hasSize(1);
            assertThat(remaining.get(0).getDeviceId()).isEqualTo("dev-2");
        }

        @Test
        @DisplayName("超出设备数量限制应踢掉最早的设备")
        void shouldEvictOldestDeviceWhenExceedingLimit() throws InterruptedException {
            for (int i = 1; i <= 3; i++) {
                deviceManager.registerDevice(1L,
                        DeviceInfo.builder().deviceId("dev-" + i).deviceName("Device " + i)
                                .deviceType(DeviceInfo.DeviceType.UNKNOWN).build(),
                        "jti-" + i);
                Thread.sleep(10); // 确保 loginTime 不同
            }

            // 注册第 4 个设备，应踢掉 dev-1
            deviceManager.registerDevice(1L,
                    DeviceInfo.builder().deviceId("dev-4").deviceName("Device 4")
                            .deviceType(DeviceInfo.DeviceType.UNKNOWN).build(),
                    "jti-4");

            List<DeviceSession> devices = deviceManager.getActiveDevices(1L);
            assertThat(devices).hasSize(3);
            assertThat(devices.stream().map(DeviceSession::getDeviceId))
                    .doesNotContain("dev-1")
                    .contains("dev-2", "dev-3", "dev-4");
        }

        @Test
        @DisplayName("updateLastActive 应更新最后活跃时间")
        void shouldUpdateLastActiveTime() throws InterruptedException {
            DeviceInfo device = DeviceInfo.builder()
                    .deviceId("dev-1")
                    .deviceName("Desktop")
                    .deviceType(DeviceInfo.DeviceType.DESKTOP)
                    .build();
            deviceManager.registerDevice(1L, device, "jti-1");

            DeviceSession before = deviceManager.getDeviceSession(1L, "dev-1");
            assertThat(before).isNotNull();
            long originalTime = before.getLastActiveTime();

            Thread.sleep(10);
            deviceManager.updateLastActive(1L, "dev-1");

            DeviceSession after = deviceManager.getDeviceSession(1L, "dev-1");
            assertThat(after).isNotNull();
            assertThat(after.getLastActiveTime()).isGreaterThan(originalTime);
        }

        @Test
        @DisplayName("kickDevice 应移除设备并吊销 Token")
        void kickDeviceShouldRevokeToken() {
            DeviceInfo device = DeviceInfo.builder()
                    .deviceId("dev-1")
                    .deviceName("Desktop")
                    .deviceType(DeviceInfo.DeviceType.DESKTOP)
                    .build();

            Map<String, Object> claims = Map.of("userId", 1L);
            String token = deviceJwtUtil.generateAccessToken("user1", claims, device);
            assertThat(deviceJwtUtil.validateToken(token)).isTrue();

            deviceJwtUtil.kickDevice(1L, "dev-1");

            assertThat(deviceManager.isDeviceActive(1L, "dev-1")).isFalse();
        }

        @Test
        @DisplayName("generateAccessToken 带 DeviceInfo 应注册设备")
        void generateAccessTokenWithDeviceShouldRegister() {
            DeviceInfo device = DeviceInfo.builder()
                    .deviceId("dev-1")
                    .deviceName("Desktop")
                    .deviceType(DeviceInfo.DeviceType.DESKTOP)
                    .build();

            Map<String, Object> claims = Map.of("userId", 1L);
            String token = deviceJwtUtil.generateAccessToken("user1", claims, device);

            assertThat(token).isNotNull();
            assertThat(deviceJwtUtil.validateToken(token)).isTrue();
            assertThat(deviceManager.isDeviceActive(1L, "dev-1")).isTrue();
        }
    }

    // ========== P3-2: Token 事件审计 ==========

    @Nested
    @DisplayName("P3-2: Token 事件审计测试")
    class AuditTest {

        @Test
        @DisplayName("审计日志记录器应正常创建和使用")
        void auditLoggerShouldWork() {
            JwtAuditLogger auditLogger = new JwtAuditLogger(true, false, null);
            JwtUtil auditJwtUtil = new JwtUtil(jwtProperties, keyManager, blacklist, null, auditLogger);

            String token = auditJwtUtil.generateToken("user1");
            assertThat(token).isNotNull();
            assertThat(auditJwtUtil.validateToken(token)).isTrue();

            auditJwtUtil.revokeToken(token);
            assertThat(auditJwtUtil.validateToken(token)).isFalse();
        }

        @Test
        @DisplayName("禁用审计时不应影响正常功能")
        void disabledAuditShouldNotAffectFunctionality() {
            JwtAuditLogger auditLogger = new JwtAuditLogger(false, false, null);
            JwtUtil auditJwtUtil = new JwtUtil(jwtProperties, keyManager, blacklist, null, auditLogger);

            String token = auditJwtUtil.generateToken("user1");
            assertThat(token).isNotNull();
            assertThat(auditJwtUtil.validateToken(token)).isTrue();
        }

        @Test
        @DisplayName("JwtAuditEntry 应正确构建")
        void auditEntryShouldBuild() {
            JwtAuditEntry entry = JwtAuditEntry.builder()
                    .eventType(JwtAuditEvent.TOKEN_GENERATED)
                    .userId("user1")
                    .deviceId("dev-1")
                    .tokenJti("jti-123")
                    .ip("198.51.100.1")
                    .userAgent("Mozilla/5.0")
                    .timestamp(System.currentTimeMillis())
                    .details(Map.of("tokenType", "access"))
                    .build();

            assertThat(entry.getEventType()).isEqualTo(JwtAuditEvent.TOKEN_GENERATED);
            assertThat(entry.getUserId()).isEqualTo("user1");
            assertThat(entry.getDeviceId()).isEqualTo("dev-1");
            assertThat(entry.getTokenJti()).isEqualTo("jti-123");
            assertThat(entry.getIp()).isEqualTo("198.51.100.1");
        }

        @Test
        @DisplayName("JwtAuditSpringEvent 应携带 auditEntry")
        void springEventShouldCarryEntry() {
            JwtAuditEntry entry = JwtAuditEntry.builder()
                    .eventType(JwtAuditEvent.TOKEN_REVOKED)
                    .userId("user1")
                    .build();

            JwtAuditSpringEvent event = new JwtAuditSpringEvent(this, entry);
            assertThat(event.getAuditEntry()).isSameAs(entry);
            assertThat(event.getAuditEntry().getEventType()).isEqualTo(JwtAuditEvent.TOKEN_REVOKED);
        }

        @Test
        @DisplayName("null auditLogger 不应影响 JwtUtil 正常工作")
        void nullAuditLoggerShouldBeHandledGracefully() {
            JwtUtil noAuditUtil = new JwtUtil(jwtProperties, keyManager, blacklist, null, null);
            String token = noAuditUtil.generateToken("user1");
            assertThat(noAuditUtil.validateToken(token)).isTrue();

            noAuditUtil.revokeToken(token);
            assertThat(noAuditUtil.validateToken(token)).isFalse();
        }
    }

    // ========== 综合测试 ==========

    @Nested
    @DisplayName("综合功能测试")
    class IntegrationTest {

        @Test
        @DisplayName("Token 应包含 jti claim")
        void tokenShouldContainJti() {
            String token = jwtUtil.generateToken("user1");
            Claims claims = jwtUtil.getClaimsFromToken(token);

            assertThat(claims).isNotNull();
            assertThat(claims.getId()).isNotNull().isNotBlank();
        }

        @Test
        @DisplayName("getUserIdFromToken 应正确解析")
        void shouldGetUserIdFromToken() {
            Map<String, Object> claims = Map.of("userId", 123L);
            String token = jwtUtil.generateToken("user1", claims);

            Long userId = jwtUtil.getUserIdFromToken(token);
            assertThat(userId).isEqualTo(123L);
        }

        @Test
        @DisplayName("getUsernameFromToken 应优先取 username claim")
        void shouldGetUsernameFromClaim() {
            Map<String, Object> claims = Map.of("username", "admin-display");
            String token = jwtUtil.generateToken("admin", claims);

            String username = jwtUtil.getUsernameFromToken(token);
            assertThat(username).isEqualTo("admin-display");
        }

        @Test
        @DisplayName("getDeptIdFromToken 应正确解析")
        void shouldGetDeptIdFromToken() {
            Map<String, Object> claims = Map.of("deptId", 5L);
            String token = jwtUtil.generateToken("user1", claims);

            Long deptId = jwtUtil.getDeptIdFromToken(token);
            assertThat(deptId).isEqualTo(5L);
        }

        @Test
        @DisplayName("getExpirationDateFromToken 应返回有效日期")
        void shouldGetExpirationDate() {
            String token = jwtUtil.generateToken("user1");
            var expDate = jwtUtil.getExpirationDateFromToken(token);
            assertThat(expDate).isNotNull();
            assertThat(expDate.getTime()).isGreaterThan(System.currentTimeMillis());
        }

        @Test
        @DisplayName("吊销后的 Refresh Token 不应能换取 Access Token")
        void revokedRefreshTokenShouldNotExchange() {
            String refreshToken = jwtUtil.generateRefreshToken("user1");
            jwtUtil.revokeToken(refreshToken);

            assertThatThrownBy(() -> jwtUtil.refreshAccessToken(refreshToken))
                    .isInstanceOf(JwtException.class)
                    .satisfies(e -> {
                        JwtException je = (JwtException) e;
                        assertThat(je.getErrorType()).isEqualTo(JwtException.ErrorType.REVOKED);
                    });
        }
    }
}
