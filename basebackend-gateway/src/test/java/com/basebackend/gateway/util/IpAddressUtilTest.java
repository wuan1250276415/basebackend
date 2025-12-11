package com.basebackend.gateway.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * IpAddressUtil 单元测试
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@DisplayName("IpAddressUtil 单元测试")
class IpAddressUtilTest {

    @Nested
    @DisplayName("stripPort 方法测试")
    class StripPortTests {

        @Test
        @DisplayName("IPv4 地址无端口应该返回原值")
        void shouldReturnOriginalForIPv4WithoutPort() {
            assertEquals("192.168.1.1", IpAddressUtil.stripPort("192.168.1.1"));
        }

        @Test
        @DisplayName("IPv4 地址带端口应该剥离端口")
        void shouldStripPortFromIPv4() {
            assertEquals("192.168.1.1", IpAddressUtil.stripPort("192.168.1.1:8080"));
            assertEquals("10.0.0.1", IpAddressUtil.stripPort("10.0.0.1:80"));
            assertEquals("127.0.0.1", IpAddressUtil.stripPort("127.0.0.1:3000"));
        }

        @Test
        @DisplayName("IPv6 地址无端口应该返回原值")
        void shouldReturnOriginalForIPv6WithoutPort() {
            assertEquals("2001:db8::1", IpAddressUtil.stripPort("2001:db8::1"));
            assertEquals("::1", IpAddressUtil.stripPort("::1"));
        }

        @Test
        @DisplayName("IPv6 地址带括号无端口应该剥离括号")
        void shouldStripBracketsFromIPv6() {
            assertEquals("2001:db8::1", IpAddressUtil.stripPort("[2001:db8::1]"));
            assertEquals("::1", IpAddressUtil.stripPort("[::1]"));
        }

        @Test
        @DisplayName("IPv6 地址带括号和端口应该剥离端口")
        void shouldStripPortFromIPv6WithBrackets() {
            assertEquals("2001:db8::1", IpAddressUtil.stripPort("[2001:db8::1]:8080"));
            assertEquals("::1", IpAddressUtil.stripPort("[::1]:80"));
        }

        @Test
        @DisplayName("空值和 null 应该返回原值")
        void shouldHandleNullAndEmpty() {
            assertNull(IpAddressUtil.stripPort(null));
            assertEquals("", IpAddressUtil.stripPort(""));
        }

        @Test
        @DisplayName("缓存应该生效")
        void shouldUseCache() {
            // 第一次调用
            String result1 = IpAddressUtil.stripPort("192.168.1.1:8080");
            // 第二次调用（应该从缓存获取）
            String result2 = IpAddressUtil.stripPort("192.168.1.1:8080");

            assertEquals(result1, result2);
            assertEquals("192.168.1.1", result1);
        }
    }

    @Nested
    @DisplayName("CIDR 匹配测试")
    class CidrMatchTests {

        @Test
        @DisplayName("IPv4 CIDR 匹配应该正确")
        void shouldMatchIPv4Cidr() {
            assertTrue(IpAddressUtil.matchesCidr("192.168.1.100", "192.168.1.0/24"));
            assertTrue(IpAddressUtil.matchesCidr("192.168.1.1", "192.168.1.0/24"));
            assertTrue(IpAddressUtil.matchesCidr("192.168.1.254", "192.168.1.0/24"));

            assertFalse(IpAddressUtil.matchesCidr("192.168.2.1", "192.168.1.0/24"));
            assertFalse(IpAddressUtil.matchesCidr("10.0.0.1", "192.168.1.0/24"));
        }

        @Test
        @DisplayName("IPv4 /16 子网匹配应该正确")
        void shouldMatchIPv4Cidr16() {
            assertTrue(IpAddressUtil.matchesCidr("192.168.0.1", "192.168.0.0/16"));
            assertTrue(IpAddressUtil.matchesCidr("192.168.255.255", "192.168.0.0/16"));

            assertFalse(IpAddressUtil.matchesCidr("192.169.0.1", "192.168.0.0/16"));
        }

        @Test
        @DisplayName("IPv4 完全匹配应该正确")
        void shouldMatchIPv4Exact() {
            assertTrue(IpAddressUtil.matchesCidr("192.168.1.1", "192.168.1.1/32"));
            assertFalse(IpAddressUtil.matchesCidr("192.168.1.2", "192.168.1.1/32"));
        }

        @Test
        @DisplayName("IPv6 CIDR 匹配应该正确")
        void shouldMatchIPv6Cidr() {
            assertTrue(IpAddressUtil.matchesCidr("2001:db8::1", "2001:db8::/32"));
            assertTrue(IpAddressUtil.matchesCidr("2001:db8:ffff::1", "2001:db8::/32"));

            assertFalse(IpAddressUtil.matchesCidr("2001:db9::1", "2001:db8::/32"));
        }

        @Test
        @DisplayName("不同地址族不应该匹配")
        void shouldNotMatchDifferentAddressFamilies() {
            assertFalse(IpAddressUtil.matchesCidr("192.168.1.1", "2001:db8::/32"));
            assertFalse(IpAddressUtil.matchesCidr("2001:db8::1", "192.168.1.0/24"));
        }

        @Test
        @DisplayName("null 参数应该返回 false")
        void shouldReturnFalseForNull() {
            assertFalse(IpAddressUtil.matchesCidr(null, "192.168.1.0/24"));
            assertFalse(IpAddressUtil.matchesCidr("192.168.1.1", null));
            assertFalse(IpAddressUtil.matchesCidr(null, null));
        }
    }

    @Nested
    @DisplayName("IP 类型判断测试")
    class IpTypeTests {

        @Test
        @DisplayName("isIPv4 应该正确识别 IPv4 地址")
        void shouldIdentifyIPv4() {
            assertTrue(IpAddressUtil.isIPv4("192.168.1.1"));
            assertTrue(IpAddressUtil.isIPv4("10.0.0.1"));
            assertTrue(IpAddressUtil.isIPv4("127.0.0.1"));

            assertFalse(IpAddressUtil.isIPv4("2001:db8::1"));
            assertFalse(IpAddressUtil.isIPv4("::1"));
            assertFalse(IpAddressUtil.isIPv4(null));
            assertFalse(IpAddressUtil.isIPv4(""));
        }

        @Test
        @DisplayName("isIPv6 应该正确识别 IPv6 地址")
        void shouldIdentifyIPv6() {
            assertTrue(IpAddressUtil.isIPv6("2001:db8::1"));
            assertTrue(IpAddressUtil.isIPv6("::1"));
            assertTrue(IpAddressUtil.isIPv6("fe80::1"));

            assertFalse(IpAddressUtil.isIPv6("192.168.1.1"));
            assertFalse(IpAddressUtil.isIPv6(null));
            assertFalse(IpAddressUtil.isIPv6(""));
        }
    }

    @Nested
    @DisplayName("CidrMatcher 内部类测试")
    class CidrMatcherTests {

        @Test
        @DisplayName("有效的 CIDR 应该创建成功")
        void shouldCreateValidCidr() {
            IpAddressUtil.CidrMatcher matcher = new IpAddressUtil.CidrMatcher("192.168.1.0/24");
            assertTrue(matcher.isValid());
            assertEquals("192.168.1.0/24", matcher.getCidr());
        }

        @Test
        @DisplayName("无效的 CIDR 应该标记为无效")
        void shouldMarkInvalidCidr() {
            IpAddressUtil.CidrMatcher matcher = new IpAddressUtil.CidrMatcher("invalid");
            assertFalse(matcher.isValid());
        }

        @Test
        @DisplayName("无前缀长度的 CIDR 应该使用默认值")
        void shouldUseDefaultPrefixLength() {
            IpAddressUtil.CidrMatcher matcher = new IpAddressUtil.CidrMatcher("192.168.1.1");
            assertTrue(matcher.isValid());
            assertTrue(matcher.matches("192.168.1.1"));
            assertFalse(matcher.matches("192.168.1.2"));
        }

        @Test
        @DisplayName("超出范围的前缀长度应该标记为无效")
        void shouldMarkInvalidPrefixLength() {
            IpAddressUtil.CidrMatcher matcher = new IpAddressUtil.CidrMatcher("192.168.1.0/33");
            assertFalse(matcher.isValid());
        }
    }

    @Nested
    @DisplayName("缓存管理测试")
    class CacheTests {

        @Test
        @DisplayName("clearCache 应该清除缓存")
        void shouldClearCache() {
            // 添加一些缓存项
            IpAddressUtil.stripPort("192.168.1.1:8080");
            IpAddressUtil.matchesCidr("192.168.1.1", "192.168.1.0/24");

            // 清除缓存
            IpAddressUtil.clearCache();

            // 验证缓存统计
            String stats = IpAddressUtil.getCacheStats();
            assertNotNull(stats);
            assertTrue(stats.contains("IP Cache"));
        }

        @Test
        @DisplayName("getCacheStats 应该返回统计信息")
        void shouldReturnCacheStats() {
            String stats = IpAddressUtil.getCacheStats();
            assertNotNull(stats);
            assertTrue(stats.contains("IP Cache"));
            assertTrue(stats.contains("CIDR Cache"));
        }
    }
}
