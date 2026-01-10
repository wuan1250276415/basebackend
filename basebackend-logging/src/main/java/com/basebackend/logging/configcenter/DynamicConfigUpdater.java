// package com.basebackend.logging.configcenter;

// import com.alibaba.nacos.api.config.listener.Listener;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.core.env.ConfigurableEnvironment;
// import org.springframework.core.env.MapPropertySource;
// import org.springframework.core.env.PropertySource;
// import org.springframework.stereotype.Component;

// import java.util.Map;
// import java.util.concurrent.ConcurrentHashMap;
// import java.util.concurrent.locks.ReadWriteLock;
// import java.util.concurrent.locks.ReentrantReadWriteLock;

// /**
// * 动态配置更新器
// *
// * 负责监听配置中心的变化并动态更新系统配置：
// * - 监听配置变化
// * - 动态更新属性
// * - 配置版本管理
// * - 变更通知
// *
// * @author basebackend team
// * @since 2025-11-22
// */
// @Slf4j
// @Component
// public class DynamicConfigUpdater {

// @Autowired(required = false)
// private NacosConfigManager nacosConfigManager;

// @Autowired
// private ConfigurableEnvironment environment;

// private final ReadWriteLock lock = new ReentrantReadWriteLock();
// private final ConcurrentHashMap<String, String> configVersions = new
// ConcurrentHashMap<>();

// /**
// * 初始化动态配置
// */
// public void init() {
// if (nacosConfigManager == null) {
// log.warn("NacosConfigManager not available, dynamic config update disabled");
// return;
// }

// log.info("初始化动态配置更新器");

// // 监听日志配置变化
// registerConfigListener("logging-appender", "basebackend",
// this::updateLoggingConfig);

// // 监听统计配置变化
// registerConfigListener("statistics-config", "basebackend",
// this::updateStatisticsConfig);

// // 监听缓存配置变化
// registerConfigListener("cache-config", "basebackend",
// this::updateCacheConfig);

// // 监听监控配置变化
// registerConfigListener("monitoring-config", "basebackend",
// this::updateMonitoringConfig);

// log.info("动态配置更新器初始化完成");
// }

// /**
// * 注册配置监听器
// *
// * @param dataId 配置 ID
// * @param group 分组
// * @param handler 配置变更处理器
// */
// public void registerConfigListener(String dataId, String group,
// ConfigChangeHandler handler) {
// log.info("注册配置监听器: dataId={}, group={}", dataId, group);

// Listener listener = NacosConfigManager.newListener()
// .dataId(dataId)
// .group(group)
// .onChange(() -> handleConfigChange(dataId, group, handler))
// .build();

// nacosConfigManager.addListener(dataId, group, listener);

// // 立即加载一次配置
// loadAndUpdateConfig(dataId, group, handler);
// }

// /**
// * 动态更新系统属性
// *
// * @param key 属性键
// * @param value 属性值
// * @param source 配置来源
// */
// public void updateProperty(String key, String value, String source) {
// lock.writeLock().lock();
// try {
// String oldValue = environment.getProperty(key);
// if (value != null) {
// environment.getSystemProperties().put(key, value);
// log.info("动态更新属性: {}={} (source: {}, old: {})",
// key, value, source, oldValue);
// } else {
// environment.getSystemProperties().remove(key);
// log.info("移除属性: {} (source: {}, old: {})", key, source, oldValue);
// }
// } finally {
// lock.writeLock().unlock();
// }
// }

// /**
// * 批量更新属性
// *
// * @param properties 属性映射
// * @param source 配置来源
// */
// public void updateProperties(Map<String, String> properties, String source) {
// if (properties == null || properties.isEmpty()) {
// return;
// }

// lock.writeLock().lock();
// try {
// properties.forEach((key, value) -> {
// if (value != null) {
// environment.getSystemProperties().put(key, value);
// } else {
// environment.getSystemProperties().remove(key);
// }
// });
// log.info("批量更新属性完成: {} 个属性 (source: {})",
// properties.size(), source);
// } finally {
// lock.writeLock().unlock();
// }
// }

// /**
// * 获取属性值
// *
// * @param key 属性键
// * @return 属性值
// */
// public String getProperty(String key) {
// lock.readLock().lock();
// try {
// return environment.getProperty(key);
// } finally {
// lock.readLock().unlock();
// }
// }

// /**
// * 检查配置是否有变化
// *
// * @param dataId 配置 ID
// * @param group 分组
// * @param content 配置内容
// * @return 是否有变化
// */
// private boolean hasConfigChanged(String dataId, String group, String content)
// {
// String versionKey = dataId + ":" + group;
// String currentVersion = configVersions.get(versionKey);
// String newVersion = String.valueOf(content.hashCode());

// if (currentVersion == null) {
// configVersions.put(versionKey, newVersion);
// return true;
// }

// return !currentVersion.equals(newVersion);
// }

// /**
// * 更新配置版本
// *
// * @param dataId 配置 ID
// * @param group 分组
// * @param content 配置内容
// */
// private void updateConfigVersion(String dataId, String group, String content)
// {
// String versionKey = dataId + ":" + group;
// String version = String.valueOf(content.hashCode());
// configVersions.put(versionKey, version);
// log.debug("更新配置版本: {} -> {}", versionKey, version);
// }

// /**
// * 加载并更新配置
// *
// * @param dataId 配置 ID
// * @param group 分组
// * @param handler 配置变更处理器
// */
// private void loadAndUpdateConfig(String dataId, String group,
// ConfigChangeHandler handler) {
// try {
// String content = nacosConfigManager.getConfig(dataId, group);
// if (content != null && !content.trim().isEmpty()) {
// if (hasConfigChanged(dataId, group, content)) {
// updateConfigVersion(dataId, group, content);
// handler.handle(content);
// }
// }
// } catch (Exception e) {
// log.error("加载配置失败: dataId={}, group={}", dataId, group, e);
// }
// }

// /**
// * 处理配置变更
// *
// * @param dataId 配置 ID
// * @param group 分组
// * @param handler 配置变更处理器
// */
// private void handleConfigChange(String dataId, String group,
// ConfigChangeHandler handler) {
// try {
// String content = nacosConfigManager.getConfig(dataId, group);
// if (content != null) {
// if (hasConfigChanged(dataId, group, content)) {
// log.info("检测到配置变更: dataId={}, group={}", dataId, group);
// updateConfigVersion(dataId, group, content);
// handler.handle(content);
// } else {
// log.debug("配置未变化: dataId={}, group={}", dataId, group);
// }
// }
// } catch (Exception e) {
// log.error("处理配置变更失败: dataId={}, group={}", dataId, group, e);
// }
// }

// // ==================== 配置变更处理器 ====================

// /**
// * 更新日志配置
// */
// private void updateLoggingConfig(String content) {
// try {
// Map<String, String> properties = parseProperties(content);
// updateProperties(properties, "nacos:logging-config");
// log.info("日志配置更新完成");
// } catch (Exception e) {
// log.error("更新日志配置失败", e);
// }
// }

// /**
// * 更新统计配置
// */
// private void updateStatisticsConfig(String content) {
// try {
// Map<String, String> properties = parseProperties(content);
// updateProperties(properties, "nacos:statistics-config");
// log.info("统计配置更新完成");
// } catch (Exception e) {
// log.error("更新统计配置失败", e);
// }
// }

// /**
// * 更新缓存配置
// */
// private void updateCacheConfig(String content) {
// try {
// Map<String, String> properties = parseProperties(content);
// updateProperties(properties, "nacos:cache-config");
// log.info("缓存配置更新完成");
// } catch (Exception e) {
// log.error("更新缓存配置失败", e);
// }
// }

// /**
// * 更新监控配置
// */
// private void updateMonitoringConfig(String content) {
// try {
// Map<String, String> properties = parseProperties(content);
// updateProperties(properties, "nacos:monitoring-config");
// log.info("监控配置更新完成");
// } catch (Exception e) {
// log.error("更新监控配置失败", e);
// }
// }

// /**
// * 解析配置内容为属性映射
// *
// * @param content 配置内容
// * @return 属性映射
// */
// private Map<String, String> parseProperties(String content) {
// Map<String, String> properties = new ConcurrentHashMap<>();
// String[] lines = content.split("\\r?\\n");

// for (String line : lines) {
// line = line.trim();
// if (line.isEmpty() || line.startsWith("#") || line.startsWith("!")) {
// continue;
// }

// int idx = line.indexOf('=');
// if (idx > 0) {
// String key = line.substring(0, idx).trim();
// String value = line.substring(idx + 1).trim();
// properties.put(key, value);
// }
// }

// return properties;
// }

// /**
// * 配置变更处理器
// */
// @FunctionalInterface
// public interface ConfigChangeHandler {
// void handle(String content);
// }
// }
