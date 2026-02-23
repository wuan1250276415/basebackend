# BaseBackend Scheduler - 实施总结报告

## 项目概述

**项目名称**: basebackend-scheduler 微服务模块
**技术栈**: Spring Boot 3.1.5 + Camunda 7.21 + PowerJob 4.3.8
**版本**: 1.0.0-SNAPSHOT
**实施日期**: 2025-01-01

## 功能特性

### 核心功能

1. **工作流引擎 (Camunda 7.21)**
   - BPMN 2.0 流程定义与执行
   - DMN 决策引擎
   - CMMN 案例管理
   - 流程任务管理
   - 历史数据追踪

2. **任务调度 (PowerJob 4.3.8)**
   - 分布式任务调度
   - 延迟任务队列
   - Cron 表达式支持
   - 任务依赖管理
   - 故障转移与重试

3. **表单模板管理**
   - 动态表单设计
   - 版本管理
   - 表单与流程关联
   - 模板使用统计

4. **统计分析**
   - 流程实例统计
   - 任务效率分析
   - 性能指标监控
   - 历史数据查询

## 项目架构

### 模块结构

```
basebackend-scheduler
├── src/main/java/com/basebackend/scheduler/
│   ├── camunda/                    # Camunda 工作流模块
│   │   ├── controller/             # 6 个控制器
│   │   │   ├── ProcessDefinitionController.java  # 流程定义
│   │   │   ├── ProcessInstanceController.java    # 流程实例
│   │   │   ├── TaskController.java               # 任务管理
│   │   │   ├── HistoricProcessInstanceController.java  # 历史查询
│   │   │   ├── ProcessStatisticsController.java  # 统计分析
│   │   │   └── FormTemplateController.java       # 表单模板
│   │   ├── service/               # 服务层
│   │   │   ├── ProcessDefinitionService.java
│   │   │   ├── ProcessInstanceService.java
│   │   │   ├── TaskManagementService.java
│   │   │   ├── HistoricProcessInstanceService.java
│   │   │   ├── ProcessStatisticsService.java
│   │   │   └── FormTemplateService.java
│   │   ├── delegate/              # JavaDelegate (4 个)
│   │   │   ├── SendEmailDelegate.java            # 邮件发送
│   │   │   ├── DataSyncDelegate.java             # 数据同步
│   │   │   ├── OrderApprovalDelegate.java        # 订单审批
│   │   │   └── MicroserviceCallDelegate.java     # 微服务调用
│   │   ├── dto/                  # 40+ DTO 类
│   │   ├── entity/              # 实体类
│   │   ├── mapper/              # Mapper 接口
│   │   └── exception/           # 异常类
│   ├── config/                  # 配置类 (5 个)
│   │   ├── CamundaConfig.java
│   │   ├── CamundaProperties.java
│   │   ├── CamundaAdminInitializer.java
│   │   ├── WorkflowCacheConfig.java
│   │   └── SwaggerConfig.java
│   └── SchedulerApplication.java # 启动类
├── src/main/resources/
│   ├── processes/                # BPMN 流程文件 (3 个)
│   │   ├── data-sync-process.bpmn           # 数据同步流程
│   │   ├── microservice-orchestration.bpmn  # 微服务编排流程
│   │   └── order-approval-process.bpmn      # 订单审批流程
│   ├── db/migration/             # Flyway 迁移脚本 (2 个)
│   │   ├── V2.0__camunda_workflow_init.sql
│   │   └── V2.1__workflow_form_template.sql
│   └── application.yml           # 主配置文件
└── docker-compose.yml            # Docker 环境
```

## 核心实现

### 1. JavaDelegate 实现

#### SendEmailDelegate
- **功能**: 发送邮件通知
- **特性**:
  - 支持模板变量替换
  - 邮件格式验证
  - 异常处理与 BpmnError 抛出

#### DataSyncDelegate
- **功能**: 数据同步操作
- **特性**:
  - 支持多种数据源类型 (database/api/file)
  - 配置化重试机制
  - 失败重试延迟

#### OrderApprovalDelegate
- **功能**: 订单审批处理
- **特性**:
  - 审批历史记录
  - 订单状态更新
  - 通知相关人员

#### MicroserviceCallDelegate
- **功能**: 微服务调用编排
- **特性**:
  - HTTP 方法支持 (GET/POST/PUT/DELETE)
  - 重试机制
  - 请求头自定义
  - 响应时间统计

### 2. BPMN 流程定义

#### 数据同步流程 (data-sync-process.bpmn)
```
开始 → 执行数据同步 → 判断结果 → 发送通知(成功/失败) → 结束
```

#### 微服务编排流程 (microservice-orchestration.bpmn)
```
开始 → 调用服务1 → 判断 → 调用服务2 → 判断 → 聚合结果 → 结束
              ↓失败
            错误处理 → 结束
```

#### 订单审批流程 (order-approval-process.bpmn)
```
开始 → 经理审批 → 执行审批处理 → 判断 → 并行通知 → 更新系统 → 结束
                                   ↓
                               系统状态更新
```

### 3. 数据库设计

#### Camunda 核心表 (22 个)
- ACT_RE_PROCDEF: 流程定义
- ACT_RU_EXECUTION: 流程实例
- ACT_RU_TASK: 任务
- ACT_RU_VARIABLE: 变量
- ACT_HI_PROCINST: 历史流程实例
- ACT_HI_TASKINST: 历史任务
- 等等...

#### 业务扩展表 (6 个)
- WF_FORM_TEMPLATE: 表单模板
- WF_FORM_TEMPLATE_HISTORY: 版本历史
- WF_FORM_TEMPLATE_USAGE: 使用记录
- WF_TASK_EXT: 任务扩展
- WF_INSTANCE_EXT: 实例扩展
- WF_OPERATION_LOG: 操作日志

### 4. Docker 部署

#### 多阶段构建
- **Builder**: Maven 构建阶段
- **Runtime**: JRE 运行阶段
- **优化**: 非 root 用户、健康检查、JVM 调优

#### 服务编排
```yaml
services:
  - basebackend-scheduler  # 主服务
  - basebackend-mysql      # MySQL 8.0
  - basebackend-redis      # Redis 7.2
  - basebackend-nacos      # Nacos 2.3
```

## 技术特性

### 1. 企业级架构
- [x] 分层架构 (Controller/Service/Entity)
- [x] 依赖注入与控制反转
- [x] 面向接口编程
- [x] 单一职责原则
- [x] 开闭原则

### 2. 性能优化
- [x] 多级缓存 (Caffeine + Redis)
- [x] 分页查询优化
- [x] 数据库索引优化
- [x] 连接池管理
- [x] JVM 调优参数

### 3. 可观测性
- [x] 日志记录 (SLF4J + Logback)
- [x] 健康检查 (Actuator)
- [x] 性能指标 (Micrometer)
- [x] 链路追踪 (OpenTelemetry)

### 4. 安全性
- [x] 输入验证
- [x] SQL 注入防护
- [x] XSS 防护
- [x] 非 root 用户运行
- [x] 敏感信息过滤

### 5. 可靠性
- [x] 异常处理机制
- [x] 重试机制
- [x] 故障转移
- [x] 优雅关闭
- [x] 资源管理

## 依赖管理

### 核心依赖
```xml
- Spring Boot 3.1.5
- Camunda 7.21.0
- PowerJob 4.3.8
- MyBatis Plus 3.5.5
- MySQL 8.0
- Redis 7.2
- Nacos 2.3
```

### 工具依赖
```xml
- Lombok
- Hutool (工具库)
- Jackson (JSON 处理)
- Swagger (API 文档)
- Flyway (数据库迁移)
```

## 部署说明

### 本地开发

#### 方式 1: Maven 编译运行
```bash
# 1. 安装依赖
mvn clean install -DskipTests

# 2. 启动应用
cd basebackend-scheduler
mvn spring-boot:run

# 3. 访问地址
# - 应用: http://localhost:8080
# - Camunda Web UI: http://localhost:8090/camunda
# - Swagger UI: http://localhost:8080/swagger-ui.html
```

#### 方式 2: Docker Compose
```bash
# 1. 启动所有服务
./start-docker.sh start

# 2. 查看服务状态
./start-docker.sh status

# 3. 查看日志
./start-docker.sh logs

# 4. 停止服务
./start-docker.sh stop
```

### 生产部署

#### Docker 镜像构建
```bash
# 构建镜像
docker build -t basebackend-scheduler:latest .

# 运行容器
docker run -d \
  -p 8080:8080 \
  -p 8090:8090 \
  -e SPRING_PROFILES_ACTIVE=prod \
  basebackend-scheduler:latest
```

#### Kubernetes 部署
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: basebackend-scheduler
spec:
  replicas: 3
  selector:
    matchLabels:
      app: basebackend-scheduler
  template:
    metadata:
      labels:
        app: basebackend-scheduler
    spec:
      containers:
      - name: basebackend-scheduler
        image: basebackend-scheduler:latest
        ports:
        - containerPort: 8080
        - containerPort: 8090
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
```

## API 接口

### 1. 流程定义接口
- `GET /api/process-definition` - 查询流程定义列表
- `POST /api/process-definition` - 部署流程定义
- `GET /api/process-definition/{id}` - 获取流程定义详情
- `DELETE /api/process-definition/{id}` - 删除流程定义
- `POST /api/process-definition/{id}/suspend` - 暂停流程定义
- `POST /api/process-definition/{id}/activate` - 激活流程定义

### 2. 流程实例接口
- `GET /api/process-instance` - 查询流程实例列表
- `POST /api/process-instance` - 启动流程实例
- `GET /api/process-instance/{id}` - 获取流程实例详情
- `DELETE /api/process-instance/{id}` - 删除流程实例
- `GET /api/process-instance/{id}/variables` - 获取变量
- `POST /api/process-instance/{id}/variables` - 设置变量
- `POST /api/process-instance/{id}/migrate` - 迁移流程实例

### 3. 任务接口
- `GET /api/task` - 查询任务列表
- `GET /api/task/{id}` - 获取任务详情
- `POST /api/task/{id}/claim` - 认领任务
- `POST /api/task/{id}/complete` - 完成任务
- `POST /api/task/{id}/delegate` - 委托任务
- `POST /api/task/{id}/variables` - 设置任务变量
- `GET /api/task/{id}/variables` - 获取任务变量

### 4. 历史数据接口
- `GET /api/historic/process-instance` - 查询历史流程实例
- `GET /api/historic/process-instance/{id}` - 获取历史实例详情
- `GET /api/historic/task` - 查询历史任务
- `GET /api/historic/activity-instance` - 查询历史活动
- `GET /api/historic/variable-instance` - 查询历史变量

### 5. 统计接口
- `GET /api/statistics/process` - 流程统计
- `GET /api/statistics/instance` - 实例统计
- `GET /api/statistics/task` - 任务统计
- `GET /api/statistics/duration` - 耗时统计

### 6. 表单模板接口
- `GET /api/form-template` - 查询表单模板列表
- `POST /api/form-template` - 创建表单模板
- `GET /api/form-template/{id}` - 获取表单模板详情
- `PUT /api/form-template/{id}` - 更新表单模板
- `DELETE /api/form-template/{id}` - 删除表单模板

## 配置说明

### 核心配置 (application.yml)

```yaml
spring:
  profiles:
    active: dev

  # 数据库配置
  datasource:
    url: jdbc:mysql://localhost:3306/basebackend_scheduler
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:BaseBackend@123}
    driver-class-name: com.mysql.cj.jdbc.Driver

  # Redis 配置
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}

  # Flyway 配置
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration

# Camunda 配置
camunda:
  datasource:
    url: jdbc:mysql://localhost:3306/basebackend_scheduler
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:BaseBackend@123}
  history-level: FULL
  history-cleanup:
    enabled: true
    cron-expression: "0 0 2 * * ?"

# PowerJob 配置
powerjob:
  worker:
    enabled: true
    server-address: ${NACOS_SERVER:localhost:8848}
    max-app-thread: 3
    maxConcurrency: 3

# 日志配置
logging:
  level:
    com.basebackend.scheduler: DEBUG
    org.camunda.bpm: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n"
```

## 测试说明

### 单元测试
```bash
# 运行所有单元测试
mvn test

# 运行指定测试类
mvn test -Dtest=ProcessDefinitionServiceImplTest

# 生成测试覆盖率报告
mvn jacoco:report
```

### 集成测试
```bash
# 启动测试环境
docker-compose -f docker-compose.test.yml up -d

# 运行集成测试
mvn integration-test -Dtest=*IntegrationTest
```

### 性能测试
```bash
# 使用 JMeter 进行压力测试
# 配置文件: src/test/jmeter/workflow-test.jmx

# 使用 Artillery 进行性能测试
artillery run src/test/perf/workflow-perf.yml
```

## 监控运维

### 1. Actuator 健康检查
```bash
# 检查应用健康状态
curl http://localhost:8080/actuator/health

# 查看所有端点
curl http://localhost:8080/actuator
```

### 2. Prometheus 指标
```bash
# 查看指标
curl http://localhost:8080/actuator/prometheus

# Grafana 仪表板
# - JVM 指标
# - Camunda 指标
# - 应用业务指标
```

### 3. 日志分析
```bash
# 查看应用日志
tail -f docker/scheduler/logs/application.log

# 查看 Camunda 日志
tail -f docker/scheduler/logs/camunda.log

# 使用 ELK Stack 分析
# - Elasticsearch: 日志存储
# - Logstash: 日志处理
# - Kibana: 日志可视化
```

### 4. 链路追踪
```bash
# 查看 Jaeger 追踪
# 访问: http://localhost:16686

# 查看 Zipkin 追踪
# 访问: http://localhost:9411
```

## 故障排除

### 常见问题

#### 1. 应用启动失败
**问题**: 数据库连接失败
**解决方案**:
```bash
# 检查数据库服务
docker-compose ps basebackend-mysql

# 检查数据库连接
mysql -h localhost -u root -p

# 查看应用日志
docker-compose logs basebackend-scheduler
```

#### 2. Camunda Web UI 无法访问
**问题**: 端口冲突或服务未启动
**解决方案**:
```bash
# 检查端口占用
netstat -tulpn | grep 8090

# 启动服务
docker-compose up -d basebackend-scheduler

# 检查服务状态
docker-compose ps
```

#### 3. BPMN 部署失败
**问题**: 流程定义文件格式错误
**解决方案**:
```bash
# 验证 BPMN 文件
# 使用 Camunda Modeler 打开 .bpmn 文件

# 查看部署日志
docker-compose logs basebackend-scheduler | grep -i deploy
```

#### 4. 任务执行失败
**问题**: JavaDelegate 异常
**解决方案**:
```bash
# 查看任务日志
docker-compose logs basebackend-scheduler | grep -i delegate

# 检查流程实例状态
# 访问 Camunda Web UI
```

### 调试技巧

#### 1. 开启调试日志
```yaml
logging:
  level:
    com.basebackend.scheduler: DEBUG
    org.camunda.bpm: DEBUG
    org.springframework.web: DEBUG
```

#### 2. 使用断点调试
```bash
# 远程调试配置
JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
```

#### 3. 数据库查询
```sql
-- 查看活跃流程实例
SELECT * FROM ACT_RU_EXECUTION WHERE END_TIME_ IS NULL;

-- 查看待办任务
SELECT * FROM ACT_RU_TASK WHERE ASSIGNEE_ IS NOT NULL;

-- 查看历史流程实例
SELECT * FROM ACT_HI_PROCINST ORDER BY START_TIME_ DESC LIMIT 100;
```

## 版本规划

### v1.1.0 (计划中)
- [ ] 动态表单设计器
- [ ] 可视化流程设计器
- [ ] 任务 SLA 管理
- [ ] 多租户支持
- [ ] 国际化支持

### v1.2.0 (计划中)
- [ ] BPMN 网关集成
- [ ] DMN 决策引擎
- [ ] CMMN 案例管理
- [ ] 消息队列集成
- [ ] 事件驱动架构

### v1.3.0 (计划中)
- [ ] 机器学习预测
- [ ] 智能任务分配
- [ ] 流程挖掘
- [ ] 高级分析报表
- [ ] 移动端支持

## 贡献指南

### 代码规范
1. 遵循阿里巴巴 Java 开发手册
2. 使用 Checkstyle 检查代码格式
3. 提交前必须运行 SonarQube 扫描
4. 代码覆盖率不低于 80%

### 提交规范
```
feat: 新功能
fix: 修复 bug
docs: 文档更新
style: 代码格式调整
refactor: 代码重构
test: 测试用例
chore: 构建/工具变动
```

### Pull Request 流程
1. Fork 项目
2. 创建特性分支
3. 提交代码更改
4. 推送分支到远程
5. 创建 Pull Request
6. 代码审查
7. 合并代码

## 许可证

本项目采用 Apache 2.0 许可证 - 详见 [LICENSE](LICENSE) 文件

## 联系方式

**开发团队**: BaseBackend Team
**邮箱**: dev@basebackend.com
**官网**: https://basebackend.com
**文档**: https://docs.basebackend.com

## 致谢

感谢以下开源项目的支持：
- Camunda - 工作流引擎
- PowerJob - 任务调度框架
- Spring Boot - 应用框架
- MyBatis Plus - ORM 框架
- Nacos - 配置中心
- Redis - 缓存数据库
- MySQL - 数据库
- Docker - 容器化平台

---

**文档版本**: v1.0.0
**最后更新**: 2025-01-01
**维护人员**: BaseBackend Team
