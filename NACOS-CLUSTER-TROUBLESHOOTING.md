# Nacos集群启动故障排除指南

## 问题描述
启动Nacos集群时出现以下错误：
```
org.springframework.beans.factory.UnsatisfiedDependencyException: Error creating bean with name 'consoleHealthController' defined in URL [jar:nested:/home/nacos/target/nacos-server.jar/!BOOT-INF/lib/nacos-console-3.1.0.jar!/com/alibaba/nacos/console/controller/v3/ConsoleHealthController.class]: Unsatisfied dependency expressed through constructor parameter 0: Error creating bean with name 'healthProxy' defined in URL [jar:nested:/home/nacos/target/nacos-server.jar/!BOOT-INF/lib/nacos-console-3.1.0.jar!/com/alibaba/nacos/console/proxy/HealthProxy.class]: Unsatisfied dependency expressed through constructor parameter 0: No qualifying bean of type 'com.alibaba.nacos.console.handler.HealthHandler' available: expected at least 1 bean which qualifies as autowire candidate.
```

## 问题原因分析

1. **数据库连接配置不一致**：nacos1节点缺少`SPRING_DATASOURCE_PLATFORM=mysql`环境变量
2. **数据库连接地址错误**：application.properties中使用外部IP地址，Docker容器内应使用服务名
3. **数据库表结构缺失**：Nacos 3.1.0版本需要额外的健康检查相关表
4. **HealthHandler bean无法创建**：缺少必要的数据库表结构

## 修复方案

### 1. 修复Docker Compose配置
已为nacos1节点添加缺失的环境变量：
```yaml
- SPRING_DATASOURCE_PLATFORM=mysql
```

### 2. 修复数据库连接地址
将application.properties中的数据库连接地址从：
```
db.url.0=jdbc:mysql://192.168.66.20:3307/nacos_config?...
```
修改为：
```
db.url.0=jdbc:mysql://nacos-mysql:3306/nacos_config?...
```

### 3. 添加必要的数据库表
创建了以下补充脚本：
- `02-add-gray-tables.sql` - 添加灰度发布相关表
- `03-health-handler-fix.sql` - 添加健康检查处理器相关表

### 4. 使用修复脚本
运行修复脚本：
```bash
./fix-nacos-cluster.sh
```

## 验证步骤

1. **检查容器状态**：
   ```bash
   docker compose -f docker/nacos/docker-compose.yml ps
   ```

2. **检查Nacos日志**：
   ```bash
   docker compose -f docker/nacos/docker-compose.yml logs nacos1
   ```

3. **访问Nacos控制台**：
   - Node 1: http://localhost:8848/nacos
   - Node 2: http://localhost:8849/nacos
   - Node 3: http://localhost:8850/nacos

4. **检查集群状态**：
   登录控制台后，在"集群管理"中查看节点状态

## 常见问题

### 问题1：容器启动失败
**解决方案**：
- 检查Docker权限：`sudo usermod -aG docker $USER`
- 重新登录或重启终端

### 问题2：数据库连接失败
**解决方案**：
- 检查MySQL容器是否正常启动
- 验证数据库连接配置
- 检查网络连接

### 问题3：HealthHandler bean仍然无法创建
**解决方案**：
- 确保数据库初始化脚本已执行
- 检查数据库表是否创建成功
- 重启Nacos容器

### 问题4：集群节点无法互相发现
**解决方案**：
- 检查网络配置
- 验证NACOS_SERVERS环境变量
- 确保端口映射正确

## 预防措施

1. **统一配置**：确保所有Nacos节点使用相同的配置
2. **版本兼容性**：使用兼容的Nacos版本和数据库版本
3. **网络配置**：确保容器间网络通信正常
4. **资源限制**：为容器分配足够的内存和CPU资源

## 监控和日志

1. **查看实时日志**：
   ```bash
   docker compose -f docker/nacos/docker-compose.yml logs -f nacos1
   ```

2. **检查健康状态**：
   ```bash
   curl http://localhost:8848/nacos/v1/console/health
   ```

3. **监控资源使用**：
   ```bash
   docker stats nacos1 nacos2 nacos3
   ```

## 联系支持

如果问题仍然存在，请提供以下信息：
1. 完整的错误日志
2. 容器状态输出
3. 数据库连接测试结果
4. 网络配置信息
