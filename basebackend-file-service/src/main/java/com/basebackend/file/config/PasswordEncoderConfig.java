package com.basebackend.file.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;

/**
 * 密码加密策略配置
 *
 * 支持多种密码编码算法，便于逐步升级：
 * - BCryptPasswordEncoder：当前生产环境使用
 * - Argon2PasswordEncoder：未来升级目标（更安全但计算成本更高）
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-28
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * 密码编码器（支持多算法自动识别）
     *
     * 使用 DelegatingPasswordEncoder 支持：
     * 1. 兼容性：自动识别现有加密算法的编码前缀
     * 2. 平滑升级：支持 BCrypt 向 Argon2 迁移
     * 3. 安全性：默认使用最强的 BCrypt
     *
     * @return 密码编码器实例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        Map<String, PasswordEncoder> encoders = new HashMap<>(4);

        // 当前生产环境使用的 BCrypt（安全性高，性能中等）
        // 工作因子默认 10，可通过 -D 参数调整：-Dbcrypt.gostFactor=12
        int bcryptStrength = Integer.parseInt(System.getProperty("bcrypt.strength", "10"));
        encoders.put("bcrypt", new BCryptPasswordEncoder(bcryptStrength));

        // 未来升级目标：Argon2（更安全，但计算成本更高）
        // 构造参数顺序（按 Spring Security 文档）：
        // Argon2PasswordEncoder(int saltLength, int hashLength, int parallelism, int memory, int iterations)
        //
        // <strong>重要</strong>：
        // - memory 参数单位是 KiB（千字节），不是 MB！
        // - 默认值：32MiB (1<<15)，符合 OWASP 安全基线
        // - 升级建议：64MiB (1<<16) 或更高
        int argon2SaltLength = Integer.parseInt(System.getProperty("argon2.saltLength", "16")); // 盐长度：16 字节
        int argon2HashLength = Integer.parseInt(System.getProperty("argon2.hashLength", "32")); // 哈希长度：32 字节
        int argon2Parallelism = Integer.parseInt(System.getProperty("argon2.parallelism", "1")); // 并行度：1
        int argon2Memory = Integer.parseInt(System.getProperty("argon2.memory", String.valueOf(1 << 15))); // 内存成本：32MiB
        int argon2Iterations = Integer.parseInt(System.getProperty("argon2.iterations", "3")); // 迭代次数：3

        encoders.put("argon2",
                new Argon2PasswordEncoder(argon2SaltLength, argon2HashLength, argon2Parallelism, argon2Memory, argon2Iterations));

        // 明文编码器（仅用于测试，生产环境不应使用）
        encoders.put("noop", org.springframework.security.crypto.password.NoOpPasswordEncoder.getInstance());

        // 创建委托密码编码器
        DelegatingPasswordEncoder passwordEncoder = new DelegatingPasswordEncoder(
                "bcrypt", // 默认编码算法
                encoders
        );

        // 设置未编码密码的默认编码格式（兼容老数据）
        passwordEncoder.setDefaultPasswordEncoderForMatches(encoders.get("bcrypt"));

        return passwordEncoder;
    }

    /**
     * 获取当前生产环境使用的密码编码器标识符
     *
     * @return 编码算法标识符
     */
    public static String getCurrentEncoderId() {
        return "bcrypt"; // 当前使用 BCrypt
    }

    /**
     * 获取推荐的未来升级编码器标识符
     *
     * @return 推荐升级的编码算法
     */
    public static String getRecommendedEncoderId() {
        return "argon2"; // 未来升级推荐 Argon2
    }
}
