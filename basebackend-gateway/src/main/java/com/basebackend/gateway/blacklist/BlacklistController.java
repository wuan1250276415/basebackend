package com.basebackend.gateway.blacklist;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Set;

/**
 * 黑白名单管理 API
 * <p>
 * 提供运行时动态管理 IP / 路径封禁的 REST 接口。
 */
@Slf4j
@RestController
@RequestMapping("/actuator/gateway/blacklist")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "gateway.blacklist", name = "enabled", havingValue = "true")
public class BlacklistController {

    private final BlacklistManager blacklistManager;

    /** 查看当前封禁状态 */
    @GetMapping
    public Mono<ResponseEntity<Map<String, Object>>> getStatus() {
        return Mono.just(ResponseEntity.ok(Map.of(
                "enabled", blacklistManager.isEnabled(),
                "staticDeniedIps", blacklistManager.getDeniedIps(),
                "staticAllowedIps", blacklistManager.getAllowedIps(),
                "staticDeniedPaths", blacklistManager.getDeniedPaths(),
                "dynamicDeniedIps", blacklistManager.getDynamicDeniedIps(),
                "dynamicDeniedPaths", blacklistManager.getDynamicDeniedPaths()
        )));
    }

    /** 封禁 IP */
    @PostMapping("/ip/deny")
    public Mono<ResponseEntity<String>> denyIp(@RequestParam String ip,
                                                @RequestParam(defaultValue = "手动封禁") String reason) {
        blacklistManager.denyIp(ip, reason);
        return Mono.just(ResponseEntity.ok("已封禁 IP: " + ip));
    }

    /** 解封 IP */
    @DeleteMapping("/ip/deny")
    public Mono<ResponseEntity<String>> allowIp(@RequestParam String ip) {
        blacklistManager.allowIp(ip);
        return Mono.just(ResponseEntity.ok("已解封 IP: " + ip));
    }

    /** 封禁路径 */
    @PostMapping("/path/deny")
    public Mono<ResponseEntity<String>> denyPath(@RequestParam String path) {
        blacklistManager.denyPath(path);
        return Mono.just(ResponseEntity.ok("已封禁路径: " + path));
    }

    /** 解封路径 */
    @DeleteMapping("/path/deny")
    public Mono<ResponseEntity<String>> allowPath(@RequestParam String path) {
        blacklistManager.allowPath(path);
        return Mono.just(ResponseEntity.ok("已解封路径: " + path));
    }
}
