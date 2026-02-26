package com.basebackend.gateway.cors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 动态跨域管理 API
 */
@Slf4j
@RestController
@RequestMapping("/actuator/gateway/cors")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "gateway.dynamic-cors", name = "enabled", havingValue = "true")
public class DynamicCorsController {

    private final DynamicCorsProperties corsProperties;

    /** 查看当前 CORS 配置 */
    @GetMapping
    public Mono<ResponseEntity<Map<String, Object>>> getConfig() {
        return Mono.just(ResponseEntity.ok(Map.of(
                "enabled", corsProperties.isEnabled(),
                "allowedOrigins", corsProperties.getAllowedOrigins(),
                "allowedMethods", corsProperties.getAllowedMethods(),
                "allowedHeaders", corsProperties.getAllowedHeaders(),
                "allowCredentials", corsProperties.isAllowCredentials(),
                "maxAge", corsProperties.getMaxAge()
        )));
    }

    /** 添加允许的源 */
    @PostMapping("/origins")
    public Mono<ResponseEntity<String>> addOrigin(@RequestParam String origin) {
        corsProperties.addAllowedOrigin(origin);
        return Mono.just(ResponseEntity.ok("已添加 CORS 允许源: " + origin));
    }

    /** 移除允许的源 */
    @DeleteMapping("/origins")
    public Mono<ResponseEntity<String>> removeOrigin(@RequestParam String origin) {
        corsProperties.removeAllowedOrigin(origin);
        return Mono.just(ResponseEntity.ok("已移除 CORS 允许源: " + origin));
    }
}
