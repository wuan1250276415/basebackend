package com.basebackend.gateway.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * IP 地址工具类
 * <p>
 * 提供高效的 IP 地址解析和处理功能：
 * <ul>
 * <li>IPv4/IPv6 地址解析</li>
 * <li>端口号剥离</li>
 * <li>CIDR 匹配</li>
 * <li>结果缓存</li>
 * </ul>
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
public final class IpAddressUtil {

    private IpAddressUtil() {
        // 工具类禁止实例化
    }

    /**
     * IPv4:port 格式匹配（如 192.168.1.1:8080）
     */
    private static final Pattern IPV4_PORT_PATTERN = Pattern.compile(
            "^(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}):(\\d+)$");

    /**
     * IPv6 带端口格式匹配（如 [2001:db8::1]:8080）
     */
    private static final Pattern IPV6_BRACKET_PORT_PATTERN = Pattern.compile(
            "^\\[([0-9a-fA-F:]+)]:(\\d+)$");

    /**
     * IPv6 带括号无端口格式（如 [2001:db8::1]）
     */
    private static final Pattern IPV6_BRACKET_PATTERN = Pattern.compile(
            "^\\[([0-9a-fA-F:]+)]$");

    /**
     * IP 解析结果缓存（最多 10000 条，5 分钟过期）
     */
    private static final Cache<String, String> IP_CACHE = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofMinutes(5))
            .build();

    /**
     * CIDR 匹配器缓存
     */
    private static final Cache<String, CidrMatcher> CIDR_CACHE = Caffeine.newBuilder()
            .maximumSize(1_000)
            .expireAfterWrite(Duration.ofHours(1))
            .build();

    /**
     * 从 host:port 字符串中提取 IP 地址（带缓存）
     * <p>
     * 支持格式：
     * <ul>
     * <li>IPv4: 192.168.1.1</li>
     * <li>IPv4:port: 192.168.1.1:8080</li>
     * <li>IPv6: 2001:db8::1</li>
     * <li>IPv6 带括号: [2001:db8::1]</li>
     * <li>IPv6 带端口: [2001:db8::1]:8080</li>
     * </ul>
     * </p>
     *
     * @param hostPort host:port 字符串
     * @return 纯 IP 地址
     */
    public static String stripPort(String hostPort) {
        if (hostPort == null || hostPort.isEmpty()) {
            return hostPort;
        }

        return IP_CACHE.get(hostPort, IpAddressUtil::doStripPort);
    }

    /**
     * 实际执行端口剥离
     */
    private static String doStripPort(String hostPort) {
        // 1. 检查 IPv6 带括号带端口格式：[2001:db8::1]:8080
        Matcher ipv6PortMatcher = IPV6_BRACKET_PORT_PATTERN.matcher(hostPort);
        if (ipv6PortMatcher.matches()) {
            return ipv6PortMatcher.group(1);
        }

        // 2. 检查 IPv6 带括号无端口格式：[2001:db8::1]
        Matcher ipv6BracketMatcher = IPV6_BRACKET_PATTERN.matcher(hostPort);
        if (ipv6BracketMatcher.matches()) {
            return ipv6BracketMatcher.group(1);
        }

        // 3. 检查 IPv4:port 格式：192.168.1.1:8080
        Matcher ipv4PortMatcher = IPV4_PORT_PATTERN.matcher(hostPort);
        if (ipv4PortMatcher.matches()) {
            return ipv4PortMatcher.group(1);
        }

        // 4. 其他情况（纯 IPv4 或纯 IPv6），直接返回
        return hostPort;
    }

    /**
     * 检查 IP 是否匹配 CIDR（带缓存）
     *
     * @param ip   IP 地址
     * @param cidr CIDR 格式（如 192.168.1.0/24）
     * @return 是否匹配
     */
    public static boolean matchesCidr(String ip, String cidr) {
        if (ip == null || cidr == null) {
            return false;
        }

        CidrMatcher matcher = CIDR_CACHE.get(cidr, CidrMatcher::new);
        return matcher != null && matcher.matches(ip);
    }

    /**
     * 判断是否为 IPv4 地址
     *
     * @param ip IP 地址
     * @return 是否为 IPv4
     */
    public static boolean isIPv4(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        return ip.indexOf(':') < 0 && ip.indexOf('.') > 0;
    }

    /**
     * 判断是否为 IPv6 地址
     *
     * @param ip IP 地址
     * @return 是否为 IPv6
     */
    public static boolean isIPv6(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        return ip.indexOf(':') >= 0;
    }

    /**
     * 清除 IP 缓存
     */
    public static void clearCache() {
        IP_CACHE.invalidateAll();
        CIDR_CACHE.invalidateAll();
        log.info("IP 缓存已清除");
    }

    /**
     * 获取缓存统计信息
     *
     * @return 缓存统计字符串
     */
    public static String getCacheStats() {
        return String.format("IP Cache: estimated=%d, CIDR Cache: estimated=%d",
                IP_CACHE.estimatedSize(),
                CIDR_CACHE.estimatedSize());
    }

    /**
     * CIDR 匹配器（使用位运算优化）
     * <p>
     * 预计算网络掩码，使用位运算进行高效匹配。
     * </p>
     */
    public static class CidrMatcher {
        private final String cidr;
        private final byte[] networkBytes;
        private final byte[] maskBytes;
        private final int addressLength;
        private final boolean valid;

        public CidrMatcher(String cidr) {
            this.cidr = cidr;
            byte[] tempNetwork = null;
            byte[] tempMask = null;
            int tempLength = 0;
            boolean tempValid = false;

            try {
                String[] parts = cidr.split("/");
                String ip = parts[0];
                InetAddress networkAddress = InetAddress.getByName(ip);
                tempNetwork = networkAddress.getAddress();
                tempLength = tempNetwork.length;

                // 解析前缀长度
                int prefixLength;
                if (parts.length > 1) {
                    prefixLength = Integer.parseInt(parts[1]);
                } else {
                    prefixLength = tempLength * 8; // 无掩码时使用完整匹配
                }

                int maxPrefixLength = tempLength * 8;
                if (prefixLength < 0 || prefixLength > maxPrefixLength) {
                    throw new IllegalArgumentException(
                            String.format("Invalid prefix length %d for CIDR %s (max: %d)",
                                    prefixLength, cidr, maxPrefixLength));
                }

                // 预计算掩码字节数组
                tempMask = createMask(tempLength, prefixLength);

                // 将网络地址与掩码进行 AND 运算，确保网络地址是规范的
                for (int i = 0; i < tempLength; i++) {
                    tempNetwork[i] = (byte) (tempNetwork[i] & tempMask[i]);
                }

                tempValid = true;
            } catch (Exception e) {
                log.warn("无效的 CIDR: {}", cidr, e);
            }

            this.networkBytes = tempNetwork;
            this.maskBytes = tempMask;
            this.addressLength = tempLength;
            this.valid = tempValid;
        }

        /**
         * 创建掩码字节数组
         */
        private static byte[] createMask(int length, int prefixLength) {
            byte[] mask = new byte[length];
            int fullBytes = prefixLength / 8;
            int remainingBits = prefixLength % 8;

            // 填充完整的 0xFF 字节
            for (int i = 0; i < fullBytes && i < length; i++) {
                mask[i] = (byte) 0xFF;
            }

            // 填充剩余位
            if (fullBytes < length && remainingBits > 0) {
                mask[fullBytes] = (byte) (0xFF << (8 - remainingBits));
            }

            return mask;
        }

        /**
         * 检查 IP 是否匹配此 CIDR
         *
         * @param ip IP 地址
         * @return 是否匹配
         */
        public boolean matches(String ip) {
            if (!valid) {
                return false;
            }

            try {
                InetAddress address = InetAddress.getByName(ip);
                byte[] addressBytes = address.getAddress();

                // 检查地址长度是否匹配（IPv4 vs IPv6）
                if (addressBytes.length != addressLength) {
                    return false;
                }

                // 使用位运算进行高效匹配
                for (int i = 0; i < addressLength; i++) {
                    if ((addressBytes[i] & maskBytes[i]) != networkBytes[i]) {
                        return false;
                    }
                }

                return true;
            } catch (Exception e) {
                log.debug("IP 匹配失败: {} vs CIDR {}", ip, cidr, e);
                return false;
            }
        }

        public boolean isValid() {
            return valid;
        }

        public String getCidr() {
            return cidr;
        }
    }
}
