# Nacos Docker部署指南

## 概述

本目录包含Nacos集群（3节点）+ MySQL的Docker部署配置。

## 架构

- **Nacos集群**: 3个Nacos节点 (nacos1, nacos2, nacos3)
- **MySQL**: 持久化存储Nacos配置数据
- **网络**: 独立的bridge网络 (nacos-network)

## 快速启动

### 1. 启动集群

```bash
cd docker/nacos
./start.sh
```

### 2. 访问Nacos控制台

打开浏览器访问任意节点：
- Node 1: http://localhost:8848/nacos
- Node 2: http://localhost:8849/nacos
- Node 3: http://localhost:8850/nacos

默认账号：`nacos` / `nacos`

### 3. 停止集群

```bash
./stop.sh
```

## 配置说明

### 端口映射

| 服务 | 容器端口 | 主机端口 |
|------|---------|---------|
| nacos1 | 8848 | 8848 |
| nacos1 (gRPC) | 9848 | 9848 |
| nacos2 | 8848 | 8849 |
| nacos2 (gRPC) | 9848 | 9849 |
| nacos3 | 8848 | 8850 |
| nacos3 (gRPC) | 9848 | 9850 |
| MySQL | 3306 | 3307 |

### MySQL配置

- **数据库**: nacos_config
- **用户**: nacos
- **密码**: nacos123
- **Root密码**: root123

数据持久化目录：`./mysql/data`

### JVM配置

每个Nacos节点默认配置：
- Xms: 512m
- Xmx: 512m

如需调整，修改docker-compose.yml中的`JVM_XMS`和`JVM_XMX`环境变量。

## 应用接入配置

在应用的`bootstrap.yml`中配置：

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848,localhost:8849,localhost:8850
        namespace: ${NACOS_NAMESPACE:public}
        username: nacos
        password: nacos
      config:
        server-addr: localhost:8848,localhost:8849,localhost:8850
        namespace: ${NACOS_NAMESPACE:public}
        username: nacos
        password: nacos
        file-extension: yml
```

## 命名空间管理

### 创建命名空间

1. 登录Nacos控制台
2. 进入"命名空间"页面
3. 点击"新建命名空间"
4. 输入命名空间ID和名称：
   - `dev` - 开发环境
   - `test` - 测试环境
   - `prod` - 生产环境

### 使用命名空间

通过环境变量指定：
```bash
export NACOS_NAMESPACE=dev
```

或在配置文件中指定：
```yaml
spring:
  cloud:
    nacos:
      discovery:
        namespace: dev
```

## 集群管理

### 查看节点状态

```bash
docker compose ps
```

### 查看节点日志

```bash
# 查看nacos1日志
docker compose logs -f nacos1

# 查看所有Nacos节点日志
docker compose logs -f nacos1 nacos2 nacos3
```

### 重启单个节点

```bash
docker compose restart nacos1
```

## 故障排查

### 1. Nacos启动失败

**检查MySQL连接**：
```bash
docker compose logs nacos-mysql
```

确保MySQL已完全启动并初始化完成。

**检查Nacos日志**：
```bash
tail -f nacos1/logs/nacos.log
```

### 2. 无法访问控制台

**检查容器状态**：
```bash
docker compose ps
```

**检查端口占用**：
```bash
netstat -tulpn | grep 8848
```

### 3. 集群节点无法通信

**检查网络**：
```bash
docker network inspect nacos_nacos-network
```

**测试节点间连通性**：
```bash
docker exec nacos1 ping nacos2
```

## 数据备份

### 备份MySQL数据

```bash
docker exec nacos-mysql mysqldump -unacos -pnacos123 nacos_config > backup.sql
```

### 恢复MySQL数据

```bash
docker exec -i nacos-mysql mysql -unacos -pnacos123 nacos_config < backup.sql
```

## 性能优化

### 1. 增加JVM内存

修改`docker-compose.yml`：
```yaml
environment:
  - JVM_XMS=1g
  - JVM_XMX=2g
```

### 2. MySQL性能调优

编辑`mysql/my.cnf`，添加：
```ini
innodb_buffer_pool_size=512M
innodb_log_file_size=256M
```

### 3. 使用SSD存储

将`./mysql/data`目录挂载到SSD磁盘。

## 安全建议

### 1. 修改默认密码

在Nacos控制台中修改`nacos`用户密码。

### 2. 修改Auth Token

编辑`docker-compose.yml`，修改`NACOS_AUTH_TOKEN`为随机字符串（至少32字符）。

### 3. 限制网络访问

使用防火墙限制Nacos端口的访问来源。

## 升级指南

### 1. 备份数据

```bash
docker exec nacos-mysql mysqldump -unacos -pnacos123 nacos_config > backup.sql
```

### 2. 停止服务

```bash
./stop.sh
```

### 3. 修改镜像版本

编辑`docker-compose.yml`，修改image版本号。

### 4. 启动新版本

```bash
./start.sh
```

## 常用命令

```bash
# 启动集群
./start.sh

# 停止集群
./stop.sh

# 重启集群
./stop.sh && ./start.sh

# 查看日志
docker compose logs -f

# 进入Nacos容器
docker exec -it nacos1 bash

# 查看MySQL数据
docker exec -it nacos-mysql mysql -unacos -pnacos123 nacos_config
```

## 参考资料

- [Nacos官方文档](https://nacos.io/zh-cn/docs/quick-start-docker.html)
- [Nacos集群部署](https://nacos.io/zh-cn/docs/cluster-mode-quick-start.html)
- [Nacos Docker镜像](https://hub.docker.com/r/nacos/nacos-server)
