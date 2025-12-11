# Cache Warming Usage Guide

## 概述

缓存预热功能允许在应用启动时自动加载热点数据到缓存中，提升用户体验。

## 配置

### 启用缓存预热

在 `application.yml` 中配置：

```yaml
basebackend:
  cache:
    warming:
      # 启用缓存预热
      enabled: true
      # 预热超时时间
      timeout: 5m
      # 是否异步预热（推荐）
      async: true
```

## 使用方式

### 方式一：通过代码注册预热任务

在应用启动类或配置类中注册预热任务：

```java
@Configuration
public class CacheWarmingConfiguration {

    @Autowired
    private CacheWarmingManager warmingManager;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ProductService productService;

    @PostConstruct
    public void registerWarmingTasks() {
        // 注册用户缓存预热任务
        CacheWarmingTask userTask = CacheWarmingTask.builder()
                .name("warm-active-users")
                .priority(1) // 优先级 1（数字越小优先级越高）
                .dataLoader(() -> {
                    // 加载活跃用户数据
                    List<User> activeUsers = userService.getActiveUsers();
                    Map<String, Object> data = new HashMap<>();
                    for (User user : activeUsers) {
                        String key = "user:" + user.getId();
                        data.put(key, user);
                    }
                    return data;
                })
                .ttl(Duration.ofHours(2))
                .async(true)
                .description("预热活跃用户缓存")
                .build();
        
        warmingManager.registerWarmingTask(userTask);

        // 注册产品缓存预热任务
        CacheWarmingTask productTask = CacheWarmingTask.builder()
                .name("warm-hot-products")
                .priority(2) // 优先级 2
                .dataLoader(() -> {
                    // 加载热门产品数据
                    List<Product> hotProducts = productService.getHotProducts();
                    Map<String, Object> data = new HashMap<>();
                    for (Product product : hotProducts) {
                        String key = "product:" + product.getId();
                        data.put(key, product);
                    }
                    return data;
                })
                .ttl(Duration.ofHours(1))
                .async(true)
                .description("预热热门产品缓存")
                .build();
        
        warmingManager.registerWarmingTask(productTask);
    }
}
```

### 方式二：手动触发预热

```java
@Service
public class CacheManagementService {

    @Autowired
    private CacheWarmingManager warmingManager;

    /**
     * 手动触发缓存预热
     */
    public void triggerCacheWarming() {
        // 同步执行
        warmingManager.executeWarmingTasks();
        
        // 或异步执行
        // warmingManager.executeWarmingTasksAsync();
    }

    /**
     * 获取预热进度
     */
    public WarmingProgress getWarmingProgress() {
        return warmingManager.getProgress();
    }
}
```

## 预热任务配置

### CacheWarmingTask 参数说明

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | String | 是 | 任务名称（唯一标识） |
| priority | int | 否 | 优先级（数字越小优先级越高，默认 0） |
| dataLoader | Supplier<Map<String, Object>> | 是 | 数据加载器，返回键值对 |
| ttl | Duration | 否 | 缓存过期时间 |
| async | boolean | 否 | 是否异步执行（默认 false） |
| description | String | 否 | 任务描述 |

### 数据加载器示例

```java
// 示例 1: 从数据库加载
Supplier<Map<String, Object>> dbLoader = () -> {
    List<Entity> entities = repository.findAll();
    return entities.stream()
            .collect(Collectors.toMap(
                e -> "entity:" + e.getId(),
                e -> e
            ));
};

// 示例 2: 从外部 API 加载
Supplier<Map<String, Object>> apiLoader = () -> {
    List<Data> dataList = externalApiClient.fetchData();
    Map<String, Object> result = new HashMap<>();
    for (Data data : dataList) {
        result.put("api:data:" + data.getId(), data);
    }
    return result;
};

// 示例 3: 从文件加载
Supplier<Map<String, Object>> fileLoader = () -> {
    try {
        String content = Files.readString(Path.of("config/cache-data.json"));
        return objectMapper.readValue(content, new TypeReference<Map<String, Object>>() {});
    } catch (IOException e) {
        log.error("Failed to load cache data from file", e);
        return Collections.emptyMap();
    }
};
```

## 监控预热进度

### 获取预热进度

```java
WarmingProgress progress = warmingManager.getProgress();

System.out.println("总任务数: " + progress.getTotalTasks());
System.out.println("已完成任务数: " + progress.getCompletedTasks());
System.out.println("成功任务数: " + progress.getSuccessTasks());
System.out.println("失败任务数: " + progress.getFailedTasks());
System.out.println("完成百分比: " + progress.getCompletionPercentage() + "%");
System.out.println("总耗时: " + progress.getTotalExecutionTime() + "ms");
```

### 查看任务详情

```java
List<CacheWarmingTask> tasks = warmingManager.getTasks();

for (CacheWarmingTask task : tasks) {
    System.out.println("任务名称: " + task.getName());
    System.out.println("任务状态: " + task.getStatus());
    System.out.println("数据条目数: " + task.getItemCount());
    System.out.println("已加载条目数: " + task.getLoadedCount());
    System.out.println("失败条目数: " + task.getFailedCount());
    System.out.println("执行耗时: " + task.getExecutionTime() + "ms");
    System.out.println("成功率: " + (task.getSuccessRate() * 100) + "%");
}
```

## 最佳实践

### 1. 优先级设置

- 核心业务数据使用低优先级数字（如 1、2）
- 次要数据使用高优先级数字（如 10、20）
- 优先级相同的任务会并发执行（异步模式下）

### 2. 数据量控制

- 避免一次性加载过多数据
- 建议单个任务加载数据不超过 10000 条
- 大数据量可以拆分为多个任务

### 3. 异步执行

- 推荐使用异步模式（`async: true`）
- 异步模式不会阻塞应用启动
- 可以设置合理的超时时间

### 4. 错误处理

- 数据加载器应该捕获异常并返回空 Map
- 预热失败不会影响应用启动
- 查看日志了解失败原因

### 5. TTL 设置

- 根据数据更新频率设置合理的 TTL
- 热点数据可以设置较长的 TTL（如 2-4 小时）
- 实时性要求高的数据设置较短的 TTL（如 5-15 分钟）

## 示例：完整的预热配置

```java
@Configuration
@Slf4j
public class CacheWarmingConfig {

    @Autowired
    private CacheWarmingManager warmingManager;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private ConfigRepository configRepository;

    @PostConstruct
    public void setupCacheWarming() {
        log.info("Setting up cache warming tasks");

        // 1. 系统配置预热（最高优先级）
        registerConfigWarmingTask();

        // 2. 用户数据预热
        registerUserWarmingTask();

        // 3. 产品数据预热
        registerProductWarmingTask();

        log.info("Cache warming tasks registered: {}", warmingManager.getTaskCount());
    }

    private void registerConfigWarmingTask() {
        CacheWarmingTask task = CacheWarmingTask.builder()
                .name("warm-system-config")
                .priority(1)
                .dataLoader(() -> {
                    List<Config> configs = configRepository.findAll();
                    return configs.stream()
                            .collect(Collectors.toMap(
                                c -> "config:" + c.getKey(),
                                c -> c.getValue()
                            ));
                })
                .ttl(Duration.ofHours(24))
                .async(false) // 同步执行，确保配置先加载
                .description("预热系统配置")
                .build();
        
        warmingManager.registerWarmingTask(task);
    }

    private void registerUserWarmingTask() {
        CacheWarmingTask task = CacheWarmingTask.builder()
                .name("warm-active-users")
                .priority(2)
                .dataLoader(() -> {
                    // 只加载最近 30 天活跃的用户
                    LocalDateTime since = LocalDateTime.now().minusDays(30);
                    List<User> users = userRepository.findActiveUsersSince(since);
                    
                    log.info("Loading {} active users for warming", users.size());
                    
                    return users.stream()
                            .collect(Collectors.toMap(
                                u -> "user:" + u.getId(),
                                u -> u
                            ));
                })
                .ttl(Duration.ofHours(2))
                .async(true)
                .description("预热活跃用户数据")
                .build();
        
        warmingManager.registerWarmingTask(task);
    }

    private void registerProductWarmingTask() {
        CacheWarmingTask task = CacheWarmingTask.builder()
                .name("warm-hot-products")
                .priority(3)
                .dataLoader(() -> {
                    // 加载热门产品（按销量排序，取前 1000）
                    List<Product> products = productRepository.findTopProducts(1000);
                    
                    log.info("Loading {} hot products for warming", products.size());
                    
                    return products.stream()
                            .collect(Collectors.toMap(
                                p -> "product:" + p.getId(),
                                p -> p
                            ));
                })
                .ttl(Duration.ofHours(1))
                .async(true)
                .description("预热热门产品数据")
                .build();
        
        warmingManager.registerWarmingTask(task);
    }
}
```

## 故障排查

### 预热任务未执行

1. 检查配置：`basebackend.cache.warming.enabled: true`
2. 检查日志：搜索 "cache warming" 关键字
3. 确认任务已注册：调用 `warmingManager.getTaskCount()`

### 预热任务失败

1. 查看任务状态：`task.getStatus()` 和 `task.getErrorMessage()`
2. 检查数据加载器是否抛出异常
3. 确认 Redis 连接正常
4. 检查数据序列化是否正确

### 预热超时

1. 增加超时时间：`basebackend.cache.warming.timeout`
2. 减少单个任务的数据量
3. 使用异步模式：`async: true`
4. 拆分大任务为多个小任务

## 性能建议

1. **控制数据量**：单个任务建议不超过 10000 条数据
2. **合理设置优先级**：核心数据优先加载
3. **使用异步模式**：避免阻塞应用启动
4. **监控预热效果**：通过日志和指标评估预热效果
5. **定期更新**：根据业务变化调整预热策略
