[根目录](../../CLAUDE.md) > **basebackend-nacos**

# basebackend-nacos

## 模块职责

Nacos集成库。封装Nacos配置管理、服务发现、灰度发布能力，供业务模块使用。

## 对外接口

- `NacosConfigService`: 配置CRUD与监听
- `ServiceDiscoveryManager`: 服务发现管理
- `GrayReleaseService`: 灰度发布服务
- `NacosConfigProperties`: 配置属性

## 测试与质量

4个测试: ServiceInstanceTest, GrayReleaseServiceTest, NacosConfigServiceTest, ServiceDiscoveryManagerTest, NacosConfigPropertiesTest

## 变更记录

| 时间 | 操作 | 说明 |
|------|------|------|
| 2026-02-20 13:17:55 | 初始创建 | 全量扫描生成 |
