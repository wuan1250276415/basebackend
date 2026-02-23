# basebackend-cache Extensions — Zero-Decision Spec-Plan

> Generated: 2026-02-21
> Status: Ready for implementation
> Module: `basebackend-cache`
> Branch: `fix/scheduler-compilation-errors` (recommend new branch: `feat/cache-extensions`)

---

## Resolved Constraints

| Decision Point | Resolution | Rationale |
|---------------|-----------|-----------|
| Rate Limiter fail mode | **Fail-open** (allow all when Redis down) | Availability over protection; rate limiting is best-effort guardrail |
| Admin API exposure | **Both** Actuator + REST controller | Actuator for ops, REST for admin UI; each independently toggleable |
| Cross-Service Invalidation transport | **Redis Pub/Sub only** | Zero new dependencies, extends existing `cache:eviction` pattern |
| Compression algorithm | **GZIP only** | JDK built-in, zero new Maven dependencies |

---

## Extension Specifications

### P0-1: Rate Limiter

**Package:** `com.basebackend.cache.ratelimit`

**Files to create:**

| File | Type | Purpose |
|------|------|---------|
| `annotation/RateLimit.java` | Annotation | Method-level rate limit declaration |
| `ratelimit/RateLimitService.java` | Interface | Programmatic rate limiting API |
| `ratelimit/RateLimitServiceImpl.java` | Implementation | Wraps Redisson `RRateLimiter` |
| `ratelimit/RateLimitAspect.java` | AOP Aspect | Processes `@RateLimit` annotation |
| `ratelimit/RateLimitExceededException.java` | Exception | Extends `CacheException`, carries retry-after info |

**Files to modify:**

| File | Change |
|------|--------|
| `config/CacheProperties.java` | Add nested `RateLimiter` config class |
| `config/CacheAutoConfiguration.java` | Add validation for rate-limiter properties, log status in `init()` |

**Annotation contract:**
```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    String key() default "";          // SpEL expression; empty = className:methodName
    long rate() default 100;          // permits per interval
    long interval() default 60;       // interval value
    TimeUnit timeUnit() default TimeUnit.SECONDS;
    RateType mode() default RateType.OVERALL;  // OVERALL | PER_CLIENT
}
```

**RateLimitService contract:**
```java
public interface RateLimitService {
    boolean tryAcquire(String key, long rate, long interval, TimeUnit unit, RateType mode);
    boolean tryAcquire(String key, long permits, long rate, long interval, TimeUnit unit, RateType mode);
    long availablePermits(String key);
    void deleteRateLimiter(String key);
}
```

**Redisson API usage:**
- `redissonClient.getRateLimiter(key)` -> `RRateLimiter`
- `rateLimiter.trySetRate(RateType.OVERALL, rate, interval, RateIntervalUnit)` on first access
- `rateLimiter.tryAcquire(1)` per request
- `rateLimiter.availablePermits()` for admin inspection

**Fail-open behavior:**
- `RateLimitAspect`: wrap `tryAcquire` in try-catch. On `RedisConnectionFailureException` or timeout, log WARN and **allow** the request.
- Emit Micrometer counter `cache.ratelimit.failopen` on each fail-open event.

**Metrics:**
- Counter: `cache.ratelimit.acquired` (tags: key, result=allowed|rejected)
- Counter: `cache.ratelimit.failopen` (tags: key)
- Gauge: `cache.ratelimit.available` (tags: key)

**Configuration properties:**
```yaml
basebackend.cache.rate-limiter:
  enabled: false                    # default disabled
  default-rate: 100                 # requests per interval
  default-interval: 60s            # interval duration
  default-mode: OVERALL            # OVERALL | PER_CLIENT
  fail-open: true                  # allow on Redis failure
  key-prefix: "rl:"               # Redis key prefix
```

**Validation rules (in CacheAutoConfiguration):**
- `rate-limiter.default-rate` > 0
- `rate-limiter.default-interval` > 0
- `rate-limiter.default-mode` in {OVERALL, PER_CLIENT}

---

### P0-2: Cache Admin Endpoints

**Package:** `com.basebackend.cache.admin`

**Files to create:**

| File | Type | Purpose |
|------|------|---------|
| `admin/CacheAdminEndpoint.java` | Actuator Endpoint | `@Endpoint(id = "cacheAdmin")` |
| `admin/CacheAdminController.java` | REST Controller | `@RestController @RequestMapping("/api/cache/admin")` |
| `admin/CacheAdminAutoConfiguration.java` | Configuration | Conditional bean registration |
| `admin/dto/CacheInfoDTO.java` | DTO | Cache summary (name, size, hitRate, avgLatency) |
| `admin/dto/CacheDetailDTO.java` | DTO | Full statistics + sample keys |

**Files to modify:**

| File | Change |
|------|--------|
| `config/CacheProperties.java` | Add nested `Admin` config class |
| `config/CacheAutoConfiguration.java` | `@Import(CacheAdminAutoConfiguration.class)`, log status |

**Actuator endpoint operations:**

| Verb | Path | Method | Delegates To |
|------|------|--------|-------------|
| GET | `/actuator/cacheAdmin` | `listCaches()` | `CacheService.getAllCacheNames()` + `getStatistics()` per name |
| GET | `/actuator/cacheAdmin/{name}` | `getCacheDetail(name)` | `CacheService.getStatistics(name)` + `getCacheSize(name)` |
| DELETE | `/actuator/cacheAdmin/{name}` | `clearCache(name)` | `CacheService.clearCache(name)` |

**REST controller endpoints:**

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/cache/admin/caches` | List all caches with summary stats |
| GET | `/api/cache/admin/caches/{name}` | Detailed cache statistics |
| GET | `/api/cache/admin/caches/{name}/keys` | Keys matching pattern (query param `pattern`, default `*`, limit 100) |
| DELETE | `/api/cache/admin/caches/{name}` | Clear a specific cache |
| POST | `/api/cache/admin/caches/clear-all` | Clear all caches (requires `confirmed=true` body param) |
| POST | `/api/cache/admin/caches/{name}/reset-stats` | Reset statistics |
| GET | `/api/cache/admin/health` | Redis connection + circuit breaker status |

**Security constraint:** REST controller annotated with `@ConditionalOnProperty(prefix = "basebackend.cache.admin", name = "rest-enabled")`. No explicit security annotations in this module — consuming application applies its own security (e.g., `@RequiresPermission` from admin-api).

**New optional dependency (pom.xml):**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
    <optional>true</optional>
</dependency>
```

**Configuration properties:**
```yaml
basebackend.cache.admin:
  enabled: false
  rest-enabled: false
  keys-scan-limit: 100        # max keys returned by /keys endpoint
```

---

### P1-1: Near-Expiry Refresh

**Package:** `com.basebackend.cache.refresh`

**Files to create:**

| File | Type | Purpose |
|------|------|---------|
| `refresh/NearExpiryRefreshManager.java` | Service | Manages async refresh scheduling |
| `refresh/RefreshTask.java` | Data model | Captures key, original method, TTL for replay |
| `refresh/NearExpiryRefreshAutoConfiguration.java` | Configuration | Thread pool + conditional beans |

**Files to modify:**

| File | Change |
|------|--------|
| `aspect/CacheAspect.java` | After cache HIT in `handleCacheable()`: call `NearExpiryRefreshManager.checkAndRefresh()` |
| `config/CacheProperties.java` | Add nested `Refresh` config class |
| `config/CacheAutoConfiguration.java` | Import, validate, log |

**Algorithm (in CacheAspect after HIT):**
```
1. remainingTTL = RedisService.getExpiration(key)  // seconds
2. if remainingTTL < 0: return  // no TTL or key gone
3. originalTTL = annotation.ttl()  // from @Cacheable annotation
4. ratio = remainingTTL / originalTTL
5. if ratio <= refreshThresholdRatio:
6.   NearExpiryRefreshManager.submitRefresh(key, joinPoint, originalTTL)
```

**NearExpiryRefreshManager.submitRefresh():**
```
1. if refreshInProgress.putIfAbsent(key, true) != null: return  // dedup
2. CompletableFuture.runAsync(() -> {
3.   try {
4.     if (!distributedLockService.tryLock("refresh:" + key, 0, 30, SECONDS)): return
5.     try {
6.       Object newValue = joinPoint.proceed()   // re-execute original method
7.       redisService.set(key, newValue, originalTTL)
8.       // update local cache if multi-level
9.     } finally {
10.      distributedLockService.unlock("refresh:" + key)
11.    }
12.  } finally {
13.    refreshInProgress.remove(key)
14.  }
15.}, refreshExecutor)
```

**Thread pool:** `ScheduledThreadPoolExecutor` with configurable `poolSize`, daemon threads, `CallerRunsPolicy` rejection handler (logs and skips).

**Metrics:**
- Counter: `cache.refresh.triggered` (tags: cacheName)
- Counter: `cache.refresh.completed` (tags: cacheName, result=success|failure|skipped)

**Configuration properties:**
```yaml
basebackend.cache.refresh:
  enabled: false
  threshold-ratio: 0.2       # trigger refresh when <= 20% TTL remaining
  pool-size: 4
  lock-wait-time: 0s          # no-wait
  lock-lease-time: 30s
```

**Validation:**
- `threshold-ratio` in (0.0, 1.0) exclusive
- `pool-size` > 0

---

### P1-2: Hot Key Detection

**Package:** `com.basebackend.cache.hotkey`

**Files to create:**

| File | Type | Purpose |
|------|------|---------|
| `hotkey/HotKeyDetector.java` | Service | Sliding window access counter |
| `hotkey/HotKeyMitigator.java` | Service | Promotes detected hot keys to local Caffeine |
| `hotkey/HotKeyStats.java` | DTO | Key name, access count, window, isHot |
| `hotkey/HotKeyAutoConfiguration.java` | Configuration | Conditional beans, standalone Caffeine for mitigation |

**Files to modify:**

| File | Change |
|------|--------|
| `aspect/CacheAspect.java` | After any cache GET: call `HotKeyDetector.recordAccess(key)` |
| `config/CacheProperties.java` | Add nested `HotKey` config class |
| `config/CacheAutoConfiguration.java` | Import, validate, log |

**HotKeyDetector algorithm:**
- Data structure: `ConcurrentHashMap<String, LongAdder>` (current window) + `ConcurrentHashMap<String, LongAdder>` (previous window)
- Scheduled rotation: every `windowSize`, swap current<->previous, clear new current
- Detection: `currentCount + previousCount > threshold` -> hot
- Top-K: maintain `ConcurrentSkipListSet<HotKeyStats>` bounded to `topK`, sorted by access count
- Memory bound: if map size exceeds `topK * 10`, evict lowest-count entries

**HotKeyMitigator:**
- Standalone `Caffeine<String, Object>` cache (independent of `MultiLevelCacheManager`)
- When hot key detected: `mitigationCache.put(key, value)` with `localCacheTtl` + random jitter (0-20%)
- `CacheAspect` checks `mitigationCache` BEFORE Redis for GET operations (if hot key feature enabled)

**Metrics:**
- Gauge: `cache.hotkey.detected` (current count of hot keys)
- Counter: `cache.hotkey.mitigated` (tags: key)
- Gauge: `cache.hotkey.topk` (exposed as JSON via admin endpoint if admin enabled)

**Configuration properties:**
```yaml
basebackend.cache.hot-key:
  enabled: false
  window-size: 10s
  threshold: 1000             # accesses per window to classify as hot
  top-k: 100
  local-cache-ttl: 5s
  local-cache-max-size: 500
  jitter-percent: 20          # random TTL jitter 0-20%
```

**Validation:**
- `window-size` > 0
- `threshold` > 0
- `top-k` > 0, <= 10000
- `local-cache-max-size` > 0

---

### P2-1: Batch Pipeline

**Package:** `com.basebackend.cache.pipeline`

**Files to create:**

| File | Type | Purpose |
|------|------|---------|
| `pipeline/RedisPipelineTemplate.java` | Service | Fluent pipeline builder |
| `pipeline/PipelineOperation.java` | Data model | Describes a single operation (type, key, value, TTL) |
| `pipeline/PipelineResult.java` | Data model | Holds ordered results |

**Files to modify:**

| File | Change |
|------|--------|
| `config/CacheProperties.java` | Add nested `Pipeline` config class |
| `config/CacheAutoConfiguration.java` | Validate, log |

**API contract:**
```java
public class RedisPipelineTemplate {
    public PipelineBuilder pipeline() { ... }

    public static class PipelineBuilder {
        PipelineBuilder get(String key);
        PipelineBuilder set(String key, Object value);
        PipelineBuilder set(String key, Object value, Duration ttl);
        PipelineBuilder delete(String key);
        PipelineBuilder expire(String key, Duration ttl);
        PipelineBuilder exists(String key);
        PipelineBuilder incr(String key);
        PipelineResult execute();  // executes via RedisTemplate.executePipelined
    }
}
```

**Constraints:**
- `execute()` throws `IllegalStateException` if operations exceed `maxBatchSize`
- Each operation validates key format via `CacheKeyGenerator.isValidKey()`
- Respects `RedisService` circuit breaker state — if OPEN, throw `CacheConnectionException`

**Configuration properties:**
```yaml
basebackend.cache.pipeline:
  enabled: true               # default enabled (lightweight)
  max-batch-size: 1000
  timeout: 5s
```

---

### P2-2: Cross-Service Invalidation

**Package:** `com.basebackend.cache.invalidation`

**Files to create:**

| File | Type | Purpose |
|------|------|---------|
| `invalidation/CacheInvalidationEvent.java` | Data model | source, cacheName, key/pattern, timestamp, correlationId |
| `invalidation/CacheInvalidationPublisher.java` | Service | Publishes via Redis Pub/Sub |
| `invalidation/CacheInvalidationListener.java` | Listener | Subscribes and applies local invalidation |
| `invalidation/CacheInvalidationAutoConfiguration.java` | Configuration | Registers listener on channel |

**Files to modify:**

| File | Change |
|------|--------|
| `config/CacheProperties.java` | Add nested `Invalidation` config class |
| `config/CacheAutoConfiguration.java` | Import, validate, log |

**Event format (JSON over Pub/Sub):**
```json
{
  "source": "user-api",
  "cacheName": "user",
  "keyPattern": "user:123",
  "type": "EVICT",           // EVICT | CLEAR | CLEAR_ALL
  "timestamp": 1708531200000,
  "correlationId": "uuid"
}
```

**Channel:** `cache:invalidation` (distinct from existing `cache:eviction` to avoid breaking backward compatibility)

**Listener behavior:**
1. Deserialize `CacheInvalidationEvent`
2. Skip if `event.source == self.serviceName` (prevent self-invalidation loops)
3. Apply: `CacheService.delete(key)` or `CacheService.clearCache(cacheName)` or `CacheService.clearAllCaches()`
4. If multi-level enabled: also evict from local Caffeine

**Metrics:**
- Counter: `cache.invalidation.published` (tags: cacheName, type)
- Counter: `cache.invalidation.received` (tags: cacheName, type, source)
- Counter: `cache.invalidation.skipped.self` (self-origin events)

**Configuration properties:**
```yaml
basebackend.cache.invalidation:
  enabled: false
  channel: "cache:invalidation"
  service-name: ${spring.application.name:unknown}
```

---

### P3-1: Compression

**Package:** `com.basebackend.cache.compression`

**Files to create:**

| File | Type | Purpose |
|------|------|---------|
| `compression/CacheCompressor.java` | Interface | `compress(byte[])` / `decompress(byte[])` |
| `compression/GzipCacheCompressor.java` | Implementation | JDK `GZIPOutputStream` / `GZIPInputStream` |
| `compression/CompressingSerializer.java` | Decorator | Wraps `CacheSerializer`, applies compression above threshold |

**Files to modify:**

| File | Change |
|------|--------|
| `serializer/SerializerFactory.java` | If compression enabled, wrap returned serializer with `CompressingSerializer` |
| `config/CacheProperties.java` | Add nested `Compression` config class |
| `config/CacheAutoConfiguration.java` | Validate, log |

**CompressingSerializer logic:**
```java
// serialize:
byte[] raw = delegate.serialize(object);
if (raw.length > thresholdBytes) {
    byte[] compressed = compressor.compress(raw);
    return prependMagicByte(COMPRESSED_MARKER, compressed);  // 1-byte prefix
} else {
    return prependMagicByte(UNCOMPRESSED_MARKER, raw);
}

// deserialize:
byte marker = data[0];
byte[] payload = Arrays.copyOfRange(data, 1, data.length);
if (marker == COMPRESSED_MARKER) {
    payload = compressor.decompress(payload);
}
return delegate.deserialize(payload, type);
```

**Magic byte:** `0x01` = uncompressed, `0x02` = GZIP. This allows adding new algorithms later without breaking existing cached data.

**No new Maven dependencies** (GZIP is JDK built-in).

**Configuration properties:**
```yaml
basebackend.cache.compression:
  enabled: false
  algorithm: GZIP
  threshold-bytes: 1024       # only compress payloads > 1KB
```

**Validation:**
- `algorithm` must be `GZIP` (only supported value in V1)
- `threshold-bytes` >= 0

---

### P3-2: Scheduled Eviction

**Package:** `com.basebackend.cache.eviction` (extends existing `manager/CacheEvictionManager`)

**Files to create:**

| File | Type | Purpose |
|------|------|---------|
| `eviction/ScheduledEvictionRule.java` | Data model | cachePattern, cron, description |
| `eviction/ScheduledEvictionExecutor.java` | Service | Registers cron tasks via `TaskScheduler` |
| `eviction/ScheduledEvictionAutoConfiguration.java` | Configuration | Conditional, creates `TaskScheduler` bean |

**Files to modify:**

| File | Change |
|------|--------|
| `config/CacheProperties.java` | Add nested `Eviction.Scheduled` config class with `List<ScheduledRule>` |
| `config/CacheAutoConfiguration.java` | Import, validate cron expressions, log |

**ScheduledEvictionExecutor:**
```java
@PostConstruct
void init() {
    for (ScheduledEvictionRule rule : rules) {
        taskScheduler.schedule(
            () -> executeEviction(rule),
            new CronTrigger(rule.getCron())
        );
    }
}

void executeEviction(ScheduledEvictionRule rule) {
    log.info("Executing scheduled eviction: pattern={}, cron={}", rule.getCachePattern(), rule.getCron());
    long evicted = cacheEvictionManager.evictByPattern(rule.getCachePattern());
    meterRegistry.counter("cache.eviction.scheduled", "pattern", rule.getCachePattern()).increment(evicted);
    log.info("Scheduled eviction completed: pattern={}, evicted={}", rule.getCachePattern(), evicted);
}
```

**Uses:** existing `CacheEvictionManager.evictByPattern()` — no new eviction logic needed.

**Configuration properties:**
```yaml
basebackend.cache.eviction:
  scheduled:
    enabled: false
    rules:
      - cache-pattern: "session:*"
        cron: "0 0 3 * * ?"
        description: "Clear expired sessions at 3 AM"
      - cache-pattern: "report:*"
        cron: "0 0 0 1 * ?"
        description: "Clear report cache monthly"
```

**Validation:**
- Each rule must have non-empty `cache-pattern`
- Each rule must have valid cron expression (use `CronExpression.isValidExpression()`)
- Duplicate patterns with same cron: log WARN

---

## PBT (Property-Based Testing) Properties

### Rate Limiter

| Property | Invariant | Falsification Strategy |
|----------|-----------|----------------------|
| **Idempotency of rate setup** | Calling `trySetRate()` on existing limiter with same params is no-op | Generate random rate/interval, call setup twice, verify second call returns false |
| **Monotonic permit consumption** | After N `tryAcquire(1)` calls, at most `rate` succeed within `interval` | For rate=R, interval=I: call tryAcquire R+10 times in <I, assert exactly R succeed |
| **Fail-open under disconnection** | When Redis unavailable, `tryAcquire` returns true (no exception) | Mock RedissonClient to throw, verify aspect allows method execution |
| **Key isolation** | Different keys have independent counters | Create 2 limiters with same rate, exhaust one, verify other still has permits |

### Cache Admin Endpoints

| Property | Invariant | Falsification Strategy |
|----------|-----------|----------------------|
| **Listing completeness** | `listCaches()` returns superset of all cache names seen by `getStatistics()` | Populate N random caches, verify list contains all N |
| **Clear idempotency** | Clearing an empty cache returns 0 and does not error | Clear same cache twice, verify second returns 0 |
| **Stats reset consistency** | After `resetStatistics(name)`, all counters are 0 | Record random metrics, reset, verify all fields are 0 |

### Near-Expiry Refresh

| Property | Invariant | Falsification Strategy |
|----------|-----------|----------------------|
| **Single-flight** | At most 1 refresh executes per key concurrently | Trigger 100 concurrent refreshes for same key, verify loader called exactly once |
| **Threshold correctness** | Refresh only triggers when `remainingTTL / originalTTL <= threshold` | Generate random (remaining, original, threshold) tuples, verify trigger decision matches formula |
| **Stale-while-revalidate** | Caller always gets a value (stale or fresh) — never blocked | Simulate slow loader (5s), verify caller returns within 10ms |

### Hot Key Detection

| Property | Invariant | Falsification Strategy |
|----------|-----------|----------------------|
| **Threshold accuracy** | Key classified as hot iff access count >= threshold in window | Generate access counts at/around threshold, verify classification boundary |
| **Memory bounded** | Counter map size never exceeds `topK * 10` | Insert 100K distinct keys, verify map size stays bounded |
| **Window rotation** | After window rotation, previous window's counts decay | Record N accesses, wait > windowSize, verify counts halved (prev window only) |
| **Jitter bounds** | Mitigation TTL is within `[localCacheTtl, localCacheTtl * (1 + jitterPercent/100)]` | Generate 1000 mitigated keys, verify all TTLs within expected range |

### Batch Pipeline

| Property | Invariant | Falsification Strategy |
|----------|-----------|----------------------|
| **Order preservation** | `PipelineResult.get(i)` corresponds to `PipelineBuilder` operation at index i | Build pipeline with N random ops, verify result indices match |
| **Size enforcement** | Pipeline with >maxBatchSize operations throws `IllegalStateException` | Build pipeline with maxBatchSize+1 operations, verify exception |
| **Atomicity (best-effort)** | All operations in a pipeline execute even if one fails | Include a DEL on non-existent key in pipeline, verify other SETs succeed |

### Cross-Service Invalidation

| Property | Invariant | Falsification Strategy |
|----------|-----------|----------------------|
| **Self-skip** | Events published by self are ignored by self's listener | Publish event, verify listener counter for "skipped.self" increments |
| **Round-trip serialization** | `deserialize(serialize(event)) == event` for all valid events | Generate random CacheInvalidationEvent instances, verify round-trip equality |
| **Channel isolation** | Messages on `cache:eviction` don't trigger invalidation listener and vice versa | Publish on wrong channel, verify no invalidation applied |

### Compression

| Property | Invariant | Falsification Strategy |
|----------|-----------|----------------------|
| **Round-trip** | `decompress(compress(data)) == data` for all byte arrays | Generate random byte arrays (0-100KB), verify round-trip |
| **Threshold respect** | Payloads < `thresholdBytes` are stored uncompressed (marker byte = 0x01) | Generate payload of size threshold-1, verify marker byte |
| **Backward compatibility** | Pre-compression data (no marker byte) is still readable | Serialize without compression, add to Redis directly, verify CompressingSerializer reads it |
| **Size reduction** | Compressed output size <= input size for compressible data (repeated strings) | Generate highly compressible data (e.g., "AAAA..." * 10000), verify output < input |

### Scheduled Eviction

| Property | Invariant | Falsification Strategy |
|----------|-----------|----------------------|
| **Cron validity** | All configured rules have parseable cron expressions | Generate random invalid cron strings, verify startup validation rejects them |
| **Pattern isolation** | Eviction of pattern A does not affect keys matching pattern B | Create keys for 2 disjoint patterns, evict pattern A, verify pattern B intact |
| **Idempotent execution** | Running eviction twice on same pattern: second run evicts 0 | Evict, then evict again, verify second returns 0 |

---

## Implementation Task Breakdown (Zero-Decision)

All tasks are pure mechanical execution — every class, method, field, and config property is fully specified above.

### Phase 1: P0 Extensions (parallel-safe)

| Task ID | Task | New Files | Modified Files | Tests |
|---------|------|-----------|----------------|-------|
| P0-1a | Create `RateLimit` annotation | 1 | 0 | 0 |
| P0-1b | Create `RateLimitService` + `RateLimitServiceImpl` | 2 | 0 | 1 unit + 1 PBT |
| P0-1c | Create `RateLimitAspect` + `RateLimitExceededException` | 2 | 0 | 1 unit |
| P0-1d | Add `RateLimiter` config to `CacheProperties` + validation in `CacheAutoConfiguration` | 0 | 2 | 1 config test |
| P0-1e | Integration test: rate limiter with Testcontainers Redis | 0 | 0 | 1 integration |
| P0-2a | Create `CacheAdminEndpoint` (Actuator) | 1 | 0 | 1 unit |
| P0-2b | Create `CacheAdminController` (REST) + DTOs | 3 | 0 | 1 unit |
| P0-2c | Create `CacheAdminAutoConfiguration` | 1 | 0 | 1 config test |
| P0-2d | Add `Admin` config to `CacheProperties` + validation + add `spring-boot-starter-actuator` optional dep | 0 | 3 (Properties, AutoConfig, pom.xml) | 0 |
| P0-2e | Integration test: admin endpoints | 0 | 0 | 1 integration |

### Phase 2: P1 Extensions (parallel-safe)

| Task ID | Task | New Files | Modified Files | Tests |
|---------|------|-----------|----------------|-------|
| P1-1a | Create `NearExpiryRefreshManager` + `RefreshTask` | 2 | 0 | 1 unit + 1 PBT |
| P1-1b | Create `NearExpiryRefreshAutoConfiguration` | 1 | 0 | 1 config test |
| P1-1c | Modify `CacheAspect.handleCacheable()` to call refresh check | 0 | 1 | 0 |
| P1-1d | Add `Refresh` config to `CacheProperties` + validation | 0 | 2 | 0 |
| P1-1e | Integration test: near-expiry refresh with Testcontainers | 0 | 0 | 1 integration |
| P1-2a | Create `HotKeyDetector` + `HotKeyStats` | 2 | 0 | 1 unit + 1 PBT |
| P1-2b | Create `HotKeyMitigator` | 1 | 0 | 1 unit |
| P1-2c | Create `HotKeyAutoConfiguration` | 1 | 0 | 1 config test |
| P1-2d | Modify `CacheAspect` to record access + check mitigation cache | 0 | 1 | 0 |
| P1-2e | Add `HotKey` config to `CacheProperties` + validation | 0 | 2 | 0 |
| P1-2f | Integration test: hot key detection + mitigation | 0 | 0 | 1 integration |

### Phase 3: P2 Extensions (parallel-safe)

| Task ID | Task | New Files | Modified Files | Tests |
|---------|------|-----------|----------------|-------|
| P2-1a | Create `RedisPipelineTemplate` + `PipelineOperation` + `PipelineResult` | 3 | 0 | 1 unit + 1 PBT |
| P2-1b | Add `Pipeline` config to `CacheProperties` + validation | 0 | 2 | 0 |
| P2-1c | Integration test: pipeline with Testcontainers | 0 | 0 | 1 integration |
| P2-2a | Create `CacheInvalidationEvent` + `CacheInvalidationPublisher` | 2 | 0 | 1 unit |
| P2-2b | Create `CacheInvalidationListener` | 1 | 0 | 1 unit + 1 PBT |
| P2-2c | Create `CacheInvalidationAutoConfiguration` | 1 | 0 | 1 config test |
| P2-2d | Add `Invalidation` config to `CacheProperties` + validation | 0 | 2 | 0 |
| P2-2e | Integration test: cross-service invalidation with Testcontainers | 0 | 0 | 1 integration |

### Phase 4: P3 Extensions (parallel-safe)

| Task ID | Task | New Files | Modified Files | Tests |
|---------|------|-----------|----------------|-------|
| P3-1a | Create `CacheCompressor` interface + `GzipCacheCompressor` | 2 | 0 | 1 unit + 1 PBT |
| P3-1b | Create `CompressingSerializer` | 1 | 0 | 1 unit + 1 PBT |
| P3-1c | Modify `SerializerFactory` to wrap with `CompressingSerializer` | 0 | 1 | 0 |
| P3-1d | Add `Compression` config to `CacheProperties` + validation | 0 | 2 | 0 |
| P3-1e | Integration test: compression with real Redis | 0 | 0 | 1 integration |
| P3-2a | Create `ScheduledEvictionRule` + `ScheduledEvictionExecutor` | 2 | 0 | 1 unit |
| P3-2b | Create `ScheduledEvictionAutoConfiguration` | 1 | 0 | 1 config test |
| P3-2c | Add `Eviction.Scheduled` config to `CacheProperties` + cron validation | 0 | 2 | 0 |
| P3-2d | Integration test: scheduled eviction with Testcontainers | 0 | 0 | 1 integration |

---

## Summary Metrics

| Metric | Count |
|--------|-------|
| New Java files | ~30 |
| Modified existing files | ~8 unique (Properties, AutoConfig, CacheAspect, SerializerFactory, pom.xml) |
| New test files | ~28 (unit + PBT + integration + config) |
| New Maven dependencies | 1 optional (`spring-boot-starter-actuator`) |
| New config property groups | 8 |
| Total tasks | 33 |

---

## Conflict Avoidance Notes

1. **CacheAspect.java** is modified by P1-1 (refresh) and P1-2 (hot key). If built in parallel, changes must be merged carefully — P1-1 adds a post-HIT check, P1-2 adds a pre-GET check and post-GET counter. These touch different code paths within `handleCacheable()`.

2. **CacheProperties.java** is modified by all 8 extensions. Each adds an independent nested class — these are structurally non-conflicting (append-only).

3. **CacheAutoConfiguration.java** modifications are also append-only (new `@Import`, new validation blocks, new log lines).

4. **SerializerFactory.java** is modified only by P3-1 (compression). No conflict risk.

5. **pom.xml** is modified only by P0-2 (actuator dependency). No conflict risk.
