package com.basebackend.jwt;

import com.basebackend.common.security.SecretManager;
import com.basebackend.common.security.SecretManagerProperties;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        MockEnvironment env = new MockEnvironment();
        env.setProperty("jwt.secret", TEST_SECRET);
        SecretManagerProperties smProps = new SecretManagerProperties();
        SecretManager secretManager = new SecretManager(env, smProps);

        blacklist = new JwtTokenBlacklist(null); // 内存模式
        jwtUtil = new JwtUtil(jwtProperties, secretManager, blacklist);
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
            // secret defaults to null

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

            // access token 验证为 access 应成功
            JwtValidationResult result1 = jwtUtil.validateTokenSafe(accessToken, JwtUtil.TOKEN_TYPE_ACCESS);
            assertThat(result1.isValid()).isTrue();

            // access token 验证为 refresh 应失败
            JwtValidationResult result2 = jwtUtil.validateTokenSafe(accessToken, JwtUtil.TOKEN_TYPE_REFRESH);
            assertThat(result2.isValid()).isFalse();
            assertThat(result2.getErrorType()).isEqualTo(JwtException.ErrorType.TOKEN_TYPE_MISMATCH);

            // refresh token 验证为 refresh 应成功
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
            // 生成正常 Token
            String token = jwtUtil.generateToken("user1");

            // 用不同 issuer 的 JwtUtil 验证
            JwtProperties otherProps = new JwtProperties();
            otherProps.setSecret(TEST_SECRET);
            otherProps.setIssuer("wrong-issuer");
            otherProps.setAudience(TEST_AUDIENCE);

            MockEnvironment env = new MockEnvironment();
            env.setProperty("jwt.secret", TEST_SECRET);
            SecretManagerProperties smProps = new SecretManagerProperties();
            SecretManager sm = new SecretManager(env, smProps);
            JwtTokenBlacklist bl = new JwtTokenBlacklist(null);
            JwtUtil otherJwtUtil = new JwtUtil(otherProps, sm, bl);

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
            // 用不同密钥生成的 Token
            JwtProperties otherProps = new JwtProperties();
            String otherSecret = "another-secret-key-for-jwt-must-be-at-least-256-bits-long-too!!";
            otherProps.setSecret(otherSecret);
            otherProps.setIssuer(TEST_ISSUER);
            otherProps.setAudience(TEST_AUDIENCE);

            MockEnvironment env = new MockEnvironment();
            env.setProperty("jwt.secret", otherSecret);
            SecretManagerProperties smProps = new SecretManagerProperties();
            SecretManager sm = new SecretManager(env, smProps);
            JwtTokenBlacklist bl = new JwtTokenBlacklist(null);
            JwtUtil otherJwtUtil = new JwtUtil(otherProps, sm, bl);

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
            // 设置过期时间为 30 分钟，阈值为 1 小时
            jwtProperties.setExpiration(1800000L);
            jwtProperties.setExpiringSoonThreshold(3600000L);
            String token = jwtUtil.generateToken("user1");

            assertThat(jwtUtil.isTokenExpiringSoon(token)).isTrue();
        }

        @Test
        @DisplayName("不会很快过期的 Token 应返回 false")
        void shouldNotDetectExpiringSoonForLongLivedToken() {
            // 设置过期时间为 24 小时，阈值为 1 小时
            jwtProperties.setExpiration(86400000L);
            jwtProperties.setExpiringSoonThreshold(3600000L);
            String token = jwtUtil.generateToken("user1");

            assertThat(jwtUtil.isTokenExpiringSoon(token)).isFalse();
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
