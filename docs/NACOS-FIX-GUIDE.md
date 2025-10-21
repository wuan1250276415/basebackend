# Nacos配置修复说明

## 问题描述

在启动Nacos集群时遇到`db.num is null`错误，这是因为：
1. MySQL连接配置错误（使用了`localhost`而不是Docker网络中的容器名）
2. 缺少必要的数据库初始化脚本

## 已修复内容

### 1. 修复docker-compose.yml配置

**修改前**:
```yaml
- MYSQL_SERVICE_HOST=localhost
- MYSQL_SERVICE_PORT=3307
```

**修改后**:
```yaml
- MYSQL_SERVICE_HOST=nacos-mysql  # 使用Docker网络中的MySQL容器名
- MYSQL_SERVICE_PORT=3306          # 使用容器内部端口
- MYSQL_SERVICE_DB_PARAM=characterEncoding=utf8&connectTimeout=10000&socketTimeout=30000&autoReconnect=true&useSSL=false&allowPublicKeyRetrieval=true
```

### 2. 创建Nacos数据库初始化脚本

已创建`docker/nacos/mysql/init/01-nacos-schema.sql`，包含：
- config_info表
- config_info_aggr表
- config_info_beta表
- config_info_tag表
- config_tags_relation表
- group_capacity表
- his_config_info表
- tenant_capacity表
- tenant_info表
- users表（默认用户：nacos/nacos）
- roles表
- permissions表

## 重新启动步骤

### 方法1：使用脚本（需要sudo或docker组权限）

```bash
cd docker/nacos

# 停止并删除旧容器和数据
sudo docker compose down -v

# 重新启动
sudo bash start.sh
```

### 方法2：手动启动（适用于没有sudo权限的情况）

如果当前用户不在docker组中,需要添加用户到docker组：

```bash
# 1. 添加用户到docker组（需要sudo）
sudo usermod -aG docker $USER

# 2. 重新登录或刷新组权限
newgrp docker

# 3. 验证docker权限
docker ps

# 4. 停止并清理现有容器
docker compose -f docker/nacos/docker-compose.yml down -v

# 5. 重新启动
cd docker/nacos
bash start.sh
```

### 方法3：直接使用sudo（临时解决方案）

```bash
cd docker/nacos

# 停止并删除旧容器和数据
sudo docker compose down -v

# 启动服务
sudo docker compose up -d

# 等待服务启动
sleep 15

# 检查服务状态
sudo docker compose ps

# 查看Nacos日志（验证是否成功连接数据库）
sudo docker compose logs nacos1 | grep -i "database"
```

## 验证启动成功

### 1. 检查容器状态
```bash
sudo docker compose ps
```

应该看到4个容器都是UP状态：
- nacos-mysql
- nacos1
- nacos2
- nacos3

### 2. 检查MySQL数据库
```bash
# 连接MySQL
docker exec -it nacos-mysql mysql -unacos -pnacos123 nacos_config

# 查看表
show tables;

# 应该看到14个Nacos表
```

### 3. 访问Nacos控制台

浏览器访问：
- Node 1: http://localhost:8848/nacos
- Node 2: http://localhost:8849/nacos
- Node 3: http://localhost:8850/nacos

默认账号：`nacos / nacos`

### 4. 检查日志

```bash
# 查看nacos1日志
sudo docker compose logs -f nacos1

# 应该看到类似输出：
# Nacos started successfully in cluster mode
# 没有数据库连接错误
```

## 配置文件位置

- docker-compose配置: `docker/nacos/docker-compose.yml`
- MySQL初始化脚本: `docker/nacos/mysql/init/01-nacos-schema.sql`
- Nacos应用配置: `docker/nacos/nacos/application.properties`
- MySQL配置: `docker/nacos/mysql/my.cnf`

## 关键配置说明

### Docker网络配置
在Docker Compose环境中，各容器通过内部网络通信：
- `nacos-mysql`: MySQL容器的主机名
- 内部端口: 3306（容器内）
- 外部端口: 3307（主机访问）

### 环境变量
```yaml
MYSQL_SERVICE_HOST=nacos-mysql        # MySQL容器名（重要！）
MYSQL_SERVICE_PORT=3306               # 容器内部端口
MYSQL_SERVICE_DB_NAME=nacos_config    # 数据库名
MYSQL_SERVICE_USER=nacos              # 数据库用户
MYSQL_SERVICE_PASSWORD=nacos123       # 数据库密码
```

## 常见问题排查

### 1. 仍然报db.num错误
```bash
# 查看Nacos容器日志
sudo docker logs nacos1

# 检查环境变量是否正确
sudo docker inspect nacos1 | grep MYSQL
```

### 2. MySQL连接失败
```bash
# 测试从nacos1连接MySQL
sudo docker exec nacos1 ping nacos-mysql

# 检查MySQL是否启动成功
sudo docker exec nacos-mysql mysql -unacos -pnacos123 -e "SELECT 1"
```

### 3. 数据库表未创建
```bash
# 重新初始化数据库
sudo docker compose down -v
sudo docker compose up -d
```

## 修复文件清单

✅ `docker/nacos/docker-compose.yml` - 修复MySQL连接配置
✅ `docker/nacos/mysql/init/01-nacos-schema.sql` - 创建Nacos数据库表结构

## 下一步

1. 确保Docker权限配置正确（添加用户到docker组）
2. 停止现有容器: `sudo docker compose down -v`
3. 重新启动: `cd docker/nacos && sudo bash start.sh`
4. 验证启动成功后访问控制台: http://localhost:8848/nacos

如果仍有问题，请查看容器日志：`sudo docker compose logs -f nacos1`
