package com.basebackend.gateway.blacklist;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("BlacklistManager 测试")
class BlacklistManagerTest {

    private BlacklistManager manager;

    @BeforeEach
    void setUp() {
        manager = new BlacklistManager();
        manager.setEnabled(true);
    }

    // ==================== IP 黑名单 ====================

    @Test
    @DisplayName("静态黑名单 IP 被拒绝")
    void staticDeniedIp() {
        manager.getDeniedIps().add("198.51.100.100");
        assertThat(manager.isIpDenied("198.51.100.100")).isTrue();
        assertThat(manager.isIpDenied("198.51.100.101")).isFalse();
    }

    @Test
    @DisplayName("动态封禁 IP")
    void dynamicDenyIp() {
        manager.denyIp("10.0.0.1", "恶意请求");
        assertThat(manager.isIpDenied("10.0.0.1")).isTrue();
        assertThat(manager.getBanReason("10.0.0.1")).isEqualTo("恶意请求");
    }

    @Test
    @DisplayName("动态解封 IP")
    void dynamicAllowIp() {
        manager.denyIp("10.0.0.1", "测试");
        manager.allowIp("10.0.0.1");
        assertThat(manager.isIpDenied("10.0.0.1")).isFalse();
        assertThat(manager.getBanReason("10.0.0.1")).isNull();
    }

    // ==================== IP 白名单模式 ====================

    @Test
    @DisplayName("白名单模式：仅允许列表中的 IP")
    void whitelistMode() {
        manager.getAllowedIps().add("198.51.100.1");
        assertThat(manager.isIpDenied("198.51.100.1")).isFalse();   // 在白名单中
        assertThat(manager.isIpDenied("198.51.100.2")).isTrue();    // 不在白名单中
    }

    @Test
    @DisplayName("白名单模式：动态添加白名单 IP")
    void whitelistModeDynamic() {
        manager.getAllowedIps().add("198.51.100.1");
        assertThat(manager.isIpDenied("10.0.0.1")).isTrue();

        // 动态加入白名单
        manager.getDynamicAllowedIps().add("10.0.0.1");
        assertThat(manager.isIpDenied("10.0.0.1")).isFalse();
    }

    // ==================== 路径黑名单 ====================

    @Test
    @DisplayName("静态路径黑名单 — 精确匹配")
    void staticDeniedPathExact() {
        manager.getDeniedPaths().add("/admin/debug");
        assertThat(manager.isPathDenied("/admin/debug")).isTrue();
        assertThat(manager.isPathDenied("/admin/other")).isFalse();
    }

    @Test
    @DisplayName("静态路径黑名单 — /** 通配符")
    void staticDeniedPathWildcard() {
        manager.getDeniedPaths().add("/admin/debug/**");
        assertThat(manager.isPathDenied("/admin/debug/test")).isTrue();
        assertThat(manager.isPathDenied("/admin/debug/a/b/c")).isTrue();
        assertThat(manager.isPathDenied("/admin/other")).isFalse();
    }

    @Test
    @DisplayName("静态路径黑名单 — /* 单层通配符")
    void staticDeniedPathSingleWildcard() {
        manager.getDeniedPaths().add("/api/internal/*");
        assertThat(manager.isPathDenied("/api/internal/test")).isTrue();
        assertThat(manager.isPathDenied("/api/internal/a/b")).isFalse();
    }

    @Test
    @DisplayName("动态封禁路径")
    void dynamicDenyPath() {
        manager.denyPath("/temp/danger/**");
        assertThat(manager.isPathDenied("/temp/danger/x")).isTrue();
    }

    @Test
    @DisplayName("动态解封路径")
    void dynamicAllowPath() {
        manager.denyPath("/temp/**");
        manager.allowPath("/temp/**");
        assertThat(manager.isPathDenied("/temp/x")).isFalse();
    }

    // ==================== 启用/禁用 ====================

    @Test
    @DisplayName("未启用时不拦截")
    void disabledDoesNotBlock() {
        manager.setEnabled(false);
        manager.getDeniedIps().add("10.0.0.1");
        assertThat(manager.isIpDenied("10.0.0.1")).isFalse();
    }

    @Test
    @DisplayName("null IP 不拦截")
    void nullIp() {
        assertThat(manager.isIpDenied(null)).isFalse();
    }

    @Test
    @DisplayName("null 路径不拦截")
    void nullPath() {
        assertThat(manager.isPathDenied(null)).isFalse();
    }

    // ==================== 查询 API ====================

    @Test
    @DisplayName("getDynamicDeniedIps 返回不可变副本")
    void dynamicDeniedIpsImmutable() {
        manager.denyIp("1.1.1.1", "test");
        assertThat(manager.getDynamicDeniedIps()).containsExactly("1.1.1.1");
        assertThatThrownBy(() -> manager.getDynamicDeniedIps().add("2.2.2.2"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("getDynamicDeniedPaths 返回不可变副本")
    void dynamicDeniedPathsImmutable() {
        manager.denyPath("/x");
        assertThat(manager.getDynamicDeniedPaths()).containsExactly("/x");
        assertThatThrownBy(() -> manager.getDynamicDeniedPaths().add("/y"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
