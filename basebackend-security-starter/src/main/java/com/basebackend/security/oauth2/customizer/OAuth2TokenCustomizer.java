package com.basebackend.security.oauth2.customizer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * OAuth2 Token自定义器
 *
 * 负责：
 * - 配置JWT解码器
 * - 自定义权限提取逻辑
 * - Token验证和转换
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-26
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "basebackend.security.oauth2.enabled", havingValue = "true")
@RequiredArgsConstructor
public class OAuth2TokenCustomizer {

    // Remove dependency on OAuth2ResourceServerProperties as it's not needed
    // We'll use Spring Boot's standard OAuth2 resource server configuration instead

    /**
     * 创建JWT解码器
     * 用于验证Token的签名和有效性
     *
     * @return JwtDecoder 解码器
     */
    @Bean
    @ConditionalOnMissingBean
    public JwtDecoder jwtDecoder() {
        try {
            // 使用环境变量或默认值
            String jwkSetUri = System.getenv("JWT_JWK_SET_URI");
            if (jwkSetUri == null || jwkSetUri.isEmpty()) {
                jwkSetUri = "https://auth.example.com/oauth2/jwks";
            }

            log.info("初始化JWT解码器 - JWK Set URL: {}", jwkSetUri);

            // 使用 NimbusJwtDecoder 从 JWK Set URI 获取密钥
            NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
                // 时钟偏差容忍
                .jwtProcessorCustomizer(customizer -> customizer.setJWTClaimsSetVerifier((claims, jwt) -> {
                    if (!validateTokenTime(claims.getClaims())) {
                        throw new JwtException("Token已过期或无效");
                    }
                    log.debug("JWT验证通过 - 用户: {}, 签发者: {}",
                        claims.getSubject(), claims.getIssuer());
                }))
                .build();

            log.debug("JWT解码器初始化成功");
            return jwtDecoder;

        } catch (Exception e) {
            log.error("JWT解码器初始化失败: {}", e.getMessage(), e);
            throw new RuntimeException("JWT解码器初始化失败", e);
        }
    }

    /**
     * 创建JWT认证转换器
     * 将JWT Token转换为Spring Security认证对象
     *
     * @return JwtAuthenticationConverter
     */
    @Bean
    @ConditionalOnMissingBean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        String authorityClaimName = "permissions";

        // 配置权限提取规则
        authoritiesConverter.setAuthorityPrefix(""); // 不添加前缀，我们使用自定义提取逻辑
        authoritiesConverter.setAuthoritiesClaimName(authorityClaimName); // 从permissions字段提取

        // 创建转换器，从JWT中提取权限
        Converter<Jwt, Collection<GrantedAuthority>> authoritiesExtractor = jwt -> {
            try {
                return extractAuthorities(jwt.getClaims());
            } catch (Exception e) {
                log.error("权限提取失败", e);
                return List.of();
            }
        };

        JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesExtractor);

        log.debug("JWT认证转换器配置完成 - 权限字段: {}, 自定义提取器: 启用",
            authorityClaimName);

        return authenticationConverter;
    }

    /**
     * 自定义权限提取逻辑
     * 从JWT claims中提取权限信息
     *
     * @param claims JWT claims
     * @return 权限列表
     */
    private List<GrantedAuthority> extractAuthorities(Map<String, Object> claims) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        try {
            // 1. 从permissions字段提取
            if (claims.containsKey("permissions")) {
                Object permissionsObj = claims.get("permissions");
                if (permissionsObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> permissions = (List<String>) permissionsObj;
                    authorities.addAll(permissions.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList()));
                    log.debug("从permissions字段提取到{}个权限", permissions.size());
                }
            }

            // 2. 从roles字段提取（兼容旧版JWT格式）
            if (claims.containsKey("roles")) {
                Object rolesObj = claims.get("roles");
                if (rolesObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> roles = (List<String>) rolesObj;
                    roles.stream()
                        .map(role -> "ROLE_" + role) // 添加ROLE_前缀
                        .map(SimpleGrantedAuthority::new)
                        .forEach(authorities::add);
                    log.debug("从roles字段提取到{}个角色", roles.size());
                }
            }

            // 3. 从scopes字段提取（OAuth2标准字段）
            if (claims.containsKey("scope") || claims.containsKey("scopes")) {
                String scopes = (String) claims.getOrDefault("scope",
                    claims.getOrDefault("scopes", ""));
                if (scopes != null && !scopes.isEmpty()) {
                    for (String scope : scopes.split(" ")) {
                        if (!scope.isEmpty()) {
                            authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope));
                        }
                    }
                    log.debug("从scopes字段提取到{}个权限范围", scopes.split(" ").length);
                }
            }

            // 4. 从authorities字段提取
            if (claims.containsKey("authorities")) {
                Object authoritiesObj = claims.get("authorities");
                if (authoritiesObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> authoritiesList = (List<String>) authoritiesObj;
                    authorities.addAll(authoritiesList.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList()));
                    log.debug("从authorities字段提取到{}个权限", authoritiesList.size());
                }
            }

        } catch (Exception e) {
            log.error("权限提取失败: {}", e.getMessage(), e);
        }

        if (authorities.isEmpty()) {
            log.warn("未从JWT claims中提取到任何权限");
        } else {
            log.debug("总共提取到{}个权限", authorities.size());
        }

        return authorities;
    }

    /**
     * 验证JWT Token的时效性
     * 检查Token是否过期
     *
     * @param claims JWT claims
     * @return true如果Token有效，false如果Token过期或无效
     */
    public boolean validateTokenTime(Map<String, Object> claims) {
        try {
            if (claims.containsKey("exp")) {
                Object expObj = claims.get("exp");
                long expTime = expObj instanceof Long ? (Long) expObj :
                    expObj instanceof Integer ? ((Integer) expObj).longValue() : 0;

                long currentTime = System.currentTimeMillis() / 1000;

                if (expTime > 0 && currentTime >= expTime) {
                    log.warn("JWT Token已过期 - 过期时间: {}, 当前时间: {}",
                        expTime, currentTime);
                    return false;
                }
            }

            if (claims.containsKey("iat")) {
                Object iatObj = claims.get("iat");
                long iatTime = iatObj instanceof Long ? (Long) iatObj :
                    iatObj instanceof Integer ? ((Integer) iatObj).longValue() : 0;

                // 检查签发时间是否在未来（防止时间同步问题）
                if (iatTime > System.currentTimeMillis() / 1000 + 300) {
                    log.warn("JWT Token签发时间在未来 - 签发时间: {}, 允许误差: 5分钟",
                        iatTime);
                    return false;
                }
            }

            return true;

        } catch (Exception e) {
            log.error("Token时效性验证失败", e);
            return false;
        }
    }

    /**
     * 提取用户标识从JWT claims
     *
     * @param claims JWT claims
     * @return 用户标识（通常是sub字段）
     */
    public String extractUserId(Map<String, Object> claims) {
        try {
            // 优先从sub字段获取
            String sub = (String) claims.getOrDefault("sub", "");
            if (!sub.isEmpty()) {
                return sub;
            }

            // 从user_id字段获取
            Object userIdObj = claims.get("user_id");
            if (userIdObj != null) {
                return userIdObj.toString();
            }

            // 从uid字段获取
            String uid = (String) claims.getOrDefault("uid", "");
            if (!uid.isEmpty()) {
                return uid;
            }

            log.warn("未找到有效的用户标识");
            return null;

        } catch (Exception e) {
            log.error("用户标识提取失败", e);
            return null;
        }
    }

    /**
     * 获取JWT Token的元数据信息
     *
     * @param claims JWT claims
     * @return Token元数据
     */
    public Map<String, Object> extractTokenMetadata(Map<String, Object> claims) {
        Map<String, Object> metadata = new java.util.HashMap<>();
        metadata.put("userId", extractUserId(claims));
        metadata.put("clientId", claims.getOrDefault("client_id", ""));
        metadata.put("scope", claims.getOrDefault("scope", ""));
        metadata.put("tokenType", claims.getOrDefault("token_type", "Bearer"));
        metadata.put("issuer", claims.getOrDefault("iss", ""));
        metadata.put("audience", claims.getOrDefault("aud", ""));
        return metadata;
    }
}
