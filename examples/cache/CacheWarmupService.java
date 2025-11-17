package com.basebackend.examples.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 缓存预热服务
 * 在系统启动时预加载热点数据到缓存
 */
@Slf4j
@Service
public class CacheWarmupService implements CommandLineRunner {

    @Autowired
    private MultiLevelCache<String> userCache;

    @Autowired
    private MultiLevelCache<String> menuCache;

    @Autowired
    private MultiLevelCache<String> permissionCache;

    @Autowired
    private MultiLevelCache<String> dictCache;

    @Autowired
    private CacheProtector cacheProtector;

    private final ExecutorService warmupExecutor = Executors.newFixedThreadPool(4);

    /**
     * 系统启动时预热缓存
     */
    @PostConstruct
    public void init() {
        log.info("开始初始化缓存预热...");
        warmupCache();
    }

    @Override
    public void run(String... args) {
        log.info("CommandLineRunner: 缓存预热启动");
        // 已经在 @PostConstruct 中执行
    }

    /**
     * 执行缓存预热
     */
    public void warmupCache() {
        log.info("===========================================");
        log.info("       开始执行多级缓存预热");
        log.info("===========================================");

        long startTime = System.currentTimeMillis();

        try {
            // 并行预热多个缓存
            CompletableFuture.allOf(
                CompletableFuture.runAsync(this::warmupUserCache, warmupExecutor),
                CompletableFuture.runAsync(this::warmupMenuCache, warmupExecutor),
                CompletableFuture.runAsync(this::warmupPermissionCache, warmupExecutor),
                CompletableFuture.runAsync(this::warmupDictCache, warmupExecutor),
                CompletableFuture.runAsync(this::warmupHotDataCache, warmupExecutor)
            ).join();

            long duration = System.currentTimeMillis() - startTime;
            log.info("===========================================");
            log.info("       缓存预热完成，耗时: {}ms", duration);
            log.info("===========================================");

            // 输出缓存统计信息
            printCacheStatistics();

        } catch (Exception e) {
            log.error("缓存预热失败", e);
        } finally {
            warmupExecutor.shutdown();
        }
    }

    /**
     * 预热用户缓存
     */
    private void warmupUserCache() {
        log.info("开始预热用户缓存...");

        try {
            // 模拟加载用户数据
            List<String> users = loadUsersFromDatabase();
            log.info("从数据库加载用户: {} 条", users.size());

            // 初始化布隆过滤器
            cacheProtector.initBloomFilter("user_bloom", users.size() * 2, 0.01);

            // 预热用户缓存
            for (int i = 0; i < users.size(); i++) {
                String username = users.get(i);
                String key = "user:username:" + username;
                String value = "User{" + i + "}";

                // 写入缓存
                userCache.put(key, value, Duration.ofHours(2));

                // 添加到布隆过滤器
                cacheProtector.addToBloomFilter("user_bloom", key);

                // 每100条输出一次日志
                if ((i + 1) % 100 == 0) {
                    log.info("用户缓存预热进度: {}/{}", i + 1, users.size());
                }
            }

            log.info("用户缓存预热完成: {} 条", users.size());

        } catch (Exception e) {
            log.error("用户缓存预热失败", e);
        }
    }

    /**
     * 预热菜单缓存
     */
    private void warmupMenuCache() {
        log.info("开始预热菜单缓存...");

        try {
            // 模拟加载菜单数据
            List<String> menus = loadMenusFromDatabase();
            log.info("从数据库加载菜单: {} 条", menus.size());

            // 缓存菜单树
            String menuTreeKey = "menu:tree";
            String menuTreeValue = "MenuTree{" + menus.size() + "}";

            menuCache.put(menuTreeKey, menuTreeValue, Duration.ofHours(6));

            // 缓存每个菜单项
            for (int i = 0; i < menus.size(); i++) {
                String menu = menus.get(i);
                String key = "menu:item:" + i;
                String value = "Menu{" + i + "}";

                menuCache.put(key, value, Duration.ofHours(3));
            }

            log.info("菜单缓存预热完成: {} 条", menus.size());

        } catch (Exception e) {
            log.error("菜单缓存预热失败", e);
        }
    }

    /**
     * 预热权限缓存
     */
    private void warmupPermissionCache() {
        log.info("开始预热权限缓存...");

        try {
            // 模拟加载权限数据
            List<String> permissions = loadPermissionsFromDatabase();
            log.info("从数据库加载权限: {} 条", permissions.size());

            // 初始化权限布隆过滤器
            cacheProtector.initBloomFilter("permission_bloom", permissions.size() * 2, 0.01);

            // 预热权限缓存
            for (int i = 0; i < permissions.size(); i++) {
                String permission = permissions.get(i);
                String key = "permission:code:" + permission;
                String value = "Permission{" + i + "}";

                permissionCache.put(key, value, Duration.ofHours(2));

                // 添加到布隆过滤器
                cacheProtector.addToBloomFilter("permission_bloom", key);
            }

            log.info("权限缓存预热完成: {} 条", permissions.size());

        } catch (Exception e) {
            log.error("权限缓存预热失败", e);
        }
    }

    /**
     * 预热字典缓存
     */
    private void warmupDictCache() {
        log.info("开始预热字典缓存...");

        try {
            // 模拟加载字典数据
            List<String> dictTypes = loadDictTypesFromDatabase();
            log.info("从数据库加载字典类型: {} 条", dictTypes.size());

            // 预热字典缓存
            for (int i = 0; i < dictTypes.size(); i++) {
                String dictType = dictTypes.get(i);
                String key = "dict:type:" + dictType;
                String value = "DictValues{" + i + "}";

                dictCache.put(key, value, Duration.ofHours(12));
            }

            log.info("字典缓存预热完成: {} 条", dictTypes.size());

        } catch (Exception e) {
            log.error("字典缓存预热失败", e);
        }
    }

    /**
     * 预热热点数据缓存
     */
    private void warmupHotDataCache() {
        log.info("开始预热热点数据缓存...");

        try {
            // 模拟加载热点数据
            List<String> hotData = loadHotDataFromDatabase();
            log.info("从数据库加载热点数据: {} 条", hotData.size());

            // 预热热点数据
            for (int i = 0; i < hotData.size(); i++) {
                String data = hotData.get(i);
                String key = "hot:data:" + i;
                String value = "HotData{" + i + "}";

                // 缓存热点数据
                String cacheKey = "hotData:" + key;
                // 这里应该使用实际的热点数据缓存实例
                // multiLevelCache.put(cacheKey, value, Duration.ofMinutes(5));
            }

            log.info("热点数据缓存预热完成: {} 条", hotData.size());

        } catch (Exception e) {
            log.error("热点数据缓存预热失败", e);
        }
    }

    /**
     * 打印缓存统计信息
     */
    private void printCacheStatistics() {
        log.info("===========================================");
        log.info("            缓存统计信息");
        log.info("===========================================");

        // 用户缓存统计
        var userStats = userCache.getStatistics();
        log.info("用户缓存 - 大小: {}, 命中率: {:.2%}",
            userStats.getL1Size(),
            userStats.getOverallHitRate());

        // 菜单缓存统计
        var menuStats = menuCache.getStatistics();
        log.info("菜单缓存 - 大小: {}, 命中率: {:.2%}",
            menuStats.getL1Size(),
            menuStats.getOverallHitRate());

        // 权限缓存统计
        var permissionStats = permissionCache.getStatistics();
        log.info("权限缓存 - 大小: {}, 命中率: {:.2%}",
            permissionStats.getL1Size(),
            permissionStats.getOverallHitRate());

        // 字典缓存统计
        var dictStats = dictCache.getStatistics();
        log.info("字典缓存 - 大小: {}, 命中率: {:.2%}",
            dictStats.getL1Size(),
            dictStats.getOverallHitRate());

        log.info("===========================================");
    }

    // ========================================
    // 模拟数据加载方法（实际项目中应该调用数据库）
    // ========================================

    private List<String> loadUsersFromDatabase() {
        log.debug("从数据库加载用户数据...");
        // 模拟加载1000个用户
        return java.util.stream.IntStream.rangeClosed(1, 1000)
            .mapToObj(i -> "user" + String.format("%04d", i))
            .collect(java.util.stream.Collectors.toList());
    }

    private List<String> loadMenusFromDatabase() {
        log.debug("从数据库加载菜单数据...");
        // 模拟加载100个菜单
        return java.util.stream.IntStream.rangeClosed(1, 100)
            .mapToObj(i -> "menu" + String.format("%03d", i))
            .collect(java.util.stream.Collectors.toList());
    }

    private List<String> loadPermissionsFromDatabase() {
        log.debug("从数据库加载权限数据...");
        // 模拟加载500个权限
        return java.util.stream.IntStream.rangeClosed(1, 500)
            .mapToObj(i -> "permission" + String.format("%03d", i))
            .collect(java.util.stream.Collectors.toList());
    }

    private List<String> loadDictTypesFromDatabase() {
        log.debug("从数据库加载字典类型...");
        // 模拟加载20个字典类型
        return List.of(
            "gender", "status", "type", "category",
            "level", "priority", "state", "phase",
            "grade", "class", "dept", "role",
            "user_type", "menu_type", "operation_type",
            "log_type", "config_type", "dict_type",
            "flag", "yes_no"
        );
    }

    private List<String> loadHotDataFromDatabase() {
        log.debug("从数据库加载热点数据...");
        // 模拟加载200条热点数据
        return java.util.stream.IntStream.rangeClosed(1, 200)
            .mapToObj(i -> "hot" + String.format("%03d", i))
            .collect(java.util.stream.Collectors.toList());
    }
}
