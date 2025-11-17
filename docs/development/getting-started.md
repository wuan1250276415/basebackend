# 开发入门指南

欢迎加入 Base Backend 项目！本指南将帮助你快速搭建开发环境并开始开发。

## 环境准备

### 必需工具

| 工具 | 版本要求 | 用途 |
|-----|---------|------|
| JDK | 17+ | Java 开发 |
| Maven | 3.8+ | 项目构建 |
| Docker | 24.0+ | 运行基础设施 |
| Git | 2.30+ | 版本控制 |
| IDE | - | 推荐 IntelliJ IDEA |

### 可选工具

| 工具 | 用途 |
|-----|------|
| Postman | API 测试 |
| DBeaver | 数据库管理 |
| Redis Desktop Manager | Redis 管理 |
| Another Redis Desktop Manager | Redis 管理 (开源) |

### 安装 JDK 17

**Windows**:
```powershell
# 使用 Chocolatey
choco install openjdk17

# 或下载安装包
# https://adoptium.net/
```

**Mac**:
```bash
# 使用 Homebrew
brew install openjdk@17
```

**Linux**:
```bash
# Ubuntu/Debian
sudo apt install openjdk-17-jdk

# CentOS/RHEL
sudo yum install java-17-openjdk-devel
```

### 安装 Maven

**Windows**:
```powershell
choco install maven
```

**Mac**:
```bash
brew install maven
```

**Linux**:
```bash
sudo apt install maven  # Ubuntu/Debian
sudo yum install maven  # CentOS/RHEL
```

## 项目结构

```
basebackend/
├── docs/                    # 📚 项目文档
│   ├── getting-started/     # 快速入门
│   ├── guides/              # 详细指南
│   ├── architecture/        # 架构设计
│   └── deployment/          # 部署文档
│
├── bin/                     # 🔧 脚本工具
│   ├── start/               # 启动脚本
│   ├── test/                # 测试脚本
│   └── maintenance/         # 运维脚本
│
├── docker/                  # 🐳 Docker 配置
│   └── compose/             # Docker Compose
│
├── config/                  # ⚙️ 配置文件
│   └── nacos-configs/       # Nacos 配置
│
└── basebackend-*/           # 📦 业务模块
    ├── basebackend-common   # 公共模块
    ├── basebackend-gateway  # API 网关
    ├── basebackend-admin-api # 管理后台 API
    └── ...                  # 其他模块
```

## 快速开始

### 1. 克隆项目

```bash
git clone https://github.com/your-org/basebackend.git
cd basebackend
```

### 2. 启动基础设施

```bash
cd docker/compose
./start-all.sh
```

等待约 90 秒，MySQL、Redis、Nacos、RocketMQ 将自动启动。

### 3. 导入 Nacos 配置

```bash
cd ../../config/nacos-configs

# Windows PowerShell
.\import-nacos-configs.ps1

# Linux/Mac
./import-nacos-configs.sh
```

### 4. 导入 IDEA 项目

1. 打开 IntelliJ IDEA
2. File → Open → 选择项目根目录
3. 等待 Maven 依赖下载完成（首次可能需要 10-15 分钟）
4. 配置 JDK 17
   - File → Project Structure → Project SDK → 选择 JDK 17

### 5. 配置运行配置

#### 启动 Gateway

1. 找到 `basebackend-gateway/src/main/java/com/basebackend/gateway/GatewayApplication.java`
2. 右键 → Run 'GatewayApplication'
3. 或创建 Spring Boot 运行配置:
   - Main class: `com.basebackend.gateway.GatewayApplication`
   - Working directory: `$MODULE_WORKING_DIR$`
   - Environment variables: `SPRING_PROFILES_ACTIVE=dev`

#### 启动 Admin API

1. 找到 `basebackend-admin-api/src/main/java/com/basebackend/admin/AdminApiApplication.java`
2. 右键 → Run 'AdminApiApplication'

### 6. 验证启动

```bash
# 测试 Gateway
curl http://localhost:8080/actuator/health

# 测试 Admin API
curl http://localhost:8081/actuator/health

# 访问 API 文档
open http://localhost:8080/doc.html
```

## 开发流程

### 1. 创建新分支

```bash
# 从 main 分支创建功能分支
git checkout main
git pull origin main
git checkout -b feature/your-feature-name
```

### 2. 开发新功能

#### 创建实体类

```java
// basebackend-admin-api/src/main/java/com/basebackend/admin/entity/User.java
@Data
@TableName("sys_user")
public class User extends BaseEntity {
    private String username;
    private String password;
    private String email;
    private String phone;
}
```

#### 创建 Mapper

```java
// basebackend-admin-api/src/main/java/com/basebackend/admin/mapper/UserMapper.java
@Mapper
public interface UserMapper extends BaseMapper<User> {
    // MyBatis-Plus 已提供基础 CRUD
    // 自定义查询方法
}
```

#### 创建 Service

```java
// basebackend-admin-api/src/main/java/com/basebackend/admin/service/UserService.java
public interface UserService {
    User getUserById(Long id);
    List<User> listUsers();
    void createUser(User user);
}

// basebackend-admin-api/src/main/java/com/basebackend/admin/service/impl/UserServiceImpl.java
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserMapper userMapper;
    
    @Override
    public User getUserById(Long id) {
        return userMapper.selectById(id);
    }
}
```

#### 创建 Controller

```java
// basebackend-admin-api/src/main/java/com/basebackend/admin/controller/UserController.java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    
    @GetMapping("/{id}")
    public Result<User> getUser(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return Result.success(user);
    }
}
```

### 3. 编写测试

```java
// basebackend-admin-api/src/test/java/com/basebackend/admin/service/UserServiceTest.java
@SpringBootTest
class UserServiceTest {
    @Autowired
    private UserService userService;
    
    @Test
    void testGetUserById() {
        User user = userService.getUserById(1L);
        assertNotNull(user);
    }
}
```

### 4. 运行测试

```bash
# 运行所有测试
mvn test

# 运行特定模块测试
mvn test -pl basebackend-admin-api

# 运行特定测试类
mvn test -Dtest=UserServiceTest
```

### 5. 提交代码

```bash
# 添加修改
git add .

# 提交（遵循提交规范）
git commit -m "feat(user): 添加用户管理功能"

# 推送到远程
git push origin feature/your-feature-name
```

## 编码规范

### 包命名规范

```
com.basebackend.{module}.{layer}

例如:
com.basebackend.admin.controller  # 控制器
com.basebackend.admin.service     # 服务层
com.basebackend.admin.mapper      # 数据访问层
com.basebackend.admin.entity      # 实体类
com.basebackend.admin.dto         # 数据传输对象
com.basebackend.admin.vo          # 视图对象
com.basebackend.admin.config      # 配置类
com.basebackend.admin.util        # 工具类
```

### 类命名规范

| 类型 | 命名规则 | 示例 |
|-----|---------|------|
| Controller | XxxController | UserController |
| Service | XxxService | UserService |
| ServiceImpl | XxxServiceImpl | UserServiceImpl |
| Mapper | XxxMapper | UserMapper |
| Entity | Xxx | User |
| DTO | XxxDTO | UserDTO |
| VO | XxxVO | UserVO |
| Config | XxxConfig | SecurityConfig |
| Util | XxxUtil | DateUtil |

### 方法命名规范

| 操作 | 命名规则 | 示例 |
|-----|---------|------|
| 查询单个 | getXxx | getUser |
| 查询列表 | listXxx | listUsers |
| 分页查询 | pageXxx | pageUsers |
| 创建 | createXxx | createUser |
| 更新 | updateXxx | updateUser |
| 删除 | deleteXxx | deleteUser |
| 批量操作 | batchXxx | batchDeleteUsers |

### Git 提交规范

```
<type>(<scope>): <subject>

type:
- feat: 新功能
- fix: 修复 bug
- docs: 文档更新
- style: 代码格式（不影响代码运行）
- refactor: 重构
- test: 测试
- chore: 构建过程或辅助工具

scope: 影响范围（模块名）
subject: 简短描述

示例:
feat(user): 添加用户管理功能
fix(auth): 修复登录超时问题
docs(readme): 更新部署文档
```

## 常用命令

### Maven 命令

```bash
# 清理编译
mvn clean

# 编译
mvn compile

# 打包
mvn package

# 安装到本地仓库
mvn install

# 跳过测试
mvn install -DskipTests

# 只编译特定模块
mvn compile -pl basebackend-admin-api

# 同时编译依赖模块
mvn compile -pl basebackend-admin-api -am

# 查看依赖树
mvn dependency:tree

# 检查依赖更新
mvn versions:display-dependency-updates
```

### Docker 命令

```bash
# 查看运行中的容器
docker ps

# 查看所有容器
docker ps -a

# 查看日志
docker logs -f basebackend-mysql

# 进入容器
docker exec -it basebackend-mysql bash

# 重启容器
docker restart basebackend-mysql

# 停止容器
docker stop basebackend-mysql

# 删除容器
docker rm basebackend-mysql

# 查看容器资源使用
docker stats
```

### Git 命令

```bash
# 查看状态
git status

# 查看分支
git branch

# 切换分支
git checkout branch-name

# 创建并切换分支
git checkout -b new-branch

# 拉取最新代码
git pull origin main

# 合并分支
git merge branch-name

# 查看提交历史
git log --oneline

# 撤销修改
git checkout -- file-name

# 重置到某个提交
git reset --hard commit-hash
```

## 调试技巧

### 1. 使用 IDEA 调试器

1. 在代码行号左侧点击设置断点
2. 以 Debug 模式启动应用
3. 触发断点，查看变量值
4. 使用 Step Over (F8) / Step Into (F7) 单步调试

### 2. 查看日志

```bash
# 应用日志位置
tail -f logs/info.log
tail -f logs/error.log

# 或在 IDEA 的 Run 窗口查看
```

### 3. 使用 Actuator 端点

```bash
# 健康检查
curl http://localhost:8080/actuator/health

# 查看所有端点
curl http://localhost:8080/actuator

# 查看环境变量
curl http://localhost:8080/actuator/env

# 查看配置
curl http://localhost:8080/actuator/configprops
```

### 4. 使用 Postman 测试 API

1. 导入 API 文档: http://localhost:8080/v3/api-docs
2. 创建测试集合
3. 设置环境变量
4. 编写测试脚本

## 常见问题

### Q1: Maven 依赖下载慢

**解决方案**: 配置国内镜像

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

### Q2: 端口被占用

**解决方案**: 修改端口或停止占用进程

```bash
# Windows 查找占用端口的进程
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Linux/Mac
lsof -i :8080
kill -9 <PID>
```

### Q3: 无法连接到 Nacos

**解决方案**: 检查 Nacos 状态

```bash
# 检查 Nacos 是否启动
docker ps | grep nacos

# 查看 Nacos 日志
docker logs basebackend-nacos

# 测试连接
curl http://localhost:8848/nacos/v1/console/health/readiness
```

### Q4: 数据库连接失败

**解决方案**: 检查 MySQL 状态和配置

```bash
# 检查 MySQL 是否启动
docker ps | grep mysql

# 测试连接
mysql -h 127.0.0.1 -P 3306 -u basebackend -pbasebackend123

# 检查配置
# application.yml 中的数据库配置是否正确
```

## 下一步

- 阅读 [架构设计文档](../architecture/)
- 查看 [API 文档](http://localhost:8080/doc.html)
- 了解 [最佳实践](best-practices.md)
- 参与 [代码审查](code-review.md)

## 获取帮助

- 查看 [常见问题](../troubleshooting/)
- 阅读 [故障排查指南](../troubleshooting/FRONTEND-TROUBLESHOOTING.md)
- 联系团队成员
- 提交 Issue

祝你开发愉快！🚀
