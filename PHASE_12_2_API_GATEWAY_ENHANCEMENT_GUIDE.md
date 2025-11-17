# Phase 12.2: API 网关增强实施指南

## 📋 概述

本指南介绍如何增强 API 网关的功能，包括动态路由规则、流量控制、灰度发布、API 版本管理等核心能力，构建智能化的 API 管理平台。

---

## 🏗️ API 网关架构

### 架构图

```
┌─────────────────────────────────────────────────────────────────────┐
│                      增强型 API 网关架构                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐           │
│  │   路由引擎    │  │   流量控制    │  │   安全防护    │           │
│  │              │  │              │  │              │           │
│  │ • 动态路由    │  │ • 限流熔断    │  │ • 认证授权    │           │
│  │ • 条件匹配    │  │ • 负载均衡    │  │ • 安全防护    │           │
│  │ • 权重分配    │  │ • 缓存策略    │  │ • WAF 防护    │           │
│  │ • 重试机制    │  │ • 降级服务    │  │ • 防重放     │           │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘           │
│         │                 │                 │                   │
│  ┌──────▼────────┐  ┌─────▼──────┐  ┌──────▼──────┐           │
│  │   灰度发布     │  │   版本管理   │  │   监控审计   │           │
│  │              │  │              │  │              │           │
│  │ • 金丝雀发布   │  │ • 版本路由   │  │ • 调用链追踪 │           │
│  │ • 蓝绿部署     │  │ • 向后兼容   │  │ • 性能指标   │           │
│  │ • A/B 测试     │  │ • 版本迁移   │  │ • 审计日志   │           │
│  │ • 流量镜像     │  │ • 版本废弃   │  │ • 异常告警   │           │
│  └──────────────┘  └─────────────┘  └─────────────┘           │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                    核心网关组件                                │ │
│  ├─────────────────────────────────────────────────────────────┤ │
│  │ • Spring Cloud Gateway / Envoy Gateway                      │ │
│  │ • Redis (限流、缓存)                                         │ │
│  │ • Prometheus (监控)                                          │ │
│  │ • Jaeger (链路追踪)                                          │ │
│  │ • Elasticsearch (日志)                                       │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 网关能力矩阵

| 功能模块 | 核心特性 | 技术实现 | 业务价值 |
|----------|----------|----------|----------|
| **动态路由** | 条件匹配、权重分配 | Spring Cloud Gateway | 灵活路由 |
| **流量控制** | 限流、熔断、降级 | Redis + Sentinel | 稳定性保障 |
| **安全防护** | 认证、授权、WAF | OAuth2 + JWT | 安全合规 |
| **灰度发布** | 金丝雀、AB 测试 | Gateway + Istio | 平滑升级 |
| **版本管理** | 多版本共存 | Header/Path 路由 | 向后兼容 |
| **监控审计** | 指标、日志、追踪 | Prometheus + ELK | 可观测性 |

---

## 🌊 动态路由规则

### 1. Spring Cloud Gateway 配置

```yaml
# application-gateway.yml
spring:
  cloud:
    gateway:
      routes:
      # 动态路由配置
      - id: user-service-route
        uri: lb://user-service
        predicates:
        - Path=/api/user/**
        - Header=X-Version, v1
        filters:
        - StripPrefix=2
        - name: RequestRateLimiter
          args:
            redis-rate-limiter.replenishRate: 10
            redis-rate-limiter.burstCapacity: 20
        - name: Retry
          args:
            retries: 3
            statuses: 500,502,503
            methods: GET,POST
            backoff:
              firstBackoff: 100ms
              maxBackoff: 1000ms
              factor: 2
              basedOnPreviousValue: true

      # 基于权重的路由
      - id: weight-route
        uri: lb://user-service
        predicates:
        - Path=/api/user/profile
        filters:
        - name: Weight
          args:
            weight: service-v1=80, service-v2=20

      # 基于 Host 的路由
      - id: host-route
        uri: lb://admin-service
        predicates:
        - Host=admin.**.com
        filters:
        - name: PrefixPath
          args:
            prefix: /admin

      # 基于 Method 的路由
      - id: method-route
        uri: lb://order-service
        predicates:
        - Method=POST
        filters:
        - name: RewritePath
          args:
            regexp: ^/api/(.*)
            replacement: /v1/$1

      # 基于 Header 的路由
      - id: header-route
        uri: lb://product-service
        predicates:
        - Header=X-User-Type, (admin|manager)
        filters:
        - name: AddRequestHeader
          args:
            X-Internal-Access: true

  # Redis 配置（用于限流）
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}
    database: 0
    timeout: 5000ms
    lettuce:
      pool:
        max-active: 8
        max-wait: -1ms
        max-idle: 8
        min-idle: 0
```

### 2. 动态路由配置类

```java
/**
 * 动态路由配置
 */
@Configuration
@EnableConfigurationProperties
public class DynamicRouteConfig {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ApplicationEventPublisher publisher;

    /**
     * 动态添加路由
     */
    public void addRoute(RouteDefinition routeDefinition) {
        try {
            // 验证路由配置
            validateRoute(routeDefinition);

            // 转换为 JSON 并存储到 Redis
            String json = JSON.toJSONString(routeDefinition);
            redisTemplate.opsForValue().set("gateway:route:" + routeDefinition.getId(), json);

            // 发送路由更新事件
            RouteUpdatedEvent event = new RouteUpdatedEvent(routeDefinition, RouteAction.ADD);
            publisher.publishEvent(event);

            log.info("路由添加成功: {}", routeDefinition.getId());
        } catch (Exception e) {
            log.error("路由添加失败", e);
            throw new RuntimeException("路由配置无效", e);
        }
    }

    /**
     * 动态更新路由
     */
    public void updateRoute(RouteDefinition routeDefinition) {
        removeRoute(routeDefinition.getId());
        addRoute(routeDefinition);
    }

    /**
     * 动态删除路由
     */
    public void removeRoute(String routeId) {
        redisTemplate.delete("gateway:route:" + routeId);

        RouteUpdatedEvent event = new RouteUpdatedEvent(null, RouteAction.REMOVE);
        event.setRouteId(routeId);
        publisher.publishEvent(event);

        log.info("路由删除成功: {}", routeId);
    }

    /**
     * 获取所有动态路由
     */
    public List<RouteDefinition> getAllRoutes() {
        Set<String> keys = redisTemplate.keys("gateway:route:*");
        List<RouteDefinition> routes = new ArrayList<>();

        for (String key : keys) {
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                RouteDefinition route = JSON.parseObject(json, RouteDefinition.class);
                routes.add(route);
            }
        }

        return routes;
    }

    /**
     * 条件路由匹配器
     */
    @Bean
    public RoutePredicateFactory<QueryRoutePredicateFactory.Config> queryRoutePredicateFactory() {
        return new QueryRoutePredicateFactory();
    }

    /**
     * 验证路由配置
     */
    private void validateRoute(RouteDefinition route) {
        if (StringUtils.isEmpty(route.getId())) {
            throw new IllegalArgumentException("路由 ID 不能为空");
        }

        if (route.getUri() == null) {
            throw new IllegalArgumentException("路由 URI 不能为空");
        }

        if (route.getPredicates() == null || route.getPredicates().isEmpty()) {
            throw new IllegalArgumentException("路由必须包含至少一个条件");
        }

        // 验证条件配置
        for (RoutePredicateDefinition predicate : route.getPredicates()) {
            if (StringUtils.isEmpty(predicate.getArgs()) || predicate.getArgs().isEmpty()) {
                throw new IllegalArgumentException("路由条件不能为空");
            }
        }
    }

    /**
     * 路由更新事件
     */
    public static class RouteUpdatedEvent extends ApplicationEvent {
        private final RouteDefinition routeDefinition;
        private final RouteAction action;
        private String routeId;

        public RouteUpdatedEvent(RouteDefinition source, RouteAction action) {
            super(source);
            this.routeDefinition = source;
            this.action = action;
        }

        // getters and setters
    }

    public enum RouteAction {
        ADD, UPDATE, REMOVE
    }
}

/**
 * 路由管理控制器
 */
@RestController
@RequestMapping("/api/admin/gateway/route")
@Api(tags = "路由管理")
@Validated
public class RouteController {

    @Autowired
    private DynamicRouteConfig routeConfig;

    /**
     * 添加路由
     */
    @PostMapping
    @PreAuthorize("hasAuthority('GATEWAY_ROUTE_WRITE')")
    @Log(value = "添加动态路由", level = LogLevel.WARN)
    public Result<Void> addRoute(@RequestBody @Valid RouteDefinition route) {
        routeConfig.addRoute(route);
        return Result.success();
    }

    /**
     * 更新路由
     */
    @PutMapping("/{routeId}")
    @PreAuthorize("hasAuthority('GATEWAY_ROUTE_WRITE')")
    @Log(value = "更新动态路由", level = LogLevel.WARN)
    public Result<Void> updateRoute(@PathVariable String routeId,
                                   @RequestBody @Valid RouteDefinition route) {
        route.setId(routeId);
        routeConfig.updateRoute(route);
        return Result.success();
    }

    /**
     * 删除路由
     */
    @DeleteMapping("/{routeId}")
    @PreAuthorize("hasAuthority('GATEWAY_ROUTE_WRITE')")
    @Log(value = "删除动态路由", level = LogLevel.WARN)
    public Result<Void> removeRoute(@PathVariable String routeId) {
        routeConfig.removeRoute(routeId);
        return Result.success();
    }

    /**
     * 查询所有路由
     */
    @GetMapping
    @PreAuthorize("hasAuthority('GATEWAY_ROUTE_READ')")
    public Result<List<RouteDefinition>> getAllRoutes() {
        List<RouteDefinition> routes = routeConfig.getAllRoutes();
        return Result.success(routes);
    }

    /**
     * 批量添加路由
     */
    @PostMapping("/batch")
    @PreAuthorize("hasAuthority('GATEWAY_ROUTE_WRITE')")
    @Log(value = "批量添加动态路由", level = LogLevel.WARN)
    public Result<Void> batchAddRoutes(@RequestBody List<RouteDefinition> routes) {
        routes.forEach(routeConfig::addRoute);
        return Result.success();
    }
}
```

### 3. 自定义路由谓词工厂

```java
/**
 * 基于权重的路由谓词工厂
 */
@Component
public class WeightRoutePredicateFactory
    extends AbstractRoutePredicateFactory<WeightRoutePredicateFactory.Config> {

    public WeightRoutePredicateFactory() {
        super(Config.class);
    }

    @Override
    public Predicate<ServerWebExchange> apply(Config config) {
        // 获取权重配置
        Map<String, Integer> weights = config.getWeights();
        int totalWeight = weights.values().stream().mapToInt(Integer::intValue).sum();

        return exchange -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();

            // 根据权重分配路由
            for (Map.Entry<String, Integer> entry : weights.entrySet()) {
                String serviceName = entry.getKey();
                int weight = entry.getValue();

                // 基于 Service Name 匹配
                if (path.contains(serviceName)) {
                    double ratio = (double) weight / totalWeight;
                    double random = Math.random();

                    if (random < ratio) {
                        log.debug("权重路由匹配: service={}, path={}, ratio={}",
                            serviceName, path, ratio);
                        return true;
                    }
                }
            }

            return false;
        };
    }

    @Data
    public static class Config {
        private Map<String, Integer> weights;
    }
}

/**
 * 基于用户类型的路由谓词工厂
 */
@Component
public class UserTypeRoutePredicateFactory
    extends AbstractRoutePredicateFactory<UserTypeRoutePredicateFactory.Config> {

    @Override
    public Predicate<ServerWebExchange> apply(Config config) {
        Set<String> allowedTypes = config.getUserTypes();

        return exchange -> {
            ServerHttpRequest request = exchange.getRequest();

            // 从 Header 获取用户类型
            String userType = request.getHeaders().getFirst("X-User-Type");
            if (userType == null) {
                userType = "anonymous";
            }

            // 从 JWT Token 获取用户类型
            if (allowedTypes.contains(userType)) {
                log.debug("用户类型路由匹配: userType={}, path={}",
                    userType, request.getURI().getPath());
                return true;
            }

            return false;
        };
    }

    @Data
    public static class Config {
        private Set<String> userTypes;
    }
}

/**
 * 基于地理位置的路由谓词工厂
 */
@Component
public class GeoRoutePredicateFactory
    extends AbstractRoutePredicateFactory<GeoRoutePredicateFactory.Config> {

    @Autowired
    private IpGeoLocationService geoLocationService;

    @Override
    public Predicate<ServerWebExchange> apply(Config config) {
        Set<String> allowedCountries = config.getAllowedCountries();

        return exchange -> {
            ServerHttpRequest request = exchange.getRequest();
            String clientIp = getClientIp(request);

            // 获取地理位置
            GeoLocation geo = geoLocationService.getLocation(clientIp);
            if (geo != null && allowedCountries.contains(geo.getCountry())) {
                log.debug("地理位置路由匹配: ip={}, country={}",
                    clientIp, geo.getCountry());
                return true;
            }

            return false;
        };
    }

    private String getClientIp(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddress().getAddress().getHostAddress();
    }

    @Data
    public static class Config {
        private Set<String> allowedCountries;
    }
}
```

---

## ⚡ 流量控制

### 1. 限流配置

```java
/**
 * 限流配置
 */
@Configuration
public class RateLimitConfig {

    /**
     * Redis 令牌桶限流器
     */
    @Bean
    public RedisRateLimiter rateLimiter() {
        return new RedisRateLimiter(10, 20); // replenishRate, burstCapacity
    }

    /**
     * 自定义限流算法
     */
    @Bean
    public RateLimiter customRateLimiter() {
        return new SlidingWindowRateLimiter();
    }

    /**
     * 限流策略配置
     */
    @Bean
    public Map<String, RateLimitStrategy> rateLimitStrategies() {
        Map<String, RateLimitStrategy> strategies = new HashMap<>();

        // IP 限流
        strategies.put("ip",
            new IpRateLimitStrategy(100, 1000)); // 100 req/s, burst 1000

        // 用户限流
        strategies.put("user",
            new UserRateLimitStrategy(50, 500)); // 50 req/s, burst 500

        // API 限流
        strategies.put("api",
            new ApiRateLimitStrategy(200, 2000)); // 200 req/s, burst 2000

        return strategies;
    }
}

/**
 * 限流过滤器
 */
@Component
public class RateLimitFilter implements GatewayFilter {

    @Autowired
    private RedisRateLimiter rateLimiter;

    @Autowired
    private RateLimitDecisionService decisionService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String clientIp = getClientIp(request);

        // 获取用户信息
        String userId = getUserId(request);

        // 判断是否需要限流
        RateLimitDecision decision = decisionService.shouldLimit(path, clientIp, userId);

        if (decision.isLimited()) {
            log.warn("请求被限流: path={}, clientIp={}, userId={}, strategy={}",
                path, clientIp, userId, decision.getStrategy());

            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            response.getHeaders().add("X-RateLimit-Limit", String.valueOf(decision.getLimit()));
            response.getHeaders().add("X-RateLimit-Remaining", "0");
            response.getHeaders().add("X-RateLimit-Reset",
                String.valueOf(System.currentTimeMillis() / 1000 + decision.getResetTime()));

            Map<String, Object> result = new HashMap<>();
            result.put("code", 429);
            result.put("message", "请求过于频繁，请稍后再试");
            result.put("retryAfter", decision.getResetTime());

            return response.writeWith(
                Mono.just(response.bufferFactory().wrap(JSON.toJSONString(result).getBytes()))
            );
        }

        return chain.filter(exchange);
    }

    private String getClientIp(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddress().getAddress().getHostAddress();
    }

    private String getUserId(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                return JWTUtil.getUserIdFromToken(token);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
}

/**
 * 限流决策服务
 */
@Service
public class RateLimitDecisionService {

    @Autowired
    private Map<String, RateLimitStrategy> strategies;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 判断是否需要限流
     */
    public RateLimitDecision shouldLimit(String path, String clientIp, String userId) {
        // 1. 检查白名单
        if (isWhitelisted(clientIp, path)) {
            return RateLimitDecision.noLimit();
        }

        // 2. 动态策略选择
        RateLimitStrategy strategy = selectStrategy(path, userId);

        // 3. 执行限流检查
        return checkLimit(strategy, path, clientIp, userId);
    }

    private RateLimitStrategy selectStrategy(String path, String userId) {
        // 优先级：用户限流 > API 限流 > IP 限流
        if (StringUtils.hasText(userId) && strategies.containsKey("user")) {
            return strategies.get("user");
        }

        if (path.startsWith("/api/admin") && strategies.containsKey("api")) {
            return strategies.get("api");
        }

        return strategies.get("ip");
    }

    private RateLimitDecision checkLimit(RateLimitStrategy strategy,
                                        String path, String clientIp, String userId) {
        String key = buildLimitKey(strategy, path, clientIp, userId);

        // 获取当前请求计数
        Long current = redisTemplate.opsForValue().increment(key);

        if (current == 1) {
            // 首次请求，设置过期时间
            redisTemplate.expire(key, strategy.getWindowSize(), TimeUnit.SECONDS);
        }

        if (current > strategy.getLimit()) {
            // 超出限制
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            return RateLimitDecision.limited(strategy, ttl);
        }

        return RateLimitDecision.noLimit();
    }

    private String buildLimitKey(RateLimitStrategy strategy,
                                String path, String clientIp, String userId) {
        StringBuilder key = new StringBuilder();
        key.append("rate_limit:").append(strategy.getType()).append(":");

        if (StringUtils.hasText(userId)) {
            key.append("user:").append(userId);
        } else {
            key.append("ip:").append(clientIp);
        }

        return key.toString();
    }

    private boolean isWhitelisted(String clientIp, String path) {
        // 检查 IP 白名单
        Set<String> whitelist = redisTemplate.opsForSet().members("whitelist:ip");
        if (whitelist != null && whitelist.contains(clientIp)) {
            return true;
        }

        // 检查路径白名单
        Set<String> pathWhitelist = redisTemplate.opsForSet().members("whitelist:path");
        if (pathWhitelist != null) {
            return pathWhitelist.stream().anyMatch(path::startsWith);
        }

        return false;
    }
}

/**
 * 限流策略
 */
public interface RateLimitStrategy {
    String getType();
    int getLimit();
    int getBurst();
    int getWindowSize();
    boolean isExceeded(long current);
}

/**
 * IP 限流策略
 */
public class IpRateLimitStrategy implements RateLimitStrategy {
    private final int limit;
    private final int burst;
    private final int windowSize;

    @Override
    public String getType() {
        return "ip";
    }

    // ... implementation
}

/**
 * 限流决策结果
 */
@Data
@Builder
public class RateLimitDecision {
    private boolean limited;
    private RateLimitStrategy strategy;
    private Long resetTime;
    private String reason;

    public static RateLimitDecision noLimit() {
        return RateLimitDecision.builder()
            .limited(false)
            .build();
    }

    public static RateLimitDecision limited(RateLimitStrategy strategy, Long resetTime) {
        return RateLimitDecision.builder()
            .limited(true)
            .strategy(strategy)
            .resetTime(resetTime)
            .reason("请求频率超过限制")
            .build();
    }
}
```

### 2. 熔断降级配置

```java
/**
 * 熔断器配置
 */
@Configuration
public class CircuitBreakerConfig {

    /**
     * 熔断器工厂
     */
    @Bean
    public Customizer<CircuitBreakerRegistry> circuitBreakerRegistry() {
        return registry -> {
            // 用户服务熔断器
            Customizer<Resilience4JCircuitBreakerFactory> userServiceBreaker =
                factory -> factory.configureBreaker(
                    CircuitBreaker.ofDefaults("user-service"),
                    "fallbackUserService"
                );

            // 订单服务熔断器
            Customizer<Resilience4JCircuitBreakerFactory> orderServiceBreaker =
                factory -> factory.configureBreaker(
                    CircuitBreaker.ofDefaults("order-service"),
                    "fallbackOrderService"
                );
        };
    }

    /**
     * 熔断器配置
     */
    @Bean
    public Resilience4JCircuitBreakerFactory circuitBreakerFactory() {
        CircuitBreakerConfig circuitBreakerConfig = new CircuitBreakerConfig();
        TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.ofDefaults();

        return new Resilience4JCircuitBreakerFactory(
            circuitBreakerConfig, timeLimiterConfig,
            new DefaultClockSupplier());
    }

    /**
     * 重试配置
     */
    @Bean
    public RetryConfig<User> retryConfig() {
        return RetryConfig.<User>builder()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(500))
            .exponentialBackoffMultiplier(2)
            .retryExceptions(ConnectTimeoutException.class,
                SocketTimeoutException.class,
                TransientDataAccessException.class)
            .build();
    }

    /**
     * 降级方法
     */
    @Bean
    public Function<Throwable, String> fallbackUserService() {
        return throwable -> {
            log.error("用户服务降级", throwable);
            return "服务暂时不可用，请稍后重试";
        };
    }

    @Bean
    public Function<Throwable, String> fallbackOrderService() {
        return throwable -> {
            log.error("订单服务降级", throwable);
            return "订单处理失败，请稍后重试";
        };
    }
}

/**
 * 熔断器网关过滤器
 */
@Component
public class CircuitBreakerFilter implements GatewayFilter {

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private TimeLimiter timeLimiter;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String serviceName = extractServiceName(path);

        if (serviceName == null) {
            return chain.filter(exchange);
        }

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(serviceName);

        return Mono.fromCallable(() -> {
                    // 调用服务
                    return WebClient.builder()
                        .build()
                        .method(HttpMethod.GET)
                        .uri("http://" + serviceName + path)
                        .retrieve()
                        .bodyToMono(String.class);
                })
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .transformDeferred(timeout(timeLimiter))
                .onErrorResume(throwable -> {
                    log.error("服务调用失败，触发降级: {}", serviceName, throwable);
                    return handleFallback(exchange, serviceName, throwable);
                })
                .onErrorResume(CircuitBreakerOpenException.class, throwable -> {
                    log.warn("熔断器打开，返回降级响应: {}", serviceName);
                    return handleFallback(exchange, serviceName, throwable);
                })
                .flatMap(response -> chain.filter(exchange));
    }

    private String extractServiceName(String path) {
        String[] parts = path.split("/");
        if (parts.length >= 3 && "api".equals(parts[1])) {
            return parts[2];
        }
        return null;
    }

    private Mono<Void> handleFallback(ServerWebExchange exchange,
                                      String serviceName,
                                      Throwable throwable) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);

        Map<String, Object> fallback = new HashMap<>();
        fallback.put("code", 503);
        fallback.put("message", "服务 " + serviceName + " 暂时不可用");
        fallback.put("error", "Circuit Breaker Open");
        fallback.put("timestamp", System.currentTimeMillis());

        String body = JSON.toJSONString(fallback);
        response.getHeaders().add("Content-Type", "application/json");

        return response.writeWith(
            Mono.just(response.bufferFactory().wrap(body.getBytes()))
        );
    }

    private <T> Publisher<T> timeout(TimeLimiter timeLimiter) {
        return publisher -> timeLimiter.executeCompletionStage(
            Schedulers.boundedElastic(),
            () -> CompletableFuture.supplyAsync(() -> publisher)
        ).toCompletableFuture();
    }
}
```

---

## 🎯 灰度发布

### 1. 金丝雀发布配置

```yaml
# canary-routing.yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: user-service-canary
spec:
  hosts:
  - user-service
  http:
  # 金丝雀路由 - 基于 Header
  - match:
    - headers:
        x-canary:
          exact: "true"
    route:
    - destination:
        host: user-service
        subset: v2
  # 金丝雀路由 - 基于 Cookie
  - match:
    - headers:
        cookie:
          regex: ".*ab_test=v2.*"
    route:
    - destination:
        host: user-service
        subset: v2
  # 金丝雀路由 - 基于用户 ID
  - match:
    - headers:
        x-user-id:
          regex: "^[1-9][0-9]*"  # 只对新用户（ID 从 100000 开始）路由
    route:
    - destination:
        host: user-service
        subset: v2
  # 百分比流量分配
  - route:
    - destination:
        host: user-service
        subset: v1
      weight: 90
    - destination:
        host: user-service
        subset: v2
      weight: 10

---
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: user-service
spec:
  host: user-service
  subsets:
  - name: v1
    labels:
      version: v1
  - name: v2
    labels:
      version: v2
```

### 2. 网关金丝雀发布实现

```java
/**
 * 金丝雀发布控制器
 */
@RestController
@RequestMapping("/api/admin/canary")
@Api(tags = "金丝雀发布")
public class CanaryReleaseController {

    @Autowired
    private CanaryReleaseService canaryService;

    /**
     * 创建金丝雀发布策略
     */
    @PostMapping("/strategy")
    @PreAuthorize("hasAuthority('CANARY_RELEASE_WRITE')")
    @Log(value = "创建金丝雀发布策略", level = LogLevel.WARN)
    public Result<Void> createStrategy(@RequestBody @Valid CanaryReleaseStrategyRequest request) {
        canaryService.createStrategy(request);
        return Result.success();
    }

    /**
     * 更新流量分配
     */
    @PutMapping("/{strategyId}/traffic")
    @PreAuthorize("hasAuthority('CANARY_RELEASE_WRITE')")
    public Result<Void> updateTraffic(@PathVariable String strategyId,
                                      @RequestBody @Valid TrafficDistributionRequest request) {
        canaryService.updateTrafficDistribution(strategyId, request);
        return Result.success();
    }

    /**
     * 获取发布状态
     */
    @GetMapping("/{strategyId}/status")
    @PreAuthorize("hasAuthority('CANARY_RELEASE_READ')")
    public Result<CanaryReleaseStatus> getStatus(@PathVariable String strategyId) {
        CanaryReleaseStatus status = canaryService.getReleaseStatus(strategyId);
        return Result.success(status);
    }

    /**
     * 取消金丝雀发布
     */
    @PostMapping("/{strategyId}/cancel")
    @PreAuthorize("hasAuthority('CANARY_RELEASE_WRITE')")
    public Result<Void> cancel(@PathVariable String strategyId) {
        canaryService.cancelCanaryRelease(strategyId);
        return Result.success();
    }

    /**
     * 完成金丝雀发布
     */
    @PostMapping("/{strategyId}/complete")
    @PreAuthorize("hasAuthority('CANARY_RELEASE_WRITE')")
    public Result<Void> complete(@PathVariable String strategyId) {
        canaryService.completeCanaryRelease(strategyId);
        return Result.success();
    }
}

/**
 * 金丝雀发布服务
 */
@Service
public class CanaryReleaseService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private CanaryReleaseMetrics metrics;

    /**
     * 创建金丝雀发布策略
     */
    @Transactional
    public void createStrategy(CanaryReleaseStrategyRequest request) {
        // 1. 创建策略对象
        CanaryReleaseStrategy strategy = new CanaryReleaseStrategy();
        strategy.setId(UUID.randomUUID().toString());
        strategy.setServiceName(request.getServiceName());
        strategy.setVersion(request.getVersion());
        strategy.setStrategyType(request.getStrategyType());
        strategy.setTrafficAllocation(request.getTrafficAllocation());
        strategy.setStatus(CanaryStatus.PENDING);
        strategy.setCreateTime(new Date());

        // 2. 保存到 Redis
        String key = "canary:strategy:" + strategy.getId();
        redisTemplate.opsForValue().set(key, JSON.toJSONString(strategy));

        // 3. 应用路由规则
        applyRoutingRules(strategy);

        // 4. 更新服务状态
        updateServiceStatus(request.getServiceName(), CanaryStatus.CANARY_RUNNING);

        log.info("金丝雀发布策略创建成功: {}", strategy.getId());
    }

    /**
     * 更新流量分配
     */
    public void updateTrafficDistribution(String strategyId,
                                         TrafficDistributionRequest request) {
        String key = "canary:strategy:" + strategyId;
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            throw new BusinessException("策略不存在");
        }

        CanaryReleaseStrategy strategy = JSON.parseObject(json, CanaryReleaseStrategy.class);

        // 验证流量分配
        if (request.getTrafficAllocation().values().stream().mapToInt(Integer::intValue).sum() != 100) {
            throw new BusinessException("流量分配总和必须为 100%");
        }

        strategy.setTrafficAllocation(request.getTrafficAllocation());
        strategy.setUpdateTime(new Date());

        // 保存更新
        redisTemplate.opsForValue().set(key, JSON.toJSONString(strategy));

        // 应用新的流量分配
        applyTrafficDistribution(strategy);

        log.info("流量分配已更新: strategyId={}", strategyId);
    }

    /**
     * 获取发布状态
     */
    public CanaryReleaseStatus getReleaseStatus(String strategyId) {
        String key = "canary:strategy:" + strategyId;
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            throw new BusinessException("策略不存在");
        }

        CanaryReleaseStrategy strategy = JSON.parseObject(json, CanaryReleaseStrategy.class);

        // 获取指标数据
        Map<String, Object> metrics = metrics.collectMetrics(strategy);

        // 计算成功率
        double successRate = calculateSuccessRate(metrics);

        // 判断是否需要自动推进
        boolean shouldPromote = checkAutoPromotion(strategy, metrics, successRate);

        if (shouldPromote) {
            // 自动推进流量
            autoPromoteTraffic(strategy);
        }

        return CanaryReleaseStatus.builder()
            .strategyId(strategyId)
            .status(strategy.getStatus())
            .trafficAllocation(strategy.getTrafficAllocation())
            .metrics(metrics)
            .successRate(successRate)
            .build();
    }

    private void applyRoutingRules(CanaryReleaseStrategy strategy) {
        // 根据策略类型应用不同的路由规则
        switch (strategy.getStrategyType()) {
            case HEADER:
                applyHeaderRouting(strategy);
                break;
            case COOKIE:
                applyCookieRouting(strategy);
                break;
            case USER_ID:
                applyUserIdRouting(strategy);
                break;
            case PERCENTAGE:
                applyPercentageRouting(strategy);
                break;
        }
    }

    private void applyHeaderRouting(CanaryReleaseStrategy strategy) {
        // 实现基于 Header 的路由
    }

    private void applyCookieRouting(CanaryReleaseStrategy strategy) {
        // 实现基于 Cookie 的路由
    }

    private void applyUserIdRouting(CanaryReleaseStrategy strategy) {
        // 实现基于用户 ID 的路由
    }

    private void applyPercentageRouting(CanaryReleaseStrategy strategy) {
        // 实现基于百分比的路由
    }

    private double calculateSuccessRate(Map<String, Object> metrics) {
        // 从指标中计算成功率
        Long totalRequests = (Long) metrics.get("totalRequests");
        Long successfulRequests = (Long) metrics.get("successfulRequests");

        if (totalRequests == null || totalRequests == 0) {
            return 100.0;
        }

        return (double) successfulRequests / totalRequests * 100;
    }

    private boolean checkAutoPromotion(CanaryReleaseStrategy strategy,
                                      Map<String, Object> metrics,
                                      double successRate) {
        // 检查是否满足自动推进条件
        // 例如：成功率 > 99%，错误率 < 0.1%，延迟 < 100ms
        return successRate > 99.0 &&
               getErrorRate(metrics) < 0.1 &&
               getAverageLatency(metrics) < 100;
    }

    private double getErrorRate(Map<String, Object> metrics) {
        // 计算错误率
        return 0.0;
    }

    private double getAverageLatency(Map<String, Object> metrics) {
        // 计算平均延迟
        return 0.0;
    }

    private void autoPromoteTraffic(CanaryReleaseStrategy strategy) {
        // 自动推进流量，例如从 10% -> 50%
        Map<Integer, Integer> currentAllocation = strategy.getTrafficAllocation();
        Map<Integer, Integer> newAllocation = new HashMap<>(currentAllocation);

        // 增加金丝雀流量
        Integer currentCanary = newAllocation.get(2);
        if (currentCanary < 50) {
            newAllocation.put(2, currentCanary + 10);
            newAllocation.put(1, currentAllocation.get(1) - 10);
        }

        strategy.setTrafficAllocation(newAllocation);
        strategy.setUpdateTime(new Date());

        // 保存更新
        String key = "canary:strategy:" + strategy.getId();
        redisTemplate.opsForValue().set(key, JSON.toJSONString(strategy));

        log.info("自动推进流量: strategyId={}, allocation={}", strategy.getId(), newAllocation);
    }

    /**
     * 完成金丝雀发布
     */
    public void completeCanaryRelease(String strategyId) {
        String key = "canary:strategy:" + strategyId;
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            throw new BusinessException("策略不存在");
        }

        CanaryReleaseStrategy strategy = JSON.parseObject(json, CanaryReleaseStrategy.class);
        strategy.setStatus(CanaryStatus.COMPLETED);
        strategy.setCompleteTime(new Date());

        // 将所有流量切换到新版本
        applyAllTrafficToNewVersion(strategy);

        // 保存更新
        redisTemplate.opsForValue().set(key, JSON.toJSONString(strategy));

        log.info("金丝雀发布完成: {}", strategyId);
    }

    private void applyAllTrafficToNewVersion(CanaryReleaseStrategy strategy) {
        // 将所有流量路由到新版本
    }

    /**
     * 取消金丝雀发布
     */
    public void cancelCanaryRelease(String strategyId) {
        String key = "canary:strategy:" + strategyId;
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            throw new BusinessException("策略不存在");
        }

        CanaryReleaseStrategy strategy = JSON.parseObject(json, CanaryReleaseStrategy.class);
        strategy.setStatus(CanaryStatus.CANCELLED);
        strategy.setCancelTime(new Date());

        // 恢复所有流量到旧版本
        applyAllTrafficToOldVersion(strategy);

        redisTemplate.opsForValue().set(key, JSON.toJSONString(strategy));

        log.info("金丝雀发布已取消: {}", strategyId);
    }

    private void applyAllTrafficToOldVersion(CanaryReleaseStrategy strategy) {
        // 将所有流量路由回旧版本
    }

    private void updateServiceStatus(String serviceName, CanaryStatus status) {
        redisTemplate.opsForValue().set("service:status:" + serviceName, status.name());
    }
}
```

### 3. 蓝绿部署实现

```java
/**
 * 蓝绿部署控制器
 */
@RestController
@RequestMapping("/api/admin/bluegreen")
@Api(tags = "蓝绿部署")
public class BlueGreenDeploymentController {

    @Autowired
    private BlueGreenDeploymentService blueGreenService;

    /**
     * 创建蓝绿部署
     */
    @PostMapping
    @PreAuthorize("hasAuthority('BLUEGREEN_WRITE')")
    public Result<Void> create(@RequestBody @Valid BlueGreenRequest request) {
        blueGreenService.createBlueGreenDeployment(request);
        return Result.success();
    }

    /**
     * 执行蓝绿切换
     */
    @PostMapping("/{deploymentId}/switch")
    @PreAuthorize("hasAuthority('BLUEGREEN_WRITE')")
    public Result<Void> switchTraffic(@PathVariable String deploymentId) {
        blueGreenService.switchTraffic(deploymentId);
        return Result.success();
    }

    /**
     * 回滚蓝绿部署
     */
    @PostMapping("/{deploymentId}/rollback")
    @PreAuthorize("hasAuthority('BLUEGREEN_WRITE')")
    public Result<Void> rollback(@PathVariable String deploymentId) {
        blueGreenService.rollback(deploymentId);
        return Result.success();
    }
}

/**
 * 蓝绿部署服务
 */
@Service
public class BlueGreenDeploymentService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 创建蓝绿部署
     */
    public void createBlueGreenDeployment(BlueGreenRequest request) {
        // 1. 创建部署对象
        BlueGreenDeployment deployment = BlueGreenDeployment.builder()
            .id(UUID.randomUUID().toString())
            .serviceName(request.getServiceName())
            .blueVersion(request.getBlueVersion())
            .greenVersion(request.getGreenVersion())
            .status(BlueGreenStatus.CREATED)
            .createTime(new Date())
            .build();

        // 2. 保存部署信息
        String key = "bluegreen:deployment:" + deployment.getId();
        redisTemplate.opsForValue().set(key, JSON.toJSONString(deployment));

        // 3. 部署服务
        deployService(deployment, "blue");
        deployService(deployment, "green");

        log.info("蓝绿部署创建成功: {}", deployment.getId());
    }

    /**
     * 执行流量切换
     */
    @Transactional
    public void switchTraffic(String deploymentId) {
        String key = "bluegreen:deployment:" + deploymentId;
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            throw new BusinessException("部署不存在");
        }

        BlueGreenDeployment deployment = JSON.parseObject(json, BlueGreenDeployment.class);

        if (deployment.getStatus() != BlueGreenStatus.READY) {
            throw new BusinessException("部署未准备就绪");
        }

        // 获取当前活跃环境
        String activeEnv = getActiveEnvironment(deployment.getServiceName());

        // 切换到另一个环境
        String targetEnv = "blue".equals(activeEnv) ? "green" : "blue";

        // 执行切换
        switchTraffic(deployment, targetEnv);

        // 更新状态
        deployment.setActiveEnvironment(targetEnv);
        deployment.setStatus(BlueGreenStatus.SWITCHED);
        deployment.setSwitchTime(new Date());

        redisTemplate.opsForValue().set(key, JSON.toJSONString(deployment));

        // 记录审计日志
        auditLogService.recordBlueGreenSwitch(deployment, activeEnv, targetEnv);

        log.info("蓝绿流量切换完成: deploymentId={}, from={}, to={}",
            deploymentId, activeEnv, targetEnv);
    }

    private void switchTraffic(BlueGreenDeployment deployment, String targetEnv) {
        // 1. 更新路由规则
        updateRoutingRules(deployment.getServiceName(), targetEnv);

        // 2. 预热服务
        warmupService(deployment.getServiceName(), targetEnv);

        // 3. 健康检查
        if (!performHealthCheck(deployment.getServiceName(), targetEnv)) {
            throw new BusinessException("健康检查失败，无法切换流量");
        }
    }

    /**
     * 回滚
     */
    public void rollback(String deploymentId) {
        String key = "bluegreen:deployment:" + deploymentId;
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            throw new BusinessException("部署不存在");
        }

        BlueGreenDeployment deployment = JSON.parseObject(json, BlueGreenDeployment.class);

        // 切换回原来的环境
        String originalEnv = "blue".equals(deployment.getActiveEnvironment()) ? "green" : "blue";

        switchTraffic(deployment, originalEnv);

        deployment.setStatus(BlueGreenStatus.ROLLED_BACK);
        deployment.setRollbackTime(new Date());

        redisTemplate.opsForValue().set(key, JSON.toJSONString(deployment));

        log.info("蓝绿部署已回滚: {}", deploymentId);
    }

    private String getActiveEnvironment(String serviceName) {
        return redisTemplate.opsForValue().get("service:active:" + serviceName) != null
            ? redisTemplate.opsForValue().get("service:active:" + serviceName)
            : "blue";
    }

    private void deployService(BlueGreenDeployment deployment, String env) {
        // 部署服务到指定环境
    }

    private void updateRoutingRules(String serviceName, String targetEnv) {
        // 更新路由规则，将流量路由到目标环境
    }

    private void warmupService(String serviceName, String env) {
        // 预热服务
    }

    private boolean performHealthCheck(String serviceName, String env) {
        // 执行健康检查
        return true;
    }

    @Data
    @Builder
    public static class BlueGreenDeployment {
        private String id;
        private String serviceName;
        private String blueVersion;
        private String greenVersion;
        private String activeEnvironment;
        private BlueGreenStatus status;
        private Date createTime;
        private Date switchTime;
        private Date rollbackTime;
    }

    public enum BlueGreenStatus {
        CREATED, DEPLOYING, READY, SWITCHED, ROLLED_BACK, COMPLETED
    }
}
```

---

## 📦 API 版本管理

### 1. 版本路由配置

```java
/**
 * API 版本路由配置
 */
@Configuration
public class ApiVersionRoutingConfig {

    /**
     * 基于 Header 的版本路由
     */
    @Bean
    public RoutePredicateFactory<HeaderRoutePredicateFactory.Config> headerRoutePredicateFactory() {
        return new HeaderRoutePredicateFactory();
    }

    /**
     * 基于 Path 的版本路由
     */
    @Bean
    public RoutePredicateFactory<PathRoutePredicateFactory.Config> pathRoutePredicateFactory() {
        return new PathRoutePredicateFactory();
    }

    /**
     * 基于 Query 的版本路由
     */
    @Bean
    public RoutePredicateFactory<QueryRoutePredicateFactory.Config> queryRoutePredicateFactory() {
        return new QueryRoutePredicateFactory();
    }

    /**
     * 版本管理配置
     */
    @Bean
    public VersionRoutingConfig versionRoutingConfig() {
        return VersionRoutingConfig.builder()
            .defaultVersion("v1")
            .versionHeader("X-API-Version")
            .versionPathPrefix(true)
            .compatibilityCheckEnabled(true)
            .build();
    }

    /**
     * 版本路由器
     */
    @Bean
    @Primary
    public VersionRouter versionRouter() {
        return new VersionRouter(versionRoutingConfig());
    }
}

/**
 * API 版本路由器
 */
@Component
public class VersionRouter {

    private final VersionRoutingConfig config;

    public VersionRouter(VersionRoutingConfig config) {
        this.config = config;
    }

    /**
     * 路由到指定版本
     */
    public String route(String path, String version) {
        // 1. 移除版本前缀
        String cleanPath = removeVersionPrefix(path);

        // 2. 获取目标服务
        String targetService = resolveTargetService(cleanPath, version);

        // 3. 构建目标路径
        String targetPath = buildTargetPath(targetService, cleanPath, version);

        return "lb://" + targetService;
    }

    /**
     * 解析版本号
     */
    public String resolveVersion(ServerHttpRequest request) {
        // 1. 从 Header 获取版本
        String version = request.getHeaders().getFirst(config.getVersionHeader());
        if (StringUtils.hasText(version)) {
            return version;
        }

        // 2. 从路径获取版本
        String path = request.getURI().getPath();
        version = extractVersionFromPath(path);
        if (StringUtils.hasText(version)) {
            return version;
        }

        // 3. 从 Query 参数获取版本
        MultiValueMap<String, String> queryParams = request.getQueryParams();
        version = queryParams.getFirst("version");
        if (StringUtils.hasText(version)) {
            return version;
        }

        // 4. 返回默认版本
        return config.getDefaultVersion();
    }

    /**
     * 版本兼容性检查
     */
    public CompatibilityResult checkCompatibility(String apiPath,
                                                  String clientVersion,
                                                  String serverVersion) {
        // 获取 API 定义
        ApiDefinition api = getApiDefinition(apiPath);
        if (api == null) {
            return CompatibilityResult.incompatible("API 未定义");
        }

        // 检查版本兼容性
        VersionCompatibility compatibility = api.getCompatibility(clientVersion, serverVersion);

        if (!compatibility.isCompatible()) {
            return CompatibilityResult.incompatible(compatibility.getMessage());
        }

        return CompatibilityResult.compatible();
    }

    private String removeVersionPrefix(String path) {
        if (!config.isVersionPathPrefix()) {
            return path;
        }

        Matcher matcher = Pattern.compile("/v\\d+/").matcher(path);
        if (matcher.find()) {
            return path.replaceFirst("/v\\d+/", "/");
        }

        return path;
    }

    private String extractVersionFromPath(String path) {
        Matcher matcher = Pattern.compile("/v(\\d+)/").matcher(path);
        if (matcher.find()) {
            return "v" + matcher.group(1);
        }
        return null;
    }

    private String resolveTargetService(String path, String version) {
        // 根据路径和版本解析目标服务
        String serviceName = extractServiceName(path);
        return serviceName + "-" + version;
    }

    private String extractServiceName(String path) {
        String[] parts = path.split("/");
        return parts.length >= 2 ? parts[1] : "default";
    }

    private String buildTargetService(String cleanPath, String version) {
        String serviceName = extractServiceName(cleanPath);
        return serviceName + "-" + version;
    }

    private String buildTargetPath(String targetService, String cleanPath, String version) {
        return cleanPath;
    }

    private ApiDefinition getApiDefinition(String apiPath) {
        // 从配置中心获取 API 定义
        return null;
    }

    @Data
    @Builder
    public static class VersionRoutingConfig {
        private String defaultVersion;
        private String versionHeader;
        private boolean versionPathPrefix;
        private boolean compatibilityCheckEnabled;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CompatibilityResult {
        private boolean compatible;
        private String message;

        public static CompatibilityResult compatible() {
            return new CompatibilityResult(true, null);
        }

        public static CompatibilityResult incompatible(String message) {
            return new CompatibilityResult(false, message);
        }
    }
}

/**
 * 版本管理控制器
 */
@RestController
@RequestMapping("/api/admin/version")
@Api(tags = "API 版本管理")
public class ApiVersionController {

    @Autowired
    private ApiVersionService versionService;

    /**
     * 注册 API 版本
     */
    @PostMapping
    @PreAuthorize("hasAuthority('VERSION_MANAGEMENT_WRITE')")
    @Log(value = "注册 API 版本", level = LogLevel.WARN)
    public Result<Void> registerVersion(@RequestBody @Valid ApiVersionRequest request) {
        versionService.registerVersion(request);
        return Result.success();
    }

    /**
     * 废弃 API 版本
     */
    @PostMapping("/{apiName}/{version}/deprecate")
    @PreAuthorize("hasAuthority('VERSION_MANAGEMENT_WRITE')")
    public Result<Void> deprecateVersion(@PathVariable String apiName,
                                        @PathVariable String version,
                                        @RequestParam(required = false) Date deprecationDate) {
        versionService.deprecateVersion(apiName, version, deprecationDate);
        return Result.success();
    }

    /**
     * 强制使用特定版本
     */
    @PostMapping("/{apiName}/force")
    @PreAuthorize("hasAuthority('VERSION_MANAGEMENT_WRITE')")
    public Result<Void> forceVersion(@PathVariable String apiName,
                                    @RequestParam String version) {
        versionService.forceVersion(apiName, version);
        return Result.success();
    }

    /**
     * 获取可用的 API 版本
     */
    @GetMapping("/{apiName}/versions")
    @PreAuthorize("hasAuthority('VERSION_MANAGEMENT_READ')")
    public Result<List<ApiVersionInfo>> getVersions(@PathVariable String apiName) {
        List<ApiVersionInfo> versions = versionService.getAvailableVersions(apiName);
        return Result.success(versions);
    }

    /**
     * 版本迁移助手
     */
    @PostMapping("/{apiName}/{version}/migrate")
    public Result<String> generateMigrationGuide(@PathVariable String apiName,
                                                @PathVariable String version) {
        String guide = versionService.generateMigrationGuide(apiName, version);
        return Result.success(guide);
    }
}

/**
 * API 版本服务
 */
@Service
public class ApiVersionService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ApiDefinitionRegistry apiRegistry;

    /**
     * 注册 API 版本
     */
    public void registerVersion(ApiVersionRequest request) {
        ApiVersion version = ApiVersion.builder()
            .apiName(request.getApiName())
            .version(request.getVersion())
            .status(ApiVersionStatus.ACTIVE)
            .createTime(new Date())
            .build();

        String key = "api:version:" + request.getApiName() + ":" + request.getVersion();
        redisTemplate.opsForValue().set(key, JSON.toJSONString(version));

        // 更新 API 版本列表
        updateVersionList(request.getApiName(), version);

        log.info("API 版本注册成功: {} v{}", request.getApiName(), request.getVersion());
    }

    /**
     * 废弃 API 版本
     */
    public void deprecateVersion(String apiName, String version, Date deprecationDate) {
        String key = "api:version:" + apiName + ":" + version;
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            throw new BusinessException("版本不存在");
        }

        ApiVersion apiVersion = JSON.parseObject(json, ApiVersion.class);
        apiVersion.setStatus(ApiVersionStatus.DEPRECATED);
        apiVersion.setDeprecationDate(deprecationDate != null ? deprecationDate : new Date());

        redisTemplate.opsForValue().set(key, JSON.toJSONString(apiVersion));

        log.info("API 版本已废弃: {} v{}", apiName, version);
    }

    /**
     * 强制使用特定版本
     */
    public void forceVersion(String apiName, String version) {
        redisTemplate.opsForValue().set("api:forced:" + apiName, version);

        log.info("强制版本设置: {} -> {}", apiName, version);
    }

    /**
     * 获取可用版本
     */
    public List<ApiVersionInfo> getAvailableVersions(String apiName) {
        String versionsKey = "api:versions:" + apiName;
        String versions = redisTemplate.opsForValue().get(versionsKey);

        if (versions == null) {
            return new ArrayList<>();
        }

        List<String> versionList = JSON.parseArray(versions, String.class);
        return versionList.stream()
            .map(v -> getVersionInfo(apiName, v))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private void updateVersionList(String apiName, ApiVersion version) {
        String versionsKey = "api:versions:" + apiName;
        String versions = redisTemplate.opsForValue().get(versionsKey);

        List<String> versionList;
        if (versions == null) {
            versionList = new ArrayList<>();
        } else {
            versionList = JSON.parseArray(versions, String.class);
        }

        if (!versionList.contains(version.getVersion())) {
            versionList.add(version.getVersion());
            redisTemplate.opsForValue().set(versionsKey, JSON.toJSONString(versionList));
        }
    }

    private ApiVersionInfo getVersionInfo(String apiName, String version) {
        String key = "api:version:" + apiName + ":" + version;
        String json = redisTemplate.opsForValue().get(key);

        if (json == null) {
            return null;
        }

        ApiVersion apiVersion = JSON.parseObject(json, ApiVersion.class);

        return ApiVersionInfo.builder()
            .version(version)
            .status(apiVersion.getStatus())
            .createTime(apiVersion.getCreateTime())
            .deprecationDate(apiVersion.getDeprecationDate())
            .isForced(isForced(apiName))
            .build();
    }

    private boolean isForced(String apiName) {
        String forcedVersion = redisTemplate.opsForValue().get("api:forced:" + apiName);
        return forcedVersion != null;
    }

    /**
     * 生成迁移指南
     */
    public String generateMigrationGuide(String apiName, String version) {
        // 获取旧版本和新版本的 API 定义
        ApiDefinition oldApi = apiRegistry.getApiDefinition(apiName, version);
        ApiDefinition newApi = apiRegistry.getLatestApiDefinition(apiName);

        // 生成迁移指南
        StringBuilder guide = new StringBuilder();
        guide.append("# API 迁移指南\n\n");
        guide.append("## 从 ").append(version).append(" 迁移到 最新版本\n\n");
        guide.append("### 主要变更\n\n");

        // 比较 API 定义，生成变更日志
        List<ApiChange> changes = compareApiDefinitions(oldApi, newApi);
        for (ApiChange change : changes) {
            guide.append("- ").append(change.getDescription()).append("\n");
        }

        guide.append("\n### 迁移步骤\n\n");
        guide.append("1. 更新 API 版本号\n");
        guide.append("2. 更新必要的请求参数\n");
        guide.append("3. 处理新增的响应字段\n");
        guide.append("4. 测试应用程序\n");

        return guide.toString();
    }

    private List<ApiChange> compareApiDefinitions(ApiDefinition oldApi, ApiDefinition newApi) {
        // 实现 API 定义比较逻辑
        return new ArrayList<>();
    }

    @Data
    @Builder
    public static class ApiVersionInfo {
        private String version;
        private ApiVersionStatus status;
        private Date createTime;
        private Date deprecationDate;
        private boolean isForced;
    }

    @Data
    @Builder
    public static class ApiVersion {
        private String apiName;
        private String version;
        private ApiVersionStatus status;
        private Date createTime;
        private Date deprecationDate;
    }

    public enum ApiVersionStatus {
        ACTIVE, DEPRECATED, RETIRED
    }
}
```

---

## 🧪 测试与验证

### 1. 网关功能测试

```bash
#!/bin/bash
# gateway-test.sh

set -e

BASE_URL="http://localhost:8080"

log_info() {
    echo -e "\033[0;34m[INFO]\033[0m $1"
}

log_success() {
    echo -e "\033[0;32m[SUCCESS]\033[0m $1"
}

log_error() {
    echo -e "\033[0;31m[ERROR]\033[0m $1"
}

# 测试路由功能
test_routing() {
    log_info "测试路由功能..."

    # 测试用户服务路由
    response=$(curl -s -X GET "${BASE_URL}/api/user/profile" \
        -H "X-API-Version: v1" \
        -w "\nHTTP_CODE:%{http_code}")
    http_code=$(echo "$response" | grep "HTTP_CODE" | cut -d':' -f2)

    if [ "$http_code" == "200" ]; then
        log_success "用户服务路由测试通过"
    else
        log_error "用户服务路由测试失败，HTTP CODE: $http_code"
        return 1
    fi
}

# 测试限流功能
test_rate_limiting() {
    log_info "测试限流功能..."

    # 发送快速请求测试限流
    count=0
    for i in {1..30}; do
        response=$(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}/api/user/profile")
        if [ "$response" == "429" ]; then
            count=$((count + 1))
        fi
        sleep 0.1
    done

    if [ $count -gt 0 ]; then
        log_success "限流功能测试通过，触发 $count 次限流"
    else
        log_error "限流功能测试失败"
        return 1
    fi
}

# 测试熔断功能
test_circuit_breaker() {
    log_info "测试熔断功能..."

    # 模拟服务故障
    kubectl scale deployment user-service --replicas=0 -n default

    sleep 5

    # 测试降级响应
    response=$(curl -s "${BASE_URL}/api/user/profile" -w "\nHTTP_CODE:%{http_code}")
    http_code=$(echo "$response" | grep "HTTP_CODE" | cut -d':' -f2)

    # 恢复服务
    kubectl scale deployment user-service --replicas=3 -n default

    if [ "$http_code" == "503" ]; then
        log_success "熔断功能测试通过"
    else
        log_error "熔断功能测试失败，HTTP CODE: $http_code"
        return 1
    fi
}

# 测试金丝雀发布
test_canary_release() {
    log_info "测试金丝雀发布..."

    # 发送金丝雀请求
    response=$(curl -s "${BASE_URL}/api/user/profile" \
        -H "X-Canary: true" \
        -w "\nHTTP_CODE:%{http_code}")
    http_code=$(echo "$response" | grep "HTTP_CODE" | cut -d':' -f2)

    if [ "$http_code" == "200" ]; then
        log_success "金丝雀发布测试通过"
    else
        log_error "金丝CANARY发布测试失败，HTTP CODE: $http_code"
        return 1
    fi
}

# 测试 API 版本
test_api_versioning() {
    log_info "测试 API 版本管理..."

    # 测试 v1 版本
    response_v1=$(curl -s "${BASE_URL}/api/v1/user/profile" -w "\nHTTP_CODE:%{http_code}")
    http_code_v1=$(echo "$response_v1" | grep "HTTP_CODE" | cut -d':' -f2)

    if [ "$http_code_v1" == "200" ]; then
        log_success "API v1 版本测试通过"
    else
        log_error "API v1 版本测试失败，HTTP CODE: $http_code_v1"
        return 1
    fi
}

# 测试安全防护
test_security() {
    log_info "测试安全防护..."

    # 测试未授权访问
    response=$(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}/api/admin/user")

    if [ "$response" == "401" ]; then
        log_success "未授权访问测试通过"
    else
        log_error "未授权访问测试失败，HTTP CODE: $response"
        return 1
    fi

    # 测试 SQL 注入防护
    response=$(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}/api/user/search?name=admin%27%20OR%20%271%27=%271")

    if [ "$response" == "400" ] || [ "$response" == "403" ]; then
        log_success "SQL 注入防护测试通过"
    else
        log_error "SQL 注入防护测试失败，HTTP CODE: $response"
        return 1
    fi
}

# 测试监控指标
test_monitoring() {
    log_info "测试监控指标..."

    # 触发几个请求
    for i in {1..5}; do
        curl -s "${BASE_URL}/api/user/profile" > /dev/null
    done

    sleep 2

    # 检查 Prometheus 指标
    metrics=$(curl -s "http://localhost:9090/api/v1/query?query=gateway_requests_total")
    if echo "$metrics" | grep -q "gateway_requests_total"; then
        log_success "监控指标测试通过"
    else
        log_error "监控指标测试失败"
        return 1
    fi
}

# 主函数
main() {
    echo "========================================"
    echo "      API 网关功能测试"
    echo "========================================"
    echo ""

    test_routing || exit 1
    echo ""

    test_rate_limiting || exit 1
    echo ""

    test_circuit_breaker || exit 1
    echo ""

    test_canary_release || exit 1
    echo ""

    test_api_versioning || exit 1
    echo ""

    test_security || exit 1
    echo ""

    test_monitoring || exit 1
    echo ""

    log_success "所有网关功能测试通过！"
}

main "$@"
```

---

## 📊 监控与告警

### 1. 网关监控指标

```yaml
# gateway-metrics.yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: spring-cloud-gateway
  namespace: istio-system
spec:
  selector:
    matchLabels:
      app: spring-cloud-gateway
  endpoints:
  - port: metrics
    interval: 15s
    path: /actuator/prometheus

---
apiVersion: v1
kind: ConfigMap
metadata:
  name: gateway-alerts
  namespace: istio-system
data:
  alerts.yml: |
    groups:
    - name: gateway.rules
      rules:
      # 高错误率告警
      - alert: GatewayHighErrorRate
        expr: |
          (
            sum(rate(gateway_requests_total{status_code=~"5.."}[5m])) by (service_name)
            /
            sum(rate(gateway_requests_total[5m])) by (service_name)
          ) > 0.1
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Gateway high error rate"
          description: "Gateway error rate for {{ $labels.service_name }} is {{ $value | humanizePercentage }}"

      # 高延迟告警
      - alert: GatewayHighLatency
        expr: histogram_quantile(0.99, sum(rate(gateway_request_duration_seconds_bucket[5m])) by (le, service_name)) > 1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Gateway high latency"
          description: "99th percentile latency for {{ $labels.service_name }} is {{ $value }}s"

      # 限流触发告警
      - alert: GatewayRateLimitTriggered
        expr: increase(gateway_rate_limited_total[5m]) > 100
        for: 1m
        labels:
          severity: warning
        annotations:
          summary: "Rate limit triggered"
          description: "Rate limit triggered {{ $value }} times in the last 5 minutes"

      # 熔断器打开告警
      - alert: GatewayCircuitBreakerOpen
        expr: gateway_circuit_breaker_open > 0
        for: 0s
        labels:
          severity: critical
        annotations:
          summary: "Circuit breaker open"
          description: "Circuit breaker is open for {{ $labels.service_name }}"

      # 动态路由更新失败告警
      - alert: GatewayDynamicRouteUpdateFailed
        expr: increase(gateway_route_update_failed_total[5m]) > 0
        for: 0s
        labels:
          severity: warning
        annotations:
          summary: "Dynamic route update failed"
          description: "Failed to update {{ $value }} dynamic routes in the last 5 minutes"
```

### 2. Grafana 仪表盘

```json
{
  "dashboard": {
    "title": "API Gateway Dashboard",
    "panels": [
      {
        "title": "Total Requests",
        "type": "stat",
        "targets": [
          {
            "expr": "sum(increase(gateway_requests_total[5m]))",
            "legendFormat": "Requests"
          }
        ]
      },
      {
        "title": "Request Rate by Service",
        "type": "graph",
        "targets": [
          {
            "expr": "sum(rate(gateway_requests_total[5m])) by (service_name)",
            "legendFormat": "{{ service_name }}"
          }
        ]
      },
      {
        "title": "Success Rate",
        "type": "singlestat",
        "targets": [
          {
            "expr": "sum(rate(gateway_requests_total{status_code!~\"5..\"}[5m])) / sum(rate(gateway_requests_total[5m])) * 100",
            "legendFormat": "Success Rate"
          }
        ],
        "thresholds": "95,99,99.9",
        "colorBackground": true
      },
      {
        "title": "Request Duration (P50/P90/P99)",
        "type": "graph",
        "targets": [
          {
            "expr": "histogram_quantile(50, sum(rate(gateway_request_duration_seconds_bucket[5m])) by (le))",
            "legendFormat": "P50"
          },
          {
            "expr": "histogram_quantile(90, sum(rate(gateway_request_duration_seconds_bucket[5m])) by (le))",
            "legendFormat": "P90"
          },
          {
            "expr": "histogram_quantile(99, sum(rate(gateway_request_duration_seconds_bucket[5m])) by (le))",
            "legendFormat": "P99"
          }
        ]
      },
      {
        "title": "Rate Limit Triggers",
        "type": "graph",
        "targets": [
          {
            "expr": "sum(rate(gateway_rate_limited_total[5m])) by (service_name)",
            "legendFormat": "{{ service_name }}"
          }
        ]
      },
      {
        "title": "Circuit Breaker Status",
        "type": "stat",
        "targets": [
          {
            "expr": "sum(gateway_circuit_breaker_open)",
            "legendFormat": "Open"
          }
        ]
      },
      {
        "title": "Error Rate by Status Code",
        "type": "graph",
        "targets": [
          {
            "expr": "sum(rate(gateway_requests_total{status_code=~\"4..\"}[5m])) by (status_code)",
            "legendFormat": "4xx - {{ status_code }}"
          },
          {
            "expr": "sum(rate(gateway_requests_total{status_code=~\"5..\"}[5m])) by (status_code)",
            "legendFormat": "5xx - {{ status_code }}"
          }
        ]
      },
      {
        "title": "Version Distribution",
        "type": "piechart",
        "targets": [
          {
            "expr": "sum(gateway_requests_total) by (version)",
            "legendFormat": "{{ version }}"
          }
        ]
      }
    ]
  }
}
```

---

## 📚 参考资料

1. [Spring Cloud Gateway 官方文档](https://cloud.spring.io/spring-cloud-gateway/)
2. [Istio Gateway 配置](https://istio.io/latest/docs/reference/config/networking/gateway/)
3. [Resilience4j 熔断器](https://resilience4j.readme.io/)
4. [Redis 限流器](https://github.com/spring-cloud/spring-cloud-gateway/tree/main/spring-cloud-gateway-server/src/main/java/org/springframework/cloud/gateway/filter/ratelimit)

---

## 📋 API 网关实施检查清单

### 路由管理
- [ ] 动态路由配置
- [ ] 基于条件的路由规则
- [ ] 权重分配路由
- [ ] 重试机制配置
- [ ] 路由优先级管理

### 流量控制
- [ ] IP 限流配置
- [ ] 用户限流配置
- [ ] API 限流配置
- [ ] 熔断器配置
- [ ] 降级策略实现
- [ ] 超时控制

### 安全防护
- [ ] 认证授权机制
- [ ] WAF 防护规则
- [ ] 防重放攻击
- [ ] SQL 注入防护
- [ ] XSS 防护
- [ ] 黑名单/白名单

### 灰度发布
- [ ] 金丝雀发布策略
- [ ] 蓝绿部署配置
- [ ] A/B 测试实现
- [ ] 流量镜像配置
- [ ] 自动推进机制

### 版本管理
- [ ] API 版本路由
- [ ] 版本兼容性检查
- [ ] 版本迁移助手
- [ ] 废弃版本管理
- [ ] 版本强制升级

### 监控审计
- [ ] 请求指标收集
- [ ] 性能监控仪表盘
- [ ] 告警规则配置
- [ ] 调用链追踪
- [ ] 审计日志记录
- [ ] 异常告警通知

---

**编制：** 浮浮酱 🐱（猫娘工程师）
**日期：** 2025-11-14
**状态：** 📋 指南完成，准备实施

**加油喵～ API 网关增强即将完成！** ฅ'ω'ฅ
