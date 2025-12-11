package com.basebackend.scheduler.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

/**
 * PowerJob分布式调度配置属性
 * <p>
 * 提供PowerJob Worker客户端的配置管理,支持从Nacos配置中心动态刷新。
 * 配置前缀: scheduler.powerjob
 * </p>
 *
 * <h3>PowerJob简介:</h3>
 * <p>
 * PowerJob是一款企业级分布式任务调度与计算框架,支持CRON、API、固定频率、固定延迟等调度策略,
 * 提供MapReduce、广播、分片等多种执行模式,具备在线日志、动态参数、失败告警等丰富功能。
 * </p>
 *
 * <h3>配置项分类:</h3>
 * <ul>
 *   <li><b>基础配置</b>: appName(应用名)、serverAddress(服务器地址)、token(认证令牌)</li>
 *   <li><b>Worker线程池</b>: workerCorePoolSize(核心线程数)、workerMaxPoolSize(最大线程数)</li>
 *   <li><b>心跳配置</b>: heartbeatIntervalMillis(心跳间隔)、maxTaskExecutionTimeoutSeconds(任务超时)</li>
 * </ul>
 *
 * <h3>使用示例:</h3>
 * <pre>{@code
 * @Autowired
 * private PowerJobProperties powerJobProperties;
 *
 * public void example() {
 *     // 获取配置值
 *     String appName = powerJobProperties.getAppName();
 *     List<String> serverAddresses = powerJobProperties.getServerAddress();
 *     int corePoolSize = powerJobProperties.getWorkerCorePoolSize();
 *
 *     // 配置会自动从Nacos刷新(@RefreshScope)
 * }
 * }</pre>
 *
 * <h3>配置文件示例(application.yml):</h3>
 * <pre>
 * scheduler:
 *   powerjob:
 *     app-name: basebackend-scheduler
 *     server-address: 192.168.1.100:7700,192.168.1.101:7700
 *     token: ENC(加密后的token)
 *     worker-core-pool-size: 4
 *     worker-max-pool-size: 8
 *     heartbeat-interval-millis: 5000
 *     max-task-execution-timeout-seconds: 0
 * </pre>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-24
 * @see org.springframework.boot.context.properties.ConfigurationProperties
 * @see org.springframework.cloud.context.config.annotation.RefreshScope
 */
@Data
@Component
@Validated
@RefreshScope
@ConfigurationProperties(prefix = "scheduler.powerjob")
public class PowerJobProperties {

    // ========== 基础配置 ==========

    /**
     * PowerJob应用名称
     * <p>
     * 必须与PowerJob服务端注册的应用名一致,用于身份识别和任务路由。
     * 应用名是PowerJob的核心概念,一个应用对应一组Worker节点。
     * </p>
     * <p>
     * 命名规范:
     * <ul>
     *   <li>使用小写字母、数字、连字符</li>
     *   <li>格式: {项目名}-{模块名}</li>
     *   <li>示例: basebackend-scheduler</li>
     * </ul>
     * </p>
     * <p>
     * 默认值: "basebackend-scheduler"
     * </p>
     */
    @NotBlank(message = "PowerJob应用名称不能为空")
    private String appName = "basebackend-scheduler";

    /**
     * PowerJob服务端地址列表
     * <p>
     * PowerJob Server的访问地址,支持多个地址实现高可用。
     * Worker会自动连接到可用的Server节点。
     * </p>
     * <p>
     * 格式:
     * <ul>
     *   <li>单节点: ["host:port"]</li>
     *   <li>多节点: ["host1:port1", "host2:port2"]</li>
     *   <li>示例: ["192.168.1.100:7700", "192.168.1.101:7700"]</li>
     * </ul>
     * </p>
     * <p>
     * 默认值: ["127.0.0.1:7700"]
     * </p>
     * <p>
     * 注意: 生产环境建议配置多个节点实现高可用
     * </p>
     */
    @NotEmpty(message = "PowerJob服务端地址不能为空")
    private List<String> serverAddress = new ArrayList<>(List.of("127.0.0.1:7700"));

    /**
     * 认证Token
     * <p>
     * 用于Worker向Server注册时的身份验证。
     * 必须与PowerJob服务端配置的Token一致。
     * </p>
     * <p>
     * 安全建议:
     * <ul>
     *   <li>生产环境必须使用Jasypt加密: ENC(加密后的token)</li>
     *   <li>定期轮换Token,提高安全性</li>
     *   <li>避免在日志中打印明文Token</li>
     * </ul>
     * </p>
     * <p>
     * 默认值: null(空)
     * </p>
     * <p>
     * 注意: 如果Server启用了Token验证,此项必填
     * </p>
     */
    private String token;

    // ========== Worker线程池配置 ==========

    /**
     * Worker核心线程数
     * <p>
     * 执行任务的核心线程池大小。
     * 建议根据CPU核心数和任务类型调整:
     * <ul>
     *   <li>CPU密集型任务: CPU核心数 + 1</li>
     *   <li>IO密集型任务: CPU核心数 * 2</li>
     *   <li>混合型任务: CPU核心数 * 1.5</li>
     * </ul>
     * </p>
     * <p>
     * 默认值: Math.max(2, CPU核心数)
     * </p>
     * <p>
     * 注意: 线程数过多会增加上下文切换开销,过少会降低并发能力
     * </p>
     */
    @Min(value = 1, message = "Worker核心线程数必须大于等于1")
    private int workerCorePoolSize = Math.max(2, Runtime.getRuntime().availableProcessors());

    /**
     * Worker最大线程数
     * <p>
     * 执行任务的最大线程池大小。
     * 当核心线程池满载时,会创建新线程直到达到此上限。
     * 建议设置为核心线程数的2-4倍。
     * </p>
     * <p>
     * 默认值: Math.max(4, CPU核心数 * 2)
     * </p>
     * <p>
     * 注意: 最大线程数应该大于等于核心线程数
     * </p>
     */
    @Min(value = 1, message = "Worker最大线程数必须大于等于1")
    private int workerMaxPoolSize = Math.max(4, Runtime.getRuntime().availableProcessors() * 2);

    // ========== 心跳配置 ==========

    /**
     * 心跳间隔(毫秒)
     * <p>
     * Worker向Server发送心跳的时间间隔。
     * 心跳用于保持连接活跃和上报Worker状态。
     * </p>
     * <p>
     * 推荐值:
     * <ul>
     *   <li>局域网环境: 3000-5000毫秒</li>
     *   <li>公网环境: 5000-10000毫秒</li>
     *   <li>不稳定网络: 10000-30000毫秒</li>
     * </ul>
     * </p>
     * <p>
     * 默认值: 5000(5秒)
     * </p>
     * <p>
     * 注意: 间隔过短会增加网络开销,过长可能导致Server误判Worker下线
     * </p>
     */
    @Min(value = 1000, message = "心跳间隔必须大于等于1000毫秒")
    private long heartbeatIntervalMillis = 5000L;

    /**
     * 任务执行超时时间(秒)
     * <p>
     * 单个任务的最大执行时间。
     * 超过此时间的任务会被强制中断(如果支持中断)。
     * </p>
     * <p>
     * 特殊值:
     * <ul>
     *   <li>0: 不限制超时(默认)</li>
     *   <li>&gt; 0: 指定超时时间(秒)</li>
     * </ul>
     * </p>
     * <p>
     * 默认值: 0(不限制)
     * </p>
     * <p>
     * 注意: 超时时间应该根据任务的实际执行时间设置,避免误杀正常任务
     * </p>
     */
    @Min(value = 0, message = "任务执行超时时间必须大于等于0秒")
    private long maxTaskExecutionTimeoutSeconds = 0L;

    /**
     * 验证配置的有效性
     * <p>
     * Spring Boot会在配置绑定后自动调用此方法(通过@Validated)。
     * 这里可以添加自定义的跨字段验证逻辑。
     * </p>
     *
     * @throws IllegalArgumentException 如果配置无效
     */
    public void validate() {
        // 验证最大线程数是否大于等于核心线程数
        if (workerMaxPoolSize < workerCorePoolSize) {
            throw new IllegalArgumentException(
                    String.format("[PowerJob配置错误] Worker最大线程数(%d)不能小于核心线程数(%d)",
                            workerMaxPoolSize, workerCorePoolSize));
        }

        // 验证服务器地址列表非空
        if (serverAddress == null || serverAddress.isEmpty()) {
            throw new IllegalArgumentException("[PowerJob配置错误] 服务器地址列表不能为空");
        }

        // 过滤并验证服务器地址格式
        List<String> validAddresses = new ArrayList<>();
        for (String address : serverAddress) {
            // 跳过空白项
            if (address == null || address.trim().isEmpty()) {
                continue;
            }

            String trimmedAddress = address.trim();

            // 验证地址格式: host:port
            if (!trimmedAddress.contains(":")) {
                throw new IllegalArgumentException(
                        "[PowerJob配置错误] 服务器地址格式无效: '" + trimmedAddress + "' (应为 host:port)");
            }

            String[] parts = trimmedAddress.split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException(
                        "[PowerJob配置错误] 服务器地址格式无效: '" + trimmedAddress + "' (应为 host:port)");
            }

            // 验证host非空
            if (parts[0].trim().isEmpty()) {
                throw new IllegalArgumentException(
                        "[PowerJob配置错误] 服务器地址host不能为空: '" + trimmedAddress + "'");
            }

            // 验证端口
            try {
                int port = Integer.parseInt(parts[1].trim());
                if (port < 1 || port > 65535) {
                    throw new IllegalArgumentException(
                            "[PowerJob配置错误] 服务器端口无效: " + port + " (应在1-65535之间)");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "[PowerJob配置错误] 服务器端口不是有效的数字: '" + parts[1] + "'", e);
            }

            validAddresses.add(trimmedAddress);
        }

        // 验证过滤后至少有一个有效地址
        if (validAddresses.isEmpty()) {
            throw new IllegalArgumentException("[PowerJob配置错误] 服务器地址列表中没有有效地址");
        }

        // 更新为清理后的地址列表
        this.serverAddress = validAddresses;
    }

    /**
     * 检查Token是否已配置
     *
     * @return true如果Token已配置且非空
     */
    public boolean isTokenConfigured() {
        return token != null && !token.trim().isEmpty();
    }
}
