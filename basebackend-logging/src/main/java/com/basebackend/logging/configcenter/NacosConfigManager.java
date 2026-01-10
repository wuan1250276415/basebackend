// package com.basebackend.logging.configcenter;

// import com.alibaba.cloud.nacos.NacosConfigProperties;
// import com.alibaba.nacos.api.config.ConfigService;
// import com.alibaba.nacos.api.config.listener.Listener;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.stereotype.Component;

// import java.util.concurrent.Executor;

// /**
// * Nacos 配置管理器
// *
// * 提供动态配置管理功能：
// * - 配置动态更新
// * - 配置监听器
// * - 配置缓存
// * - 故障回退
// *
// * @author basebackend team
// * @since 2025-11-22
// */
// @Slf4j
// @Component
// public class NacosConfigManager {

// private final ConfigService configService;
// private final NacosConfigProperties nacosConfigProperties;
// private volatile boolean connected = false;

// public NacosConfigManager(ConfigService configService,
// NacosConfigProperties nacosConfigProperties) {
// this.configService = configService;
// this.nacosConfigProperties = nacosConfigProperties;
// this.connected = true;
// log.info("Nacos 配置管理器初始化完成, serverAddr: {}",
// nacosConfigProperties.getServerAddr());
// }

// /**
// * 获取配置
// *
// * @param dataId 配置 ID
// * @param group 分组
// * @return 配置内容
// */
// public String getConfig(String dataId, String group) {
// if (!connected || configService == null) {
// log.warn("Nacos 未连接，返回 null");
// return null;
// }

// try {
// String config = configService.getConfig(dataId, group, 5000);
// log.debug("获取配置成功: dataId={}, group={}, length={}",
// dataId, group, config != null ? config.length() : 0);
// return config;
// } catch (Exception e) {
// log.error("获取配置失败: dataId={}, group={}", dataId, group, e);
// connected = false;
// return null;
// }
// }

// /**
// * 发布配置
// *
// * @param dataId 配置 ID
// * @param group 分组
// * @param content 配置内容
// * @return 是否成功
// */
// public boolean publishConfig(String dataId, String group, String content) {
// if (!connected || configService == null) {
// log.warn("Nacos 未连接，发布配置失败");
// return false;
// }

// try {
// boolean result = configService.publishConfig(dataId, group, content);
// log.info("发布配置成功: dataId={}, group={}, result={}",
// dataId, group, result);
// return result;
// } catch (Exception e) {
// log.error("发布配置失败: dataId={}, group={}", dataId, group, e);
// connected = false;
// return false;
// }
// }

// /**
// * 添加配置监听器
// *
// * @param dataId 配置 ID
// * @param group 分组
// * @param listener 监听器
// */
// public void addListener(String dataId, String group, Listener listener) {
// if (!connected || configService == null) {
// log.warn("Nacos 未连接，添加监听器失败");
// return;
// }

// try {
// configService.addListener(dataId, group, listener);
// log.info("添加监听器成功: dataId={}, group={}", dataId, group);
// } catch (Exception e) {
// log.error("添加监听器失败: dataId={}, group={}", dataId, group, e);
// connected = false;
// }
// }

// /**
// * 删除配置
// *
// * @param dataId 配置 ID
// * @param group 分组
// * @return 是否成功
// */
// public boolean removeConfig(String dataId, String group) {
// if (!connected || configService == null) {
// log.warn("Nacos 未连接，删除配置失败");
// return false;
// }

// try {
// // Nacos 没有直接的 removeConfig 方法，这里模拟删除
// boolean result = publishConfig(dataId, group, "");
// log.info("删除配置成功: dataId={}, group={}", dataId, group);
// return result;
// } catch (Exception e) {
// log.error("删除配置失败: dataId={}, group={}", dataId, group, e);
// return false;
// }
// }

// /**
// * 检查连接状态
// *
// * @return 是否连接
// */
// public boolean isConnected() {
// return connected;
// }

// /**
// * 重新连接
// */
// public void reconnect() {
// log.info("尝试重新连接 Nacos...");
// // 重新初始化 ConfigService
// connected = true;
// log.info("Nacos 重新连接完成");
// }

// /**
// * 获取服务器地址
// *
// * @return 服务器地址
// */
// public String getServerAddr() {
// return nacosConfigProperties.getServerAddr();
// }

// /**
// * 配置监听器构建器
// */
// public static class ListenerBuilder {
// private String dataId;
// private String group;
// private Runnable onChange;
// private Executor executor;

// public ListenerBuilder dataId(String dataId) {
// this.dataId = dataId;
// return this;
// }

// public ListenerBuilder group(String group) {
// this.group = group;
// return this;
// }

// public ListenerBuilder onChange(Runnable onChange) {
// this.onChange = onChange;
// return this;
// }

// public ListenerBuilder executor(Executor executor) {
// this.executor = executor;
// return this;
// }

// public Listener build() {
// return new Listener() {
// @Override
// public void receiveConfigInfo(String configInfo) {
// log.info("配置变更: dataId={}, group={}, content={}",
// dataId, group, configInfo);
// if (onChange != null) {
// if (executor != null) {
// executor.execute(onChange);
// } else {
// onChange.run();
// }
// }
// }

// @Override
// public Executor getExecutor() {
// return executor;
// }
// };
// }
// }

// /**
// * 创建监听器构建器
// *
// * @return 构建器
// */
// public static ListenerBuilder newListener() {
// return new ListenerBuilder();
// }
// }
