# Base Backend 快速开始指南

> 5 分钟快速启动 Base Backend 开发环境

## 🚀 一键启动

### 前置条件

- Docker 24.0+
- JDK 17+
- Maven 3.8+

### 启动步骤

```bash
# 1. 克隆项目
git clone https://github.com/your-org/basebackend.git
cd basebackend

# 2. 启动基础设施（MySQL, Redis, Nacos, RocketMQ）
cd docker/compose
./start-all.sh

# 3. 等待服务启动（约90秒）
# 可以在另一个终端查看状态
docker ps | grep basebackend

# 4. 导入 Nacos 配置
cd ../../config/nacos-configs
./import-nacos-configs.sh  # Linux/Mac
# 或
.\import-nacos-configs.ps1  # Windows PowerShell

# 5. 编译项目
cd ../..
mvn clean install -DskipTests

# 6. 启动 Gateway
cd basebackend-gateway
mvn spring-boot:run

# 7. 在新终端启动 Admin API
cd basebackend-admin-api
mvn spring-boot:run
```

## ✅ 验证部署

### 检查服务状态

```bash
# 使用健康检查脚本
./bin/maintenance/health-check.sh

# 或手动检查
curl http://localhost:8080/actuator/health  # Gateway
curl http://localhost:8081/actuator/health  # Admin API
```

### 访问服务

| 服务 | 地址 | 账号密码 |
|-----|------|---------|
| **API 文档** | http://localhost:8080/doc.html | - |
| **Nacos 控制台** | http://localhost:8848/nacos | nacos/nacos |
| **RocketMQ 控制台** | http://localhost:8180 | - |
| **Gateway** | http://localhost:8080 | - |
| **Admin API** | http://localhost:8081 | - |

### 测试 API

```bash
# 测试登录接口
curl -X POST http://localhost:8080/api/admin/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 测试用户列表（需要先登录获取 token）
curl http://localhost:8080/api/users \
  -H "Authorization: Bearer YOUR_TOKEN"
```

## 📚 下一步

### 开发指南

- [开发入门](docs/development/getting-started.md) - 完整的开发指南
- [编码规范](docs/development/getting-started.md#编码规范) - 代码规范
- [API 文档](http://localhost:8080/doc.html) - 在线 API 文档

### 部署指南

- [Docker 部署](docs/deployment/docker/quick-start.md) - Docker 详细部署
- [部署架构](docs/deployment/README.md) - 完整部署文档

### 架构文档

- [项目结构](README.md#项目结构) - 项目目录说明
- [架构设计](docs/architecture/) - 架构设计文档
- [模块说明](README.md#核心功能) - 各模块功能说明

## 🔧 常见问题

### Q1: 端口被占用

**错误**: `Bind for 0.0.0.0:3306 failed: port is already allocated`

**解决方案**:
```bash
# 修改端口配置
vim docker/compose/env/.env.dev
# 修改对应端口，例如: MYSQL_PORT=3307
```

### Q2: 服务无法启动

**错误**: 服务启动失败或健康检查失败

**解决方案**:
```bash
# 查看服务日志
docker logs basebackend-mysql
docker logs basebackend-nacos

# 重启服务
cd docker/compose
./stop-all.sh
./start-all.sh
```

### Q3: Maven 依赖下载慢

**解决方案**: 配置阿里云镜像

编辑 `~/.m2/settings.xml`:
```xml
<mirrors>
    <mirror>
        <id>aliyun</id>
        <mirrorOf>central</mirrorOf>
        <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
</mirrors>
```

### Q4: 无法连接到 Nacos

**解决方案**:
```bash
# 检查 Nacos 是否完全启动
docker logs basebackend-nacos

# 等待 Nacos 完全启动（约60秒）
curl http://localhost:8848/nacos/v1/console/health/readiness

# 如果返回 "UP"，说明 Nacos 已就绪
```

## 🆘 获取帮助

### 文档资源

- [完整文档](docs/) - 所有项目文档
- [故障排查](docs/troubleshooting/) - 常见问题解决
- [部署指南](docs/deployment/) - 详细部署文档

### 健康检查

```bash
# 运行健康检查脚本
./bin/maintenance/health-check.sh

# 查看详细状态
docker ps -a | grep basebackend
docker-compose -f docker/compose/base/docker-compose.base.yml ps
```

### 日志查看

```bash
# 应用日志
tail -f logs/info.log
tail -f logs/error.log

# Docker 日志
docker logs -f basebackend-mysql
docker logs -f basebackend-nacos
```

## 🎯 快速命令参考

```bash
# 启动所有基础设施
cd docker/compose && ./start-all.sh

# 停止所有基础设施
cd docker/compose && ./stop-all.sh

# 健康检查
./bin/maintenance/health-check.sh

# 编译项目
mvn clean install -DskipTests

# 启动 Gateway
cd basebackend-gateway && mvn spring-boot:run

# 启动 Admin API
cd basebackend-admin-api && mvn spring-boot:run

# 查看日志
docker logs -f basebackend-nacos

# 重启服务
docker restart basebackend-mysql
```

## 📊 系统要求

### 最小配置

- CPU: 4核
- 内存: 8GB
- 磁盘: 50GB
- 网络: 10Mbps

### 推荐配置

- CPU: 8核
- 内存: 16GB
- 磁盘: 100GB SSD
- 网络: 100Mbps

---

**祝你开发愉快！** 🎉

如有问题，请查看 [完整文档](docs/) 或提交 Issue。
