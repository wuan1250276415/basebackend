# 任务调度系统快速开始指南

## 概述

BaseBackend任务调度系统基于PowerJob构建，提供企业级任务调度能力，支持：
- 定时任务、延迟任务、即时任务
- 多维度重试与幂等性保障
- DAG工作流编排
- 失败告警（钉钉/企业微信/邮件）

## 快速开始

### 步骤1: 启动PowerJob Server

使用Docker快速启动：

```bash
docker run -d \
  --name powerjob-server \
  -p 7700:7700 \
  -p 10086:10086 \
  -p 10010:10010 \
  -e TZ="Asia/Shanghai" \
  -e JVMOPTIONS="-Xmx512m" \
  -e PARAMS="--spring.profiles.active=product --spring.datasource.core.jdbc-url=jdbc:h2:mem:powerjob-product" \
  tjqq/powerjob-server:latest
```

访问控制台：http://localhost:7700/
- 默认用户名/密码：admin/admin

### 步骤2: 创建应用

在PowerJob控制台创建应用：
- 应用名称：basebackend
- 密码：留空或自定义

### 步骤3: 配置Worker

编辑`application-scheduler.yml`：

```yaml
powerjob:
  worker:
    server-address: http://localhost:7700
    app-name: basebackend
    port: 27777
```

### 步骤4: 启动应用

```bash
java -jar basebackend-admin-api.jar --spring.profiles.active=scheduler
```

## 功能演示

### 1. 延迟任务 - 订单超时取消

```java
@Autowired
private DelayTaskService delayTaskService;

// 创建订单后，30分钟后自动取消未支付订单
public void createOrder(Order order) {
    // ... 创建订单逻辑

    // 提交延迟任务
    delayTaskService.submitDelayTask(
        DelayTaskType.ORDER_TIMEOUT,
        order.getId().toString(),
        Map.of("orderId", order.getId()),
        Duration.ofMinutes(30)
    );
}
```

### 2. 定时任务 - 每日数据清理

在PowerJob控制台创建任务：
- 任务名称：daily-data-cleanup
- CRON表达式：0 0 2 * * ? (每天凌晨2点)
- 处理器：com.basebackend.scheduler.delay.handlers.DataCleanupHandler
- 执行类型：单机执行

### 3. DAG工作流 - 订单处理流程

```java
@Autowired
private DAGEngine dagEngine;

public void processOrder() {
    // 构建工作流
    WorkflowDefinition workflow = WorkflowDefinition.builder()
        .name("订单处理流程")
        .build();

    // 添加节点
    workflow.addNode(WorkflowNode.builder()
        .nodeId("check_stock")
        .nodeName("检查库存")
        .processorType("StockCheckHandler")
        .build());

    workflow.addNode(WorkflowNode.builder()
        .nodeId("create_order")
        .nodeName("创建订单")
        .processorType("OrderCreateHandler")
        .build());

    workflow.addNode(WorkflowNode.builder()
        .nodeId("payment")
        .nodeName("支付处理")
        .processorType("PaymentHandler")
        .build());

    // 添加依赖关系
    workflow.addEdge("check_stock", "create_order");
    workflow.addEdge("create_order", "payment");

    // 执行工作流
    Map<String, Object> result = dagEngine.execute(workflow);
}
```

### 4. 幂等性保障

```java
@Autowired
private JobIdempotencyManager idempotencyManager;

public void processTask(Long jobId, Long instanceId) {
    // 检查是否已执行
    if (idempotencyManager.isDuplicate(jobId, instanceId)) {
        log.info("任务已执行，跳过");
        return;
    }

    // 获取执行锁
    RLock lock = idempotencyManager.tryAcquireLock(jobId, instanceId);
    if (lock == null) {
        log.warn("获取锁失败，任务可能正在执行");
        return;
    }

    try {
        // 执行业务逻辑
        // ...

        // 标记已执行
        idempotencyManager.markExecuted(jobId, instanceId, 3600);
    } finally {
        idempotencyManager.releaseLock(lock);
    }
}
```

### 5. 重试策略

```java
RetryPolicy retryPolicy = RetryPolicy.builder()
    .maxRetryTimes(3)              // 最大重试3次
    .retryInterval(60)              // 初始间隔60秒
    .exponentialBackoff(true)       // 启用指数退避
    .backoffMultiplier(2.0)         // 退避倍数2
    .maxBackoffInterval(3600)       // 最大间隔1小时
    .build();

// 第1次失败：60秒后重试
// 第2次失败：120秒后重试
// 第3次失败：240秒后重试
// 超过3次：进入死信队列
```

### 6. 失败告警

配置告警：

```yaml
scheduler:
  alert:
    enabled: true
    ding-talk-webhook: https://oapi.dingtalk.com/robot/send?access_token=xxx
    wechat-webhook: https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=xxx
    email-to: admin@example.com
```

任务失败会自动发送告警通知。

## 数据库表结构

系统会自动创建5张表：

1. **sys_job_info** - 任务定义表
2. **sys_job_instance** - 任务实例表
3. **sys_workflow_definition** - 工作流定义表
4. **sys_workflow_instance** - 工作流实例表
5. **sys_job_dead_letter** - 死信任务表

## 监控与管理

### PowerJob控制台

访问 http://localhost:7700/ 可以：
- 查看任务列表
- 查看执行历史
- 手动触发任务
- 查看Worker状态
- 查看执行日志

### 任务状态

- **WAITING**: 等待执行
- **RUNNING**: 执行中
- **SUCCESS**: 执行成功
- **FAILED**: 执行失败
- **CANCELLED**: 已取消
- **TIMEOUT**: 超时
- **STOPPED**: 已停止

## 最佳实践

### 1. 延迟任务场景

- **订单超时**: 30分钟未支付自动取消
- **消息延迟**: 定时推送营销消息
- **数据清理**: 7天后删除临时文件
- **状态流转**: 3天后自动审核通过

### 2. 重试策略

- **幂等接口**: 可以安全重试多次
- **非幂等接口**: 使用幂等性管理器
- **重试次数**: 根据业务重要性设置1-5次
- **退避策略**: 使用指数退避避免雪崩

### 3. 工作流设计

- **节点粒度**: 每个节点应该是原子操作
- **依赖关系**: 避免循环依赖
- **错误处理**: 关键节点设置allowFailure=false
- **超时时间**: 根据业务设置合理超时

### 4. 监控告警

- **关键任务**: 失败立即告警
- **普通任务**: 连续失败3次后告警
- **告警渠道**: 生产环境使用钉钉/企业微信
- **告警分级**: 区分P0/P1/P2级别

## 常见问题

### Q: PowerJob连接失败？

A: 检查配置：
```bash
# 查看Worker日志
tail -f logs/powerjob-worker.log

# 确认Server地址正确
curl http://localhost:7700/
```

### Q: 任务不执行？

A: 检查：
1. Worker是否启动
2. 应用名称是否匹配
3. 任务是否启用
4. CRON表达式是否正确

### Q: 如何查看任务日志？

A: 在PowerJob控制台 -> 任务管理 -> 执行日志

## 下一步

- 查看完整文档：[SCHEDULER-FEATURES.md](./SCHEDULER-FEATURES.md)
- 了解工作流编排：[WORKFLOW-GUIDE.md](./WORKFLOW-GUIDE.md)
- 部署PowerJob集群：[docker/powerjob/README.md](../docker/powerjob/README.md)

## 总结

✅ PowerJob提供强大的任务调度能力
✅ 支持定时、延迟、工作流等多种任务类型
✅ 完善的重试、幂等、告警机制
✅ Web控制台便于管理和监控
✅ 分布式架构支持高可用

现在您可以开始使用任务调度系统了！
