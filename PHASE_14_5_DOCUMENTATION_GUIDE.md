# Phase 14.5: 文档完善实施指南

## 📋 概述

本指南介绍如何构建企业级智能平台的完整文档体系，包括API文档、运维手册、故障处理指南等，确保平台的可维护性、可操作性和可持续性，降低运维成本，提升团队协作效率。

---

## 📚 文档体系架构

### 文档结构图

```
┌─────────────────────────────────────────────────────────────────────┐
│                      智能平台文档体系                                 │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐           │
│  │   API文档     │  │   运维手册     │  │   开发指南     │           │
│  │              │  │              │  │              │           │
│  │ • OpenAPI    │  │ • 部署指南     │  │ • 开发规范     │           │
│  │ • 接口说明     │  │ • 监控指南     │  │ • 代码规范     │           │
│  │ • 示例代码     │  │ • 维护手册     │  │ • 集成指南     │           │
│  │ • SDK文档     │  │ • 升级指南     │  │ • 测试指南     │           │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘           │
│         │                 │                 │                     │
│  ┌──────▼────────┐  ┌─────▼──────┐  ┌──────▼──────┐           │
│  │   故障处理     │  │   最佳实践   │  │   架构文档     │           │
│  │              │  │              │  │              │           │
│  │ • 故障排查     │  │ • 性能调优     │  │ • 总体架构     │           │
│  │ • 解决方案     │  │ • 安全配置     │  │ • 设计模式     │           │
│  │ • 应急响应     │  │ • 成本优化     │  │ • 技术选型     │           │
│  │ • 知识库       │  │ • 运维经验     │  │ • 演进路径     │           │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘           │
│         │                 │                 │                     │
│  ┌──────▼────────┐  ┌─────▼──────┐  ┌──────▼──────┐           │
│  │   培训材料     │  │   版本记录   │  │   常见问题     │           │
│  │              │  │              │  │              │           │
│  │ • 新手入门     │  │ • 版本历史     │  │ • FAQ        │           │
│  │ • 进阶教程     │  │ • 发布说明     │  │ • 疑难解答     │           │
│  │ • 视频教程     │  │ • 升级说明     │  │ • 问题归档     │           │
│  │ • 实操演示     │  │ • 迁移指南     │  │ • 解决方案     │           │
│  └──────────────┘  └─────────────┘  └─────────────┘           │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 文档管理策略

| 文档类型 | 更新频率 | 责任人 | 评审流程 | 版本控制 |
|----------|----------|--------|----------|----------|
| **API文档** | 每次API变更 | 开发团队 | 代码审查 | Git版本 |
| **运维手册** | 每季度 | 运维团队 | 运维评审 | Git版本 |
| **开发指南** | 每月 | 架构师 | 技术评审 | Git版本 |
| **故障处理** | 实时 | 运维团队 | 事后复盘 | Wiki |
| **最佳实践** | 实时 | 各团队 | 定期评审 | Git版本 |

---

## 📖 API文档系统

### 1. OpenAPI规范文档

```yaml
# openapi.yaml - 智能决策平台API文档
openapi: 3.0.3
info:
  title: 智能决策平台 API
  description: |
    企业级智能决策平台，提供机器学习决策、强化学习、智能推荐等能力。

    ## 核心功能
    - 🤖 机器学习决策引擎
    - 🎯 智能推荐系统
    - 🔄 自动化运营策略
    - 📊 决策效果监控

    ## 认证方式
    API使用Bearer Token进行认证，请在请求头中携带：
    ```
    Authorization: Bearer {your-token}
    ```

  version: 1.0.0
  contact:
    name: API支持团队
    email: api-support@example.com
  license:
    name: Apache 2.0
    url: https://www.apache.org/licenses/LICENSE-2.0.html

servers:
  - url: https://api.intelligent-platform.com/v1
    description: 生产环境
  - url: https://staging-api.intelligent-platform.com/v1
    description: 测试环境
  - url: http://localhost:8080/v1
    description: 开发环境

security:
  - BearerAuth: []

paths:
  # 智能决策API
  /decision:
    post:
      tags:
        - 智能决策
      summary: 执行智能决策
      description: |
        基于规则引擎、机器学习模型和强化学习的融合决策。
        支持多种决策策略：保守、平衡、激进、强化学习。

      operationId: makeDecision
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/DecisionRequest'
            examples:
              conservative_decision:
                summary: 保守策略决策
                value:
                  context:
                    userId: "user_123"
                    scenario: "transaction_risk"
                  features:
                    amount: 1000
                    userAge: 35
                    transactionHistory: 50
                  strategy: CONSERVATIVE
              balanced_decision:
                summary: 平衡策略决策
                value:
                  context:
                    userId: "user_123"
                    scenario: "transaction_risk"
                  features:
                    amount: 1000
                    userAge: 35
                    transactionHistory: 50
                  strategy: BALANCED
      responses:
        '200':
          description: 决策成功
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/DecisionResult'
              examples:
                approval:
                  summary: 审批通过
                  value:
                    success: true
                    decision: APPROVE
                    confidence: 0.95
                    explanation:
                      primaryReason: "用户信用良好，交易金额正常"
                      ruleResults:
                        - ruleName: "high_amount_check"
                          matched: false
                      mlResults:
                        - model: "fraud_detection_model"
                          score: 0.05
                    processingTime: 45
                review:
                  summary: 需人工审核
                  value:
                    success: true
                    decision: REVIEW
                    confidence: 0.75
                    explanation:
                      primaryReason: "交易时间异常，建议人工审核"
                      riskFactors:
                        - factor: "unusual_time"
                          impact: 0.3
                    processingTime: 52
        '400':
          $ref: '#/components/responses/BadRequest'
        '401':
          $ref: '#/components/responses/Unauthorized'
        '500':
          $ref: '#/components/responses/InternalError'

  # 智能推荐API
  /recommendation:
    post:
      tags:
        - 智能推荐
      summary: 生成智能推荐
      description: |
        基于协同过滤、内容推荐、深度学习等算法的混合推荐系统。
        支持多种推荐场景：商品推荐、内容推荐、用户推荐等。

      operationId: generateRecommendations
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/RecommendationRequest'
            examples:
              product_recommendation:
                summary: 商品推荐
                value:
                  userId: "user_123"
                  scenario: "product_recommendation"
                  topN: 10
                  context:
                    category: "electronics"
                    priceRange: "1000-5000"
                  constraints:
                    excludeViewed: true
                    diversityBoost: 0.2
      responses:
        '200':
          description: 推荐生成成功
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/RecommendationResult'
              examples:
                success:
                  summary: 推荐成功
                  value:
                    userId: "user_123"
                    requestId: "req_456"
                    recommendations:
                      - itemId: "product_789"
                        score: 0.95
                        reason: "基于您的购买历史，推荐相似商品"
                      - itemId: "product_790"
                        score: 0.88
                        reason: "热门商品，与您的兴趣匹配"
                    algorithmUsed: "HYBRID"
                    confidence: 0.92
                    generatedAt: "2025-11-15T10:30:00Z"

  # 智能运营API
  /operation/scaling:
    post:
      tags:
        - 智能运营
      summary: 智能资源调度
      description: |
        基于机器学习的智能资源调度和自动伸缩。
        支持多目标优化：成本、性能、可靠性。

      operationId: makeScalingDecision
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ScalingRequest'
      responses:
        '200':
          description: 调度决策生成成功
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ScalingResult'

components:
  schemas:
    DecisionRequest:
      type: object
      required:
        - context
        - features
      properties:
        context:
          type: object
          description: 决策上下文
          properties:
            userId:
              type: string
              description: 用户ID
              example: "user_123"
            scenario:
              type: string
              description: 决策场景
              enum: [transaction_risk, login_security, credit_approval, inventory_optimization]
              example: "transaction_risk"
            timestamp:
              type: string
              format: date-time
              description: 决策时间
        features:
          type: object
          description: 特征数据
          additionalProperties:
            type: string
          example:
            amount: "1000"
            userAge: "35"
            transactionHistory: "50"
        strategy:
          type: string
          description: 决策策略
          enum: [CONSERVATIVE, BALANCED, AGGRESSIVE, REINFORCEMENT_LEARNING]
          default: BALANCED
          example: "BALANCED"

    DecisionResult:
      type: object
      properties:
        success:
          type: boolean
          description: 决策是否成功
          example: true
        decision:
          type: string
          description: 决策结果
          enum: [APPROVE, REJECT, REVIEW]
          example: "APPROVE"
        confidence:
          type: number
          format: float
          minimum: 0
          maximum: 1
          description: 决策置信度
          example: 0.95
        explanation:
          type: object
          description: 决策解释
          properties:
            primaryReason:
              type: string
              description: 主要原因
              example: "用户信用良好，交易金额正常"
            ruleResults:
              type: array
              description: 规则引擎结果
              items:
                $ref: '#/components/schemas/RuleResult'
            mlResults:
              type: array
              description: 机器学习结果
              items:
                $ref: '#/components/schemas/MLResult'
        processingTime:
          type: integer
          description: 处理时间（毫秒）
          example: 45
        timestamp:
          type: string
          format: date-time
          description: 决策时间戳

    RecommendationRequest:
      type: object
      required:
        - userId
        - scenario
        - topN
      properties:
        userId:
          type: string
          description: 用户ID
          example: "user_123"
        scenario:
          type: string
          description: 推荐场景
          enum: [product_recommendation, content_recommendation, user_recommendation, merchant_recommendation]
          example: "product_recommendation"
        topN:
          type: integer
          minimum: 1
          maximum: 100
          description: 推荐数量
          example: 10
        context:
          type: object
          description: 推荐上下文
          additionalProperties:
            type: string
          example:
            category: "electronics"
            priceRange: "1000-5000"
        constraints:
          type: object
          description: 推荐约束
          properties:
            excludeViewed:
              type: boolean
              description: 排除已查看
              default: true
            diversityBoost:
              type: number
              format: float
              minimum: 0
              maximum: 1
              description: 多样性增强
              default: 0

    RecommendationResult:
      type: object
      properties:
        userId:
          type: string
          description: 用户ID
          example: "user_123"
        requestId:
          type: string
          description: 请求ID
          example: "req_456"
        recommendations:
          type: array
          description: 推荐列表
          items:
            $ref: '#/components/schemas/Recommendation'
        algorithmUsed:
          type: string
          description: 使用算法
          enum: [COLLABORATIVE_FILTERING, CONTENT_BASED, DEEP_LEARNING, HYBRID, REAL_TIME]
          example: "HYBRID"
        confidence:
          type: number
          format: float
          minimum: 0
          maximum: 1
          description: 推荐置信度
          example: 0.92
        generatedAt:
          type: string
          format: date-time
          description: 生成时间

    Recommendation:
      type: object
      properties:
        itemId:
          type: string
          description: 物品ID
          example: "product_789"
        score:
          type: number
          format: float
          minimum: 0
          maximum: 1
          description: 推荐分数
          example: 0.95
        reason:
          type: string
          description: 推荐理由
          example: "基于您的购买历史，推荐相似商品"

  responses:
    BadRequest:
      description: 请求参数错误
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/ErrorResponse'
          example:
            success: false
            error:
              code: "INVALID_REQUEST"
              message: "请求参数不正确"
              details:
                - field: "userId"
                  message: "用户ID不能为空"

    Unauthorized:
      description: 未授权
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/ErrorResponse'
          example:
            success: false
            error:
              code: "UNAUTHORIZED"
              message: "未授权访问，请检查认证信息"

    InternalError:
      description: 服务器内部错误
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/ErrorResponse'
          example:
            success: false
            error:
              code: "INTERNAL_ERROR"
              message: "服务器内部错误，请稍后重试"

  securitySchemes:
    BearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
```

### 2. SDK文档示例

```java
// 智能决策平台 Java SDK
package com.intelligentplatform.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 智能决策平台 Java SDK
 * 提供简洁易用的API调用接口
 */
public class IntelligentDecisionClient {

    private final String baseUrl;
    private final String apiKey;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public IntelligentDecisionClient(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 执行智能决策
     *
     * @param request 决策请求
     * @return 决策结果
     * @throws DecisionException 决策异常
     */
    public DecisionResult makeDecision(DecisionRequest request) throws DecisionException {
        try {
            // 构建请求
            Request httpRequest = new Request.Builder()
                .url(baseUrl + "/decision")
                .post(RequestBody.create(
                    objectMapper.writeValueAsString(request),
                    MediaType.parse("application/json")
                ))
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .build();

            // 发送请求
            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    throw new DecisionException("决策请求失败: " + response.code());
                }

                String responseBody = response.body().string();
                return objectMapper.readValue(responseBody, DecisionResult.class);
            }
        } catch (IOException e) {
            throw new DecisionException("网络请求异常", e);
        }
    }

    /**
     * 生成智能推荐
     *
     * @param request 推荐请求
     * @return 推荐结果
     * @throws RecommendationException 推荐异常
     */
    public RecommendationResult generateRecommendations(RecommendationRequest request)
            throws RecommendationException {
        try {
            Request httpRequest = new Request.Builder()
                .url(baseUrl + "/recommendation")
                .post(RequestBody.create(
                    objectMapper.writeValueAsString(request),
                    MediaType.parse("application/json")
                ))
                .addHeader("Authorization", "Bearer " + apiKey)
                .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    throw new RecommendationException("推荐请求失败: " + response.code());
                }

                String responseBody = response.body().string();
                return objectMapper.readValue(responseBody, RecommendationResult.class);
            }
        } catch (IOException e) {
            throw new RecommendationException("网络请求异常", e);
        }
    }

    /**
     * 执行智能伸缩
     *
     * @param serviceName 服务名称
     * @param timeWindow 时间窗口
     * @return 伸缩决策
     * @throws OperationException 操作异常
     */
    public ScalingDecision makeScalingDecision(String serviceName, Duration timeWindow)
            throws OperationException {
        ScalingRequest request = ScalingRequest.builder()
            .serviceName(serviceName)
            .timeWindow(timeWindow)
            .build();

        try {
            Request httpRequest = new Request.Builder()
                .url(baseUrl + "/operation/scaling")
                .post(RequestBody.create(
                    objectMapper.writeValueAsString(request),
                    MediaType.parse("application/json")
                ))
                .addHeader("Authorization", "Bearer " + apiKey)
                .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    throw new OperationException("伸缩请求失败: " + response.code());
                }

                String responseBody = response.body().string();
                return objectMapper.readValue(responseBody, ScalingResult.class);
            }
        } catch (IOException e) {
            throw new OperationException("网络请求异常", e);
        }
    }
}

// 使用示例
public class ClientExample {

    public static void main(String[] args) {
        // 初始化客户端
        IntelligentDecisionClient client = new IntelligentDecisionClient(
            "https://api.intelligent-platform.com/v1",
            "your-api-key"
        );

        try {
            // 决策示例
            DecisionRequest decisionRequest = DecisionRequest.builder()
                .context(DecisionContext.builder()
                    .userId("user_123")
                    .scenario("transaction_risk")
                    .build())
                .features(Map.of(
                    "amount", "1000",
                    "userAge", "35",
                    "transactionHistory", "50"
                ))
                .strategy(DecisionStrategy.BALANCED)
                .build();

            DecisionResult decision = client.makeDecision(decisionRequest);
            System.out.println("决策结果: " + decision.getDecision());
            System.out.println("置信度: " + decision.getConfidence());

            // 推荐示例
            RecommendationRequest recRequest = RecommendationRequest.builder()
                .userId("user_123")
                .scenario("product_recommendation")
                .topN(10)
                .build();

            RecommendationResult recommendations = client.generateRecommendations(recRequest);
            System.out.println("推荐数量: " + recommendations.getRecommendations().size());

        } catch (DecisionException e) {
            System.err.println("决策异常: " + e.getMessage());
        }
    }
}
```

### 3. API文档生成脚本

```bash
#!/bin/bash
# generate-api-docs.sh - API文档自动生成脚本

set -e

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DOCS_DIR="${PROJECT_ROOT}/docs/api"
OPENAPI_FILE="${DOCS_DIR}/openapi.yaml"

echo "开始生成API文档..."

# 1. 验证OpenAPI规范
echo "验证OpenAPI规范..."
if command -v swagger-cli &> /dev/null; then
    swagger-cli validate "${OPENAPI_FILE}"
else
    echo "警告: swagger-cli未安装，跳过验证"
fi

# 2. 生成HTML文档
echo "生成HTML文档..."
if command -v redoc-cli &> /dev/null; then
    redoc-cli bundle "${OPENAPI_FILE}" --output "${DOCS_DIR}/index.html"
elif command -v npx &> /dev/null; then
    npx redoc-cli@latest bundle "${OPENAPI_FILE}" --output "${DOCS_DIR}/index.html"
else
    echo "错误: 需要redoc-cli或npx来生成HTML文档"
    exit 1
fi

# 3. 生成Postman集合
echo "生成Postman集合..."
if command -v openapi2postmanv2 &> /dev/null; then
    openapi2postmanv2 -s "${OPENAPI_FILE}" -o "${DOCS_DIR}/postman-collection.json"
elif command -v npx &> /dev/null; then
    npx openapi2postmanv2@latest -s "${OPENAPI_FILE}" -o "${DOCS_DIR}/postman-collection.json"
else
    echo "错误: 需要openapi2postmanv2来生成Postman集合"
    exit 1
fi

# 4. 生成SDK
echo "生成SDK..."
# Java SDK
if command -v npx &> /dev/null; then
    npx @openapitools/openapi-generator-cli@latest generate \
        -i "${OPENAPI_FILE}" \
        -g java \
        -o "${DOCS_DIR}/sdk/java" \
        --library resttemplate \
        --package-name com.intelligentplatform.sdk
fi

# Python SDK
if command -v npx &> /dev/null; then
    npx @openapitools/openapi-generator-cli@latest generate \
        -i "${OPENAPI_FILE}" \
        -g python \
        -o "${DOCS_DIR}/sdk/python" \
        --package-name intelligent_platform_sdk
fi

# JavaScript SDK
if command -v npx &> /dev/null; then
    npx @openapitools/openapi-generator-cli@latest generate \
        -i "${OPENAPI_FILE}" \
        -g javascript \
        -o "${DOCS_DIR}/sdk/javascript" \
        --module-name IntelligentPlatformSDK
fi

# 5. 生成Markdown文档
echo "生成Markdown文档..."
mkdir -p "${DOCS_DIR}/markdown"

# 使用redoc将HTML转换为Markdown
if command -v npx &> /dev/null; then
    npx redoc-cli@latest bundle "${OPENAPI_FILE}" \
        --output "${DOCS_DIR}/index.html" \
        --options.copyGeneratedFiles

    # 生成API端点列表
    grep -oP '(?<=paths:\s*\n)[\s\S]*?(?=\n\w+\s*:)' "${OPENAPI_FILE}" | \
        sed 's/^[[:space:]]*//' > "${DOCS_DIR}/markdown/endpoints.md"
fi

echo "API文档生成完成!"
echo "HTML文档: ${DOCS_DIR}/index.html"
echo "Postman集合: ${DOCS_DIR}/postman-collection.json"
echo "SDK目录: ${DOCS_DIR}/sdk"
```

---

## 🔧 运维手册

### 1. 部署指南

#### 1.1 环境准备

```yaml
# deployment/requirements.yaml
---
# 基础设施要求
infrastructure:
  kubernetes:
    version: "1.28+"
    nodes: 3
    resources_per_node:
      cpu: "8 cores"
      memory: "32GB"
      storage: "500GB SSD"

  database:
    postgresql:
      version: "15+"
      instances: 2
      resources:
        cpu: "4 cores"
        memory: "16GB"
        storage: "1TB SSD"

    redis:
      version: "7.2+"
      cluster_mode: true
      nodes: 3
      resources:
        cpu: "2 cores"
        memory: "8GB"

  messaging:
    kafka:
      version: "3.5+"
      brokers: 3
      resources:
        cpu: "4 cores"
        memory: "8GB"
        storage: "500GB SSD"

  monitoring:
    prometheus:
      version: "2.47+"
      storage: "100GB"

    grafana:
      version: "10.2+"
      resources:
        cpu: "2 cores"
        memory: "4GB"

# 部署配置
deployment:
  namespace: intelligent-platform
  replicas:
    api_gateway: 3
    decision_engine: 5
    recommendation_service: 5
    operation_service: 3
    monitoring_service: 2

  resources:
    limits:
      cpu: "2 cores"
      memory: "4GB"
    requests:
      cpu: "500m"
      memory: "1GB"
```

#### 1.2 部署流程

```bash
#!/bin/bash
# deploy-platform.sh - 智能平台一键部署脚本

set -e

# 配置变量
NAMESPACE="intelligent-platform"
RELEASE_NAME="intelligent-platform"
HELM_CHART_PATH="./deployment/helm"

echo "=================================="
echo "智能平台部署开始"
echo "=================================="

# 1. 检查前置条件
echo "检查前置条件..."
kubectl version --short
helm version --short

# 检查集群资源
echo "检查集群资源..."
kubectl top nodes || echo "metrics-server未安装"

# 2. 创建命名空间
echo "创建命名空间: ${NAMESPACE}"
kubectl create namespace ${NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -

# 3. 添加Helm仓库
echo "添加Helm仓库..."
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo add grafana https://grafana.github.io/helm-charts
helm repo update

# 4. 安装依赖组件
echo "安装依赖组件..."

# 安装Prometheus
echo "安装Prometheus监控..."
helm upgrade --install prometheus prometheus-community/kube-prometheus-stack \
  --namespace monitoring --create-namespace \
  --values deployment/values/prometheus.yaml

# 安装Grafana
echo "安装Grafana仪表盘..."
helm upgrade --install grafana grafana/grafana \
  --namespace monitoring --create-namespace \
  --values deployment/values/grafana.yaml

# 安装Kafka
echo "安装Kafka消息队列..."
helm upgrade --install kafka bitnami/kafka \
  --namespace ${NAMESPACE} \
  --values deployment/values/kafka.yaml

# 安装PostgreSQL
echo "安装PostgreSQL数据库..."
helm upgrade --install postgresql bitnami/postgresql \
  --namespace ${NAMESPACE} \
  --values deployment/values/postgresql.yaml

# 安装Redis
echo "安装Redis缓存..."
helm upgrade --install redis bitnami/redis \
  --namespace ${NAMESPACE} \
  --values deployment/values/redis.yaml

# 5. 安装智能平台
echo "安装智能平台..."

# 等待数据库就绪
echo "等待数据库就绪..."
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=postgresql \
  --namespace ${NAMESPACE} --timeout=300s

# 部署应用
echo "部署应用服务..."
helm upgrade --install ${RELEASE_NAME} ${HELM_CHART_PATH} \
  --namespace ${NAMESPACE} \
  --values deployment/values/production.yaml \
  --set image.tag=latest \
  --set replicaCount.replica1=3

# 6. 验证部署
echo "验证部署状态..."
kubectl get pods -n ${NAMESPACE}
kubectl get svc -n ${NAMESPACE}

# 健康检查
echo "执行健康检查..."
sleep 30

# 检查Pod状态
POD_STATUS=$(kubectl get pods -n ${NAMESPACE} -l app=intelligent-platform -o jsonpath='{.items[*].status.phase}')
if [[ "$POD_STATUS" == *"Running"* ]]; then
    echo "✓ 所有Pod运行正常"
else
    echo "✗ Pod状态异常"
    kubectl describe pods -n ${NAMESPACE}
    exit 1
fi

# 检查服务状态
SERVICE_STATUS=$(kubectl get svc -n ${NAMESPACE} -o jsonpath='{.items[*].status.loadBalancer.ingress[*].ip}')
if [[ -n "$SERVICE_STATUS" ]]; then
    echo "✓ 服务就绪，访问地址: http://${SERVICE_STATUS}"
else
    echo "⚠ 服务IP未分配，请检查负载均衡器配置"
fi

# 7. 执行冒烟测试
echo "执行冒烟测试..."
API_ENDPOINT="http://$(kubectl get svc -n ${NAMESPACE} api-gateway -o jsonpath='{.status.loadBalancer.ingress[0].ip}')"

# 测试健康检查端点
if curl -f "${API_ENDPOINT}/actuator/health" > /dev/null 2>&1; then
    echo "✓ 健康检查端点正常"
else
    echo "⚠ 健康检查端点异常"
fi

# 测试决策API
TEST_REQUEST='{"context":{"userId":"test_user","scenario":"transaction_risk"},"features":{"amount":"1000"}}'
if curl -X POST -H "Content-Type: application/json" \
     -d "${TEST_REQUEST}" \
     "${API_ENDPOINT}/api/v1/decision" > /dev/null 2>&1; then
    echo "✓ 决策API测试通过"
else
    echo "⚠ 决策API测试失败"
fi

echo "=================================="
echo "智能平台部署完成！"
echo "=================================="
echo "访问地址: ${API_ENDPOINT}"
echo "Grafana: http://$(kubectl get svc -n monitoring grafana -o jsonpath='{.status.loadBalancer.ingress[0].ip}')"
echo "监控: http://$(kubectl get svc -n monitoring prometheus-server -o jsonpath='{.status.loadBalancer.ingress[0].ip}')"
```

#### 1.3 Helm图表结构

```yaml
# deployment/helm/Chart.yaml
apiVersion: v2
name: intelligent-platform
description: 企业级智能决策平台
type: application
version: 1.0.0
appVersion: "1.0"
keywords:
  - ai
  - machine-learning
  - decision-engine
  - recommendation
home: https://github.com/intelligent-platform
sources:
  - https://github.com/intelligent-platform

# deployment/helm/values.yaml
replicaCount: 1

image:
  repository: intelligent-platform/api
  pullPolicy: IfNotPresent
  tag: "1.0.0"

imagePullSecrets: []
nameOverride: ""
fullnameOverride: ""

serviceAccount:
  create: true
  annotations: {}
  name: ""

podAnnotations: {}

podSecurityContext:
  fsGroup: 1000

securityContext:
  runAsNonRoot: true
  runAsUser: 1000
  readOnlyRootFilesystem: false

resources:
  limits:
    cpu: 2000m
    memory: 4Gi
  requests:
    cpu: 500m
    memory: 1Gi

autoscaling:
  enabled: true
  minReplicas: 2
  maxReplicas: 10
  targetCPUUtilizationPercentage: 70
  targetMemoryUtilizationPercentage: 80

nodeSelector: {}

tolerations: []

affinity: {}

# 组件配置
components:
  apiGateway:
    enabled: true
    replicaCount: 3
    service:
      type: LoadBalancer
      port: 80
      targetPort: 8080

  decisionEngine:
    enabled: true
    replicaCount: 5
    resources:
      limits:
        cpu: 2000m
        memory: 4Gi

  recommendationService:
    enabled: true
    replicaCount: 5

  operationService:
    enabled: true
    replicaCount: 3

  monitoringService:
    enabled: true
    replicaCount: 2

# 数据库配置
database:
  postgresql:
    enabled: true
    host: postgresql
    port: 5432
    name: intelligent_platform
    user: platform_user
    existingSecret: postgresql-secret

  redis:
    enabled: true
    host: redis
    port: 6379
    database: 0

# 外部服务
externalServices:
  prometheus:
    enabled: true
    url: http://prometheus-server:80

  grafana:
    enabled: true
    url: http://grafana:3000

  kafka:
    enabled: true
    brokers:
      - kafka:9092
```

### 2. 监控指南

#### 2.1 监控指标体系

```yaml
# monitoring/metrics.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: prometheus-rules
  namespace: monitoring
data:
  # 智能决策平台监控规则
  decision-engine.rules: |
    groups:
    - name: decision-engine
      rules:
      - alert: DecisionEngineDown
        expr: up{job="decision-engine"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "决策引擎服务宕机"
          description: "决策引擎服务 {{ $labels.instance }} 已宕机超过1分钟"

      - alert: HighDecisionLatency
        expr: histogram_quantile(0.95, rate(decision_duration_seconds_bucket[5m])) > 1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "决策延迟过高"
          description: "95%的决策请求延迟超过1秒"

      - alert: LowDecisionAccuracy
        expr: decision_accuracy_ratio < 0.90
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "决策准确率过低"
          description: "决策准确率低于90%，当前值: {{ $value }}"

      - alert: HighDecisionErrorRate
        expr: rate(decision_requests_total{status="error"}[5m]) / rate(decision_requests_total[5m]) > 0.05
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "决策错误率过高"
          description: "决策错误率超过5%，当前值: {{ $value }}"

  # 推荐系统监控规则
  recommendation-engine.rules: |
    groups:
    - name: recommendation-engine
      rules:
      - alert: RecommendationServiceDown
        expr: up{job="recommendation-service"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "推荐服务宕机"

      - alert: LowRecommendationClickRate
        expr: recommendation_ctr < 0.10
        for: 15m
        labels:
          severity: warning
        annotations:
          summary: "推荐点击率过低"
          description: "推荐点击率低于10%，当前值: {{ $value }}"

      - alert: HighRecommendationLatency
        expr: histogram_quantile(0.95, rate(recommendation_duration_seconds_bucket[5m])) > 0.5
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "推荐延迟过高"
          description: "95%的推荐请求延迟超过500ms"

  # 智能运营监控规则
  operation-service.rules: |
    groups:
    - name: operation-service
      rules:
      - alert: ScalingOperationFailure
        expr: increase(operation_scaling_failures_total[5m]) > 10
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "伸缩操作频繁失败"
          description: "过去5分钟内伸缩操作失败超过10次"

      - alert: HighResourceUtilization
        expr: avg(resource_utilization_ratio) by (service) > 0.90
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "资源利用率过高"
          description: "服务 {{ $labels.service }} 资源利用率超过90%"
```

#### 2.2 Grafana仪表盘

```json
{
  "dashboard": {
    "id": null,
    "title": "智能决策平台监控",
    "tags": ["intelligent-platform", "ai", "monitoring"],
    "style": "dark",
    "timezone": "browser",
    "refresh": "30s",
    "time": {
      "from": "now-1h",
      "to": "now"
    },
    "panels": [
      {
        "id": 1,
        "title": "决策引擎状态",
        "type": "stat",
        "targets": [
          {
            "expr": "up{job=\"decision-engine\"}",
            "legendFormat": "服务状态"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "color": {
              "mode": "thresholds"
            },
            "thresholds": {
              "steps": [
                {"color": "red", "value": 0},
                {"color": "green", "value": 1}
              ]
            }
          }
        },
        "gridPos": {"h": 8, "w": 6, "x": 0, "y": 0}
      },
      {
        "id": 2,
        "title": "决策请求QPS",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(decision_requests_total[1m])",
            "legendFormat": "{{instance}}"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 6, "y": 0}
      },
      {
        "id": 3,
        "title": "决策延迟分布",
        "type": "graph",
        "targets": [
          {
            "expr": "histogram_quantile(0.50, rate(decision_duration_seconds_bucket[5m]))",
            "legendFormat": "P50"
          },
          {
            "expr": "histogram_quantile(0.95, rate(decision_duration_seconds_bucket[5m]))",
            "legendFormat": "P95"
          },
          {
            "expr": "histogram_quantile(0.99, rate(decision_duration_seconds_bucket[5m]))",
            "legendFormat": "P99"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 0, "y": 8}
      },
      {
        "id": 4,
        "title": "决策准确率",
        "type": "graph",
        "targets": [
          {
            "expr": "decision_accuracy_ratio",
            "legendFormat": "准确率"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 12, "y": 8}
      },
      {
        "id": 5,
        "title": "资源利用率",
        "type": "graph",
        "targets": [
          {
            "expr": "avg(resource_utilization_ratio) by (service)",
            "legendFormat": "{{service}}"
          }
        ],
        "gridPos": {"h": 8, "w": 24, "x": 0, "y": 16}
      }
    ]
  }
}
```

#### 2.3 监控配置脚本

```bash
#!/bin/bash
# setup-monitoring.sh - 监控环境配置脚本

set -e

NAMESPACE="monitoring"

echo "配置监控环境..."

# 1. 安装Prometheus Operator
echo "安装Prometheus Operator..."
kubectl apply -f https://raw.githubusercontent.com/prometheus-operator/prometheus-operator/main/bundle.yaml

# 2. 等待Operator就绪
echo "等待Prometheus Operator就绪..."
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=prometheus-operator \
  --namespace default --timeout=300s

# 3. 创建Prometheus实例
echo "创建Prometheus实例..."
kubectl apply -f - <<EOF
apiVersion: monitoring.coreos.com/v1
kind: Prometheus
metadata:
  name: prometheus
  namespace: ${NAMESPACE}
spec:
  serviceAccountName: prometheus
  serviceMonitorSelector:
    matchLabels:
      team: platform
  ruleSelector:
    matchLabels:
      prometheus: main
  resources:
    requests:
      memory: 2Gi
      cpu: 1000m
    limits:
      memory: 4Gi
      cpu: 2000m
EOF

# 4. 创建ServiceMonitor
echo "创建ServiceMonitor..."
kubectl apply -f - <<EOF
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: decision-engine
  namespace: ${NAMESPACE}
  labels:
    team: platform
spec:
  selector:
    matchLabels:
      app: decision-engine
  endpoints:
  - port: http
    path: /actuator/prometheus
    interval: 30s
EOF

# 5. 应用告警规则
echo "应用告警规则..."
kubectl apply -f monitoring/metrics.yaml

# 6. 配置Alertmanager
echo "配置Alertmanager..."
kubectl apply -f - <<EOF
apiVersion: monitoring.coreos.com/v1
kind: Alertmanager
metadata:
  name: main
  namespace: ${NAMESPACE}
spec:
  replicas: 3
EOF

# 7. 安装Grafana
echo "安装Grafana..."
helm repo add grafana https://grafana.github.io/helm-charts
helm upgrade --install grafana grafana/grafana \
  --namespace ${NAMESPACE} \
  --set adminPassword='admin123' \
  --set persistence.enabled=true

# 8. 导入仪表盘
echo "导入Grafana仪表盘..."
GRAFANA_POD=$(kubectl get pods -n ${NAMESPACE} -l app.kubernetes.io/name=grafana -o jsonpath='{.items[0].metadata.name}')

# 创建数据源
kubectl exec -n ${NAMESPACE} ${GRAFANA_POD} -- \
  grafana-cli plugins install grafana-piechart-panel

# 导入仪表盘配置
kubectl create configmap grafana-dashboard \
  --from-file=monitoring/grafana-dashboard.json \
  --namespace ${NAMESPACE} \
  --dry-run=client -o yaml | kubectl apply -f -

echo "监控环境配置完成!"
echo "Grafana访问地址: http://$(kubectl get svc grafana -n ${NAMESPACE} -o jsonpath='{.status.loadBalancer.ingress[0].ip}')"
echo "用户名: admin"
echo "密码: admin123"
```

### 3. 维护手册

#### 3.1 日常维护任务

```bash
#!/bin/bash
# daily-maintenance.sh - 日常维护脚本

set -e

NAMESPACE="intelligent-platform"
LOG_FILE="/var/log/platform-maintenance-$(date +%Y%m%d).log"

echo "$(date): 开始日常维护任务" | tee -a ${LOG_FILE}

# 1. 检查服务状态
echo "$(date): 检查服务状态" | tee -a ${LOG_FILE}
kubectl get pods -n ${NAMESPACE} | tee -a ${LOG_FILE}

# 检查失败的Pod
FAILED_PODS=$(kubectl get pods -n ${NAMESPACE} -o jsonpath='{.items[*].metadata.name}')
for pod in $FAILED_PODS; do
    if [ -n "$pod" ]; then
        STATUS=$(kubectl get pod ${pod} -n ${NAMESPACE} -o jsonpath='{.status.phase}')
        if [ "$STATUS" != "Running" ]; then
            echo "$(date): 警告: Pod ${pod} 状态异常: ${STATUS}" | tee -a ${LOG_FILE}
            kubectl describe pod ${pod} -n ${NAMESPACE} | tee -a ${LOG_FILE}
        fi
    fi
done

# 2. 检查资源使用情况
echo "$(date): 检查资源使用情况" | tee -a ${LOG_FILE}
kubectl top pods -n ${NAMESPACE} --sort-by=memory | tee -a ${LOG_FILE}
kubectl top pods -n ${NAMESPACE} --sort-by=cpu | tee -a ${LOG_FILE}

# 3. 检查数据库连接
echo "$(date): 检查数据库连接" | tee -a ${LOG_FILE}
PG_POD=$(kubectl get pods -n ${NAMESPACE} -l app.kubernetes.io/name=postgresql -o jsonpath='{.items[0].metadata.name}')
kubectl exec -n ${NAMESPACE} ${PG_POD} -- \
  psql -U postgres -c "SELECT version();" | tee -a ${LOG_FILE}

# 4. 清理日志
echo "$(date): 清理过期日志" | tee -a ${LOG_FILE}
kubectl logs -n ${NAMESPACE} --since=24h > /tmp/platform-logs-backup-$(date +%Y%m%d).log
find /var/log -name "*.log" -mtime +30 -delete

# 5. 备份重要配置
echo "$(date): 备份配置" | tee -a ${LOG_FILE}
kubectl get all -n ${NAMESPACE} -o yaml > /tmp/backup-config-$(date +%Y%m%d).yaml

# 6. 更新Prometheus metrics
echo "$(date): 更新Prometheus metrics" | tee -a ${LOG_FILE}
curl -X POST http://prometheus:9090/-/reload || echo "Prometheus reload failed"

# 7. 检查磁盘使用
echo "$(date): 检查磁盘使用" | tee -a ${LOG_FILE}
df -h | tee -a ${LOG_FILE}

# 检查PVC使用情况
kubectl get pvc -n ${NAMESPACE} | tee -a ${LOG_FILE}

# 8. 检查证书到期时间
echo "$(date): 检查证书到期时间" | tee -a ${LOG_FILE}
kubectl get certificates -n ${NAMESPACE} -o json | \
  jq -r '.items[] | select(.status.notAfter | fromdateiso8601 < now + 86400*30) | .metadata.name' | \
  while read cert; do
    echo "警告: 证书 ${cert} 即将过期" | tee -a ${LOG_FILE}
  done

# 9. 性能统计
echo "$(date): 生成性能统计" | tee -a ${LOG_FILE}

# QPS统计
echo "过去1小时QPS:" | tee -a ${LOG_FILE}
kubectl exec -n monitoring prometheus-prometheus-0 -- \
  curl -s "http://localhost:9090/api/v1/query?query=rate(decision_requests_total[1h])" | \
  jq -r '.data.result[] | "\(.metric.instance): \(.value[1])"' | tee -a ${LOG_FILE}

# 延迟统计
echo "过去1小时P95延迟:" | tee -a ${LOG_FILE}
kubectl exec -n monitoring prometheus-prometheus-0 -- \
  curl -s "http://localhost:9090/api/v1/query?query=histogram_quantile(0.95, rate(decision_duration_seconds_bucket[1h]))" | \
  jq -r '.data.result[] | "\(.metric.instance): \(.value[1])"' | tee -a ${LOG_FILE}

echo "$(date): 日常维护任务完成" | tee -a ${LOG_FILE}
```

#### 3.2 性能优化指南

```markdown
# 性能优化指南

## 1. JVM调优

### 决策引擎JVM参数
```bash
JAVA_OPTS="
-Xms4g -Xmx4g                    # 堆内存4GB
-XX:NewRatio=3                   # 新生代:老年代 = 1:3
-XX:SurvivorRatio=8              # Eden:Survivor = 8:1
-XX:+UseG1GC                     # 使用G1垃圾收集器
-XX:MaxGCPauseMillis=200         # 最大GC暂停时间200ms
-XX:G1HeapRegionSize=16m         # G1堆区域大小16MB
-XX:+UseStringDeduplication      # 字符串去重
-XX:+HeapDumpOnOutOfMemoryError  # OOM时生成堆转储
-XX:HeapDumpPath=/dumps/         # 堆转储文件路径
-XX:+UnlockExperimentalVMOptions
-XX:+UseJVMCICompiler
"
```

### 推荐服务JVM参数
```bash
JAVA_OPTS="
-Xms2g -Xmx2g                    # 堆内存2GB
-XX:+UseParallelGC               # 使用Parallel GC
-XX:ParallelGCThreads=4          # 并行GC线程数
-XX:+AggressiveOpts              # 启用激进优化
-XX:+UseFastAccessorMethods      # 快速访问方法
-XshowSettings:vm                # 显示VM设置
"
```

## 2. 数据库优化

### PostgreSQL配置优化
```sql
-- postgresql.conf
shared_buffers = 1GB                  # 共享缓冲区
effective_cache_size = 3GB            # 有效缓存大小
work_mem = 16MB                       # 工作内存
maintenance_work_mem = 256MB          # 维护工作内存
checkpoint_completion_target = 0.9    # 检查点完成目标
wal_buffers = 16MB                    # WAL缓冲区
default_statistics_target = 1000      # 默认统计目标
random_page_cost = 1.1                # 随机页成本
effective_io_concurrency = 200        # 有效IO并发数
```

### Redis配置优化
```conf
# redis.conf
maxmemory 2gb                        # 最大内存2GB
maxmemory-policy allkeys-lru         # LRU淘汰策略
save 900 1                           # 900秒内1个key变更时保存
save 300 10                          # 300秒内10个key变更时保存
save 60 10000                        # 60秒内10000个key变更时保存
tcp-keepalive 300                    # TCP keepalive 300秒
timeout 300                          # 客户端超时300秒
tcp-backlog 511                      # TCP backlog 511
```

## 3. 连接池优化

### HikariCP配置
```yaml
spring:
  datasource:
    hikari:
      minimum-idle: 10                # 最小空闲连接数
      maximum-pool-size: 50           # 最大连接池大小
      connection-timeout: 30000       # 连接超时30秒
      idle-timeout: 600000            # 空闲超时10分钟
      max-lifetime: 1800000           # 最大生命周期30分钟
      leak-detection-threshold: 60000 # 泄漏检测阈值60秒
      pool-name: IntelligentPlatformHikariCP
```

### Redis连接池配置
```yaml
spring:
  redis:
    lettuce:
      pool:
        max-active: 100               # 最大活跃连接数
        max-idle: 20                  # 最大空闲连接数
        min-idle: 5                   # 最小空闲连接数
        max-wait: 2000ms              # 最大等待时间2秒
```

## 4. 缓存策略

### 多级缓存架构
```java
@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        RedisCacheManager.Builder builder = RedisCacheManager
            .RedisCacheManagerBuilder
            .fromConnectionFactory(redisConnectionFactory())
            .cacheDefaults(getCacheConfiguration(Duration.ofMinutes(10)));

        // 配置不同缓存的过期时间
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put("decision_cache",
            getCacheConfiguration(Duration.ofMinutes(30)));
        cacheConfigurations.put("recommendation_cache",
            getCacheConfiguration(Duration.ofHours(1)));
        cacheConfigurations.put("feature_cache",
            getCacheConfiguration(Duration.ofMinutes(5)));

        builder.withInitialCacheConfigurations(cacheConfigurations);
        return builder.build();
    }

    private RedisCacheConfiguration getCacheConfiguration(Duration ttl) {
        return RedisCacheConfiguration
            .defaultCacheConfig()
            .entryTtl(ttl)
            .disableCachingNullValues()
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));
    }
}
```

## 5. 异步处理优化

### 线程池配置
```java
@Configuration
public class AsyncConfig {

    @Bean("decisionExecutor")
    public ThreadPoolTaskScheduler decisionExecutor() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(20);
        scheduler.setThreadNamePrefix("decision-");
        scheduler.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(60);
        scheduler.initialize();
        return scheduler;
    }

    @Bean("recommendationExecutor")
    public ThreadPoolTaskScheduler recommendationExecutor() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(30);
        scheduler.setThreadNamePrefix("recommendation-");
        scheduler.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        scheduler.initialize();
        return scheduler;
    }
}
```

## 6. API限流优化

### Sentinel限流配置
```java
@Component
public class SentinelConfig {

    @PostConstruct
    public void initRules() {
        // 决策API限流
        FlowRule decisionFlowRule = new FlowRule("decision-api")
            .setGrade(RuleConstant.FLOW_GRADE_QPS)
            .setCount(100) // 每秒100次请求
            .setLimitApp("default")
            .setStrategy(RuleConstant.STRATEGY_DIRECT);
        FlowRuleManager.loadRules(Collections.singletonList(decisionFlowRule));

        // 推荐API限流
        FlowRule recommendationFlowRule = new FlowRule("recommendation-api")
            .setGrade(RuleConstant.FLOW_GRADE_QPS)
            .setCount(200) // 每秒200次请求
            .setLimitApp("default")
            .setStrategy(RuleConstant.STRATEGY_DIRECT);
        FlowRuleManager.loadRules(Arrays.asList(decisionFlowRule, recommendationFlowRule));
    }
}
```
```

### 4. 升级指南

```bash
#!/bin/bash
# upgrade-platform.sh - 平台升级脚本

set -e

OLD_VERSION=$1
NEW_VERSION=$2
NAMESPACE="intelligent-platform"
RELEASE_NAME="intelligent-platform"

if [ -z "$OLD_VERSION" ] || [ -z "$NEW_VERSION" ]; then
    echo "用法: $0 <旧版本> <新版本>"
    echo "示例: $0 1.0.0 1.1.0"
    exit 1
fi

echo "=================================="
echo "智能平台升级"
echo "版本: ${OLD_VERSION} -> ${NEW_VERSION}"
echo "=================================="

# 1. 升级前检查
echo "执行升级前检查..."
kubectl get nodes
kubectl get pods -n ${NAMESPACE}
helm list -n ${NAMESPACE}

# 2. 创建备份
echo "创建配置和数据备份..."
kubectl get all -n ${NAMESPACE} -o yaml > backup-pre-upgrade-${NEW_VERSION}.yaml

# 备份数据库
echo "备份数据库..."
PG_POD=$(kubectl get pods -n ${NAMESPACE} -l app.kubernetes.io/name=postgresql -o jsonpath='{.items[0].metadata.name}')
kubectl exec -n ${NAMESPACE} ${PG_POD} -- \
  pg_dump -U postgres -d intelligent_platform > backup-db-${NEW_VERSION}.sql

# 3. 滚动升级
echo "开始滚动升级..."
helm upgrade ${RELEASE_NAME} ./deployment/helm \
  --namespace ${NAMESPACE} \
  --set image.tag=${NEW_VERSION} \
  --wait \
  --timeout=600s

# 4. 升级后验证
echo "验证升级结果..."
sleep 30

# 检查Pod状态
kubectl get pods -n ${NAMESPACE}
kubectl rollout status deployment/api-gateway -n ${NAMESPACE}
kubectl rollout status deployment/decision-engine -n ${NAMESPACE}
kubectl rollout status deployment/recommendation-service -n ${NAMESPACE}

# 健康检查
echo "执行健康检查..."
API_ENDPOINT="http://$(kubectl get svc -n ${NAMESPACE} api-gateway -o jsonpath='{.status.loadBalancer.ingress[0].ip}')"

if curl -f "${API_ENDPOINT}/actuator/health"; then
    echo "✓ 健康检查通过"
else
    echo "✗ 健康检查失败"
    exit 1
fi

# 功能测试
echo "执行功能测试..."

# 测试决策API
TEST_REQUEST='{"context":{"userId":"test_user","scenario":"transaction_risk"},"features":{"amount":"1000"}}'
RESPONSE=$(curl -s -X POST -H "Content-Type: application/json" -d "${TEST_REQUEST}" "${API_ENDPOINT}/api/v1/decision")
if echo "$RESPONSE" | jq -e '.success' > /dev/null; then
    echo "✓ 决策API测试通过"
else
    echo "✗ 决策API测试失败"
    echo "$RESPONSE"
    exit 1
fi

# 测试推荐API
TEST_REQUEST='{"userId":"test_user","scenario":"product_recommendation","topN":10}'
RESPONSE=$(curl -s -X POST -H "Content-Type: application/json" -d "${TEST_REQUEST}" "${API_ENDPOINT}/api/v1/recommendation")
if echo "$RESPONSE" | jq -e '.userId' > /dev/null; then
    echo "✓ 推荐API测试通过"
else
    echo "✗ 推荐API测试失败"
    echo "$RESPONSE"
    exit 1
fi

# 5. 清理旧版本镜像
echo "清理旧版本镜像..."
kubectl set image deployment/decision-engine decision-engine=intelligent-platform/decision:${NEW_VERSION} -n ${NAMESPACE}
kubectl set image deployment/recommendation-service recommendation-service=intelligent-platform/recommendation:${NEW_VERSION} -n ${NAMESPACE}

# 6. 更新监控配置
echo "更新监控配置..."
kubectl apply -f monitoring/updated-rules.yaml

echo "=================================="
echo "升级完成!"
echo "新版本: ${NEW_VERSION}"
echo "=================================="
```

---

## 🚨 故障处理指南

### 1. 常见故障排查

#### 1.1 服务不可用

```bash
#!/bin/bash
# troubleshoot-unavailable-service.sh - 服务不可用排查脚本

SERVICE_NAME=$1
NAMESPACE=$2

if [ -z "$SERVICE_NAME" ] || [ -z "$NAMESPACE" ]; then
    echo "用法: $0 <服务名称> <命名空间>"
    echo "示例: $0 decision-engine intelligent-platform"
    exit 1
fi

echo "=================================="
echo "排查服务: ${SERVICE_NAME}"
echo "命名空间: ${NAMESPACE}"
echo "=================================="

# 1. 检查Deployment状态
echo "1. 检查Deployment状态..."
kubectl get deployment ${SERVICE_NAME} -n ${NAMESPACE}
DESIRED=$(kubectl get deployment ${SERVICE_NAME} -n ${NAMESPACE} -o jsonpath='{.spec.replicas}')
READY=$(kubectl get deployment ${SERVICE_NAME} -n ${NAMESPACE} -o jsonpath='{.status.readyReplicas}')
echo "期望副本数: ${DESIRED}, 就绪副本数: ${READY}"

if [ "$DESIRED" != "$READY" ]; then
    echo "✗ Deployment副本数不匹配"
fi

# 2. 检查Pod状态
echo "2. 检查Pod状态..."
PODS=$(kubectl get pods -n ${NAMESPACE} -l app=${SERVICE_NAME} -o jsonpath='{.items[*].metadata.name}')
for pod in $PODS; do
    STATUS=$(kubectl get pod ${pod} -n ${NAMESPACE} -o jsonpath='{.status.phase}')
    echo "Pod: ${pod}, 状态: ${STATUS}"

    if [ "$STATUS" != "Running" ]; then
        echo "描述Pod信息:"
        kubectl describe pod ${pod} -n ${NAMESPACE}

        echo "检查Pod日志:"
        kubectl logs ${pod} -n ${NAMESPACE} --tail=50
    fi
done

# 3. 检查Service状态
echo "3. 检查Service状态..."
SERVICE=$(kubectl get svc -n ${NAMESPACE} -l app=${SERVICE_NAME} -o jsonpath='{.items[0].metadata.name}')
if [ -n "$SERVICE" ]; then
    echo "Service: ${SERVICE}"
    kubectl get svc ${SERVICE} -n ${NAMESPACE}

    # 检查Endpoints
    echo "检查Endpoints:"
    kubectl get endpoints ${SERVICE} -n ${NAMESPACE}
else
    echo "✗ 未找到Service"
fi

# 4. 检查资源使用
echo "4. 检查资源使用..."
kubectl top pods -n ${NAMESPACE} -l app=${SERVICE_NAME}

# 5. 检查事件
echo "5. 检查相关事件..."
kubectl get events -n ${NAMESPACE} --sort-by='.lastTimestamp' | grep ${SERVICE_NAME} | tail -20

# 6. 检查配置
echo "6. 检查配置..."
echo "环境变量:"
kubectl get deployment ${SERVICE_NAME} -n ${NAMESPACE} -o jsonpath='{.spec.template.spec.containers[0].env}' | jq

echo "挂载卷:"
kubectl get deployment ${SERVICE_NAME} -n ${NAMESPACE} -o jsonpath='{.spec.template.spec.containers[0].volumeMounts}' | jq

# 7. 网络诊断
echo "7. 网络诊断..."
# 尝试curl测试端点
if [ -n "$SERVICE" ]; then
    PORT=$(kubectl get svc ${SERVICE} -n ${NAMESPACE} -o jsonpath='{.spec.ports[0].port}')
    echo "尝试访问服务 ${SERVICE}:${PORT}..."

    # 如果有sidecar容器，可以进行网络测试
    # kubectl exec -it <pod> -n ${NAMESPACE} -- curl http://${SERVICE}:${PORT}/actuator/health
fi

echo "=================================="
echo "排查完成"
echo "=================================="
```

#### 1.2 高延迟问题

```bash
#!/bin/bash
# troubleshoot-high-latency.sh - 高延迟问题排查脚本

NAMESPACE="intelligent-platform"

echo "=================================="
echo "排查高延迟问题"
echo "=================================="

# 1. 检查响应时间指标
echo "1. 检查响应时间指标..."
kubectl exec -n monitoring prometheus-prometheus-0 -- \
  curl -s "http://localhost:9090/api/v1/query?query=histogram_quantile(0.95, rate(request_duration_seconds_bucket[5m]))" | \
  jq -r '.data.result[] | "\(.metric.instance): P95=\(.value[1])s"'

kubectl exec -n monitoring prometheus-prometheus-0 -- \
  curl -s "http://localhost:9090/api/v1/query?query=histogram_quantile(0.99, rate(request_duration_seconds_bucket[5m]))" | \
  jq -r '.data.result[] | "\(.metric.instance): P99=\(.value[1])s"'

# 2. 检查资源使用情况
echo "2. 检查CPU使用率..."
kubectl top pods -n ${NAMESPACE} --sort-by=cpu

echo "检查内存使用率..."
kubectl top pods -n ${NAMESPACE} --sort-by=memory

# 3. 检查GC情况
echo "3. 检查GC情况..."
PODS=$(kubectl get pods -n ${NAMESPACE} -o jsonpath='{.items[*].metadata.name}')
for pod in $PODS; do
    echo "Pod: ${pod}"
    kubectl exec -n ${NAMESPACE} ${pod} -- \
      jstat -gc $(jps | grep -v jps | awk '{print $1}') | tail -1
done

# 4. 检查数据库性能
echo "4. 检查数据库性能..."
PG_POD=$(kubectl get pods -n ${NAMESPACE} -l app.kubernetes.io/name=postgresql -o jsonpath='{.items[0].metadata.name}')
kubectl exec -n ${NAMESPACE} ${PG_POD} -- \
  psql -U postgres -d intelligent_platform -c "
    SELECT query, mean_exec_time, calls
    FROM pg_stat_statements
    ORDER BY mean_exec_time DESC
    LIMIT 10;"

# 5. 检查缓存命中率
echo "5. 检查Redis缓存..."
REDIS_POD=$(kubectl get pods -n ${NAMESPACE} -l app.kubernetes.io/name=redis -o jsonpath='{.items[0].metadata.name}')
kubectl exec -n ${NAMESPACE} ${REDIS_POD} -- redis-cli info stats | grep keyspace

# 6. 检查网络延迟
echo "6. 检查网络延迟..."
kubectl exec -n ${NAMESPACE} ${PG_POD} -- ping -c 3 redis

# 7. 分析慢查询
echo "7. 分析慢查询日志..."
for pod in $PODS; do
    echo "Pod: ${pod} 慢查询:"
    kubectl exec -n ${NAMESPACE} ${pod} -- \
      find /var/log -name "*.log" -exec grep -l "slow" {} \; | head -5
done

echo "=================================="
echo "高延迟排查完成"
echo "=================================="
```

### 2. 应急响应流程

```markdown
# 应急响应手册

## 响应级别定义

### P0 - 严重 (Critical)
- **影响范围**: 整个平台不可用
- **响应时间**: 15分钟内
- **恢复时间**: 1小时内
- **示例**: 数据库宕机、所有API服务不可用

### P1 - 高 (High)
- **影响范围**: 核心功能受损
- **响应时间**: 30分钟内
- **恢复时间**: 4小时内
- **示例**: 决策引擎不可用、推荐系统故障

### P2 - 中 (Medium)
- **影响范围**: 部分功能受影响
- **响应时间**: 1小时内
- **恢复时间**: 24小时内
- **示例**: 单个服务实例故障、监控告警

### P3 - 低 (Low)
- **影响范围**: 非核心功能问题
- **响应时间**: 4小时内
- **恢复时间**: 72小时内
- **示例**: 文档错误、非关键配置问题

## 应急响应流程

### 阶段1: 检测和初步响应 (0-15分钟)

1. **接收告警**
   - 监控系统自动告警
   - 用户反馈
   - 运维团队发现

2. **快速评估**
   - 确定影响范围
   - 评估严重程度
   - 启动相应级别响应

3. **组建应急团队**
   - P0/P1: 立即组建
   - P2: 30分钟内组建
   - P3: 4小时内组建

### 阶段2: 故障定位和诊断 (15-30分钟)

1. **日志分析**
   ```bash
   # 查看关键日志
   kubectl logs -f deployment/decision-engine -n intelligent-platform --tail=100

   # 搜索错误信息
   kubectl logs -f deployment/decision-engine -n intelligent-platform | grep ERROR
   ```

2. **资源检查**
   ```bash
   # 检查Pod状态
   kubectl get pods -n intelligent-platform

   # 检查资源使用
   kubectl top pods -n intelligent-platform

   # 检查节点状态
   kubectl get nodes
   ```

3. **性能指标分析**
   - 查看Grafana仪表盘
   - 分析Prometheus指标
   - 检查APM追踪信息

### 阶段3: 故障缓解 (30分钟-1小时)

1. **立即缓解措施**
   - 重启故障服务
   - 扩容实例数量
   - 回滚到稳定版本

2. **实施解决方案**
   - 根据故障分析结果
   - 应用修复补丁
   - 更新配置

3. **验证修复效果**
   - 检查服务恢复
   - 监控指标正常
   - 用户验证通过

### 阶段4: 根因分析和改进 (故障恢复后)

1. **故障复盘**
   - 时间线梳理
   - 根本原因分析
   - 影响评估

2. **制定改进计划**
   - 预防措施
   - 监控增强
   - 流程优化

3. **文档更新**
   - 更新故障处理手册
   - 完善应急预案
   - 知识库更新

## 应急联系人

| 角色 | 姓名 | 电话 | 邮箱 |
|------|------|------|------|
| 技术负责人 | 张三 | 138-0000-0000 | zhangsan@example.com |
| 运维负责人 | 李四 | 138-0000-0001 | lisi@example.com |
| 开发负责人 | 王五 | 138-0000-0002 | wangwu@example.com |
| 数据库专家 | 赵六 | 138-0000-0003 | zhaoliu@example.com |

## 常用故障处理命令

```bash
# 快速重启服务
kubectl rollout restart deployment/decision-engine -n intelligent-platform

# 查看服务日志
kubectl logs -f deployment/decision-engine -n intelligent-platform

# 进入Pod调试
kubectl exec -it <pod-name> -n intelligent-platform -- /bin/bash

# 查看资源使用
kubectl top pods -n intelligent-platform

# 检查证书到期
kubectl get certificates -n intelligent-platform -o json | jq -r '.items[] | select(.status.notAfter | fromdateiso8601 < now + 86400*30) | .metadata.name'

# 数据库连接测试
kubectl exec -it postgresql-0 -n intelligent-platform -- psql -U postgres -d intelligent_platform -c "SELECT 1;"

# Redis连接测试
kubectl exec -it redis-0 -n intelligent-platform -- redis-cli ping

# 网络连通性测试
kubectl run -it --rm debug --image=nicolaka/netshoot --restart=Never -- <pod-name>

# 查看事件
kubectl get events -n intelligent-platform --sort-by='.lastTimestamp'
```

## 常见故障场景及解决方案

### 场景1: 内存溢出 (OOM)

**症状**
- Pod重启频繁
- 应用日志中出现OutOfMemoryError
- 性能急剧下降

**解决方案**
```bash
# 1. 查看Pod资源使用
kubectl top pods -n intelligent-platform

# 2. 增加内存限制
kubectl patch deployment decision-engine -n intelligent-platform \
  -p '{"spec":{"template":{"spec":{"containers":[{"name":"decision-engine","resources":{"limits":{"memory":"8Gi"},"requests":{"memory":"4Gi"}}}]}}}}'

# 3. 分析堆转储文件
kubectl exec -it <pod> -n intelligent-platform -- jmap -dump:live,format=b,file=/tmp/heap.hprof <pid>

# 4. 优化JVM参数
kubectl set env deployment decision-engine -n intelligent-platform \
  JAVA_OPTS="-Xms4g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

### 场景2: 数据库连接池耗尽

**症状**
- 应用响应超时
- 数据库连接错误日志
- 线程池阻塞

**解决方案**
```bash
# 1. 检查连接池状态
kubectl exec -it postgresql-0 -n intelligent-platform -- \
  psql -U postgres -c "SELECT count(*) FROM pg_stat_activity;"

# 2. 调整连接池参数
kubectl set env deployment decision-engine -n intelligent-platform \
  SPRING_DATASOURCE_HIKARI_MAXIMUM-POOLSIZE=50 \
  SPRING_DATASOURCE_HIKARI_MINIMUM-IDLE=10

# 3. 重启服务应用配置
kubectl rollout restart deployment decision-engine -n intelligent-platform
```

### 场景3: 磁盘空间不足

**症状**
- Pod无法启动
- 日志写入失败
- 数据持久化错误

**解决方案**
```bash
# 1. 检查磁盘使用
kubectl exec -it <pod> -n intelligent-platform -- df -h

# 2. 清理临时文件
kubectl exec -it <pod> -n intelligent-platform -- \
  find /tmp -type f -mtime +7 -delete

# 3. 扩容PVC
kubectl patch pvc <pvc-name> -n intelligent-platform \
  --patch '{"spec":{"resources":{"requests":{"storage":"100Gi"}}}}'

# 4. 配置日志轮转
kubectl set env deployment decision-engine -n intelligent-platform \
  LOG_ROTATION_ENABLED=true
```
```

---

## 📊 最佳实践

### 1. 性能优化最佳实践

```markdown
# 智能平台性能优化指南

## 1. 代码层面优化

### 异步处理
```java
// 使用异步调用提升性能
@Async("taskExecutor")
public CompletableFuture<DecisionResult> makeDecisionAsync(DecisionRequest request) {
    return CompletableFuture.supplyAsync(() -> {
        try {
            return makeDecision(request);
        } catch (Exception e) {
            throw new CompletionException(e);
        }
    });
}

// 批量处理提升吞吐量
public List<DecisionResult> makeBatchDecision(List<DecisionRequest> requests) {
    return requests.parallelStream()
        .map(this::makeDecision)
        .collect(Collectors.toList());
}
```

### 缓存策略
```java
// 多级缓存
@Cacheable(value = "decision_cache", key = "#request.context.userId + '_' + #request.features")
public DecisionResult makeDecisionCached(DecisionRequest request) {
    return makeDecision(request);
}

// 缓存预热
@PostConstruct
public void warmUpCache() {
    // 预加载热门用户数据
    List<String> hotUsers = userService.getHotUsers();
    hotUsers.parallelStream().forEach(userId -> {
        // 触发缓存加载
        decisionCache.get(userId);
    });
}
```

## 2. 数据库优化

### 索引优化
```sql
-- 决策记录表索引
CREATE INDEX CONCURRENTLY idx_decision_user_time ON decision_records(user_id, created_at);
CREATE INDEX CONCURRENTLY idx_decision_scenario ON decision_records(scenario, created_at);

-- 复合索引优化
CREATE INDEX CONCURRENTLY idx_decision_composite ON decision_records(user_id, scenario, status)
WHERE status IN ('APPROVE', 'REJECT');

-- 分区表优化（按时间分区）
CREATE TABLE decision_records_2025_11 PARTITION OF decision_records
FOR VALUES FROM ('2025-11-01') TO ('2025-12-01');
```

### 查询优化
```java
// 使用JOIN优化
@Query("SELECT d FROM Decision d JOIN FETCH d.features WHERE d.userId = :userId")
List<Decision> findByUserIdWithFeatures(@Param("userId") String userId);

// 分页查询优化
Page<Decision> findByStatusOrderByCreatedAtDesc(
    String status,
    Pageable pageable
);

// 批量插入优化
@Transactional
public void batchSaveDecisions(List<Decision> decisions) {
    String sql = "INSERT INTO decision (...) VALUES (...)";
    jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
        @Override
        public void setValues(PreparedStatement ps, int i) throws SQLException {
            Decision d = decisions.get(i);
            ps.setString(1, d.getId());
            // ... 设置其他参数
        }

        @Override
        public int getBatchSize() {
            return decisions.size();
        }
    });
}
```

## 3. 微服务优化

### 服务发现和注册
```yaml
# application.yml
spring:
  cloud:
    service-registry:
      auto-registration:
        enabled: true
        register-health-check: true
    nacos:
      discovery:
        server-addr: nacos:8848
        namespace: intelligent-platform
        metadata:
          version: 1.0.0
          zone: zone-1
```

### 熔断和降级
```java
// 使用Resilience4j实现熔断
@CircuitBreaker(name = "decisionService", fallbackMethod = "fallbackDecision")
@RateLimiter(name = "decisionService")
public DecisionResult makeDecision(DecisionRequest request) {
    return decisionClient.makeDecision(request);
}

public DecisionResult fallbackDecision(DecisionRequest request, Exception ex) {
    return DecisionResult.builder()
        .decision(DecisionEnum.REVIEW)
        .confidence(0.5)
        .explanation("服务降级，采用保守策略")
        .build();
}
```

### API网关优化
```yaml
# gateway配置
spring:
  cloud:
    gateway:
      routes:
      - id: decision-service
        uri: lb://decision-service
        predicates:
        - Path=/api/v1/decision/**
        filters:
        - name: RequestRateLimiter
          args:
            rate-limiter: "#{@redisRateLimiter}"
            key-resolver: "#{@userKeyResolver}"
        - name: CircuitBreaker
          args:
            name: decision-circuit-breaker
            fallbackUri: forward:/fallback/decision
```

## 4. 监控优化

### 关键指标监控
```java
// 自定义指标
@Component
public class CustomMetrics {

    private final MeterRegistry meterRegistry;

    public CustomMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordDecisionLatency(Duration latency) {
        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(Timer.builder("decision_latency")
            .description("决策延迟")
            .register(meterRegistry));
    }

    public void incrementDecisionRequests(String scenario) {
        Counter.builder("decision_requests")
            .tag("scenario", scenario)
            .register(meterRegistry)
            .increment();
    }
}
```

### 日志优化
```xml
<!-- logback-spring.xml -->
<configuration>
    <appender name="ASYNC_FILE" class="ch.qos.logback.classic.AsyncAppender">
        <appender-ref ref="FILE"/>
        <queueSize>1024</queueSize>
        <discardingThreshold>0</discardingThreshold>
    </appender>

    <logger name="com.intelligentplatform" level="INFO" additivity="false">
        <appender-ref ref="ASYNC_FILE"/>
        <appender-ref ref="CONSOLE"/>
    </logger>

    <root level="WARN">
        <appender-ref ref="ASYNC_FILE"/>
    </root>
</configuration>
```

## 5. 安全最佳实践

### API安全
```java
// API限流
@Component
public class RateLimitingInterceptor implements HandlerInterceptor {

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request,
                           HttpServletResponse response,
                           Object handler) {
        String userId = getCurrentUserId(request);
        String key = "rate_limit:" + userId;

        long count = redisTemplate.opsForValue().increment(key);
        if (count == 1) {
            redisTemplate.expire(key, Duration.ofMinutes(1));
        }

        if (count > 100) { // 每分钟100次请求限制
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            return false;
        }

        return true;
    }
}

// 数据加密
@Configuration
public class EncryptionConfig {

    @Bean
    public StringEncryptor stringEncryptor() {
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        config.setPassword("encryption-key");
        config.setAlgorithm("PBEWITHHMACSHA512ANDAES_256");
        config.setKeyObtentionIterations("1000");
        encryptor.setConfig(config);
        return encryptor;
    }
}
```

### 认证和授权
```java
// JWT令牌验证
@Component
public class JwtTokenProvider {

    public Authentication getAuthentication(String token) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(getUsername(token));
        return new UsernamePasswordAuthenticationToken(
            userDetails, "", userDetails.getAuthorities());
    }

    public boolean validateToken(String token) {
        try {
            Jws<Claims> claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token);

            return !claims.getPayload().getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}

// RBAC权限控制
@PreAuthorize("hasRole('ADMIN') or hasRole('USER') and #userId == authentication.name")
public DecisionResult makeDecision(String userId, DecisionRequest request) {
    // 业务逻辑
}
```
```

### 2. 成本优化最佳实践

```markdown
# 成本优化实践

## 1. 资源优化

### 容器资源优化
```yaml
# Kubernetes资源限制
resources:
  requests:
    cpu: "500m"
    memory: "1Gi"
  limits:
    cpu: "2000m"
    memory: "4Gi"

# 基于QPS动态调整副本数
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: decision-engine-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: decision-engine
  minReplicas: 2
  maxReplicas: 20
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

### 存储成本优化
```bash
# 存储生命周期管理
kubectl apply -f - <<EOF
apiVersion: v1
kind: ConfigMap
metadata:
  name: storage-policy
data:
  policy.yaml: |
    rules:
    - name: decision-logs
      match:
        labels:
          type: decision-log
      storageClass: standard
      retention:
        days: 7
    - name: decision-archive
      match:
        labels:
          type: decision-archive
      storageClass: glacier
      retention:
        years: 7
EOF
```

### 数据库成本优化
```sql
-- 数据归档
DELETE FROM decision_records
WHERE created_at < NOW() - INTERVAL '2 years'
RETURNING *;

-- 数据压缩
ALTER TABLE decision_records SET (toast.autovacuum_enabled = true);

-- 分区清理
DROP TABLE decision_records_2023_01 PARTITION OF decision_records
FOR VALUES FROM ('2023-01-01') TO ('2023-02-01');
```

## 2. 云资源优化

### 自动缩容配置
```yaml
# CronJob自动缩容
apiVersion: batch/v1
kind: CronJob
metadata:
  name: auto-scaler
spec:
  schedule: "0 2 * * *"  # 每天凌晨2点
  jobTemplate:
    spec:
      template:
        spec:
          containers:
          - name: scale-down
            image: bitnami/kubectl
            command:
            - /bin/bash
            - -c
            - |
              kubectl scale deployment decision-engine --replicas=2
              kubectl scale deployment recommendation-service --replicas=2
          restartPolicy: OnFailure
```

### 预留实例优化
```bash
# 预留实例推荐
aws ce get-reservation-purchase-recommendation \
  --service EC2-Instance \
  --payment-option PARTIAL_UPFRONT

# 成本分析
aws ce get-cost-and-usage \
  --time-period Start=2025-11-01,End=2025-12-01 \
  --granularity MONTHLY \
  --metrics BlendedCost \
  --group-by Type=DIMENSION,Key=SERVICE
```

## 3. 监控成本

### 成本告警配置
```yaml
# Prometheus告警规则
groups:
- name: cost-alerts
  rules:
  - alert: HighCloudCost
    expr: increase(cloud_cost_daily[1d]) > 1000
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "云成本过高"
      description: "过去24小时云成本超过$1000"
```

## 4. 自动化运维成本

### 定期清理任务
```bash
#!/bin/bash
# cost-optimization.sh - 成本优化脚本

set -e

echo "执行成本优化..."

# 1. 清理未使用的镜像
echo "清理未使用的镜像..."
docker system prune -af

# 2. 清理旧日志
echo "清理旧日志..."
find /var/log -name "*.log" -mtime +30 -delete

# 3. 清理临时文件
echo "清理临时文件..."
find /tmp -type f -mtime +7 -delete

# 4. 压缩旧数据
echo "压缩旧数据..."
tar -czf /backup/old-data-$(date +%Y%m).tar.gz /data/old-data/
rm -rf /data/old-data/

# 5. 磁盘使用检查
echo "磁盘使用情况:"
df -h

# 6. 清理Kubernetes事件
echo "清理旧事件..."
kubectl delete events --all-namespaces --field-selector 'lastTimestamp<$(date -d "7 days ago" --iso-8601)'

echo "成本优化完成"
```
```

---

## 📋 实施检查清单

### API文档
- [ ] OpenAPI 3.0规范编写完成
- [ ] 所有API端点文档化
- [ ] 请求/响应示例提供
- [ ] 错误码文档完善
- [ ] SDK生成（Java、Python、JavaScript）
- [ ] Postman集合导出
- [ ] 交互式HTML文档生成
- [ ] 文档更新自动化脚本

### 运维手册
- [ ] 部署指南编写完成
- [ ] 环境要求清单明确
- [ ] 部署脚本测试通过
- [ ] Helm图表优化完善
- [ ] 监控指标体系建立
- [ ] Grafana仪表盘配置
- [ ] Prometheus告警规则
- [ ] 日常维护任务清单
- [ ] 升级流程文档化
- [ ] 备份恢复方案

### 故障处理
- [ ] 常见故障排查脚本
- [ ] 应急响应流程定义
- [ ] 故障级别分类明确
- [ ] 应急联系人列表
- [ ] 常用处理命令整理
- [ ] 故障场景及解决方案
- [ ] 根因分析方法
- [ ] 故障复盘模板
- [ ] 知识库维护机制

### 最佳实践
- [ ] 性能优化指南
- [ ] 代码优化实践
- [ ] 数据库优化建议
- [ ] 缓存策略文档
- [ ] 安全最佳实践
- [ ] 成本优化方案
- [ ] 监控最佳实践
- [ ] 运维自动化指南

### 培训材料
- [ ] 新手入门教程
- [ ] 进阶使用指南
- [ ] 视频教程制作
- [ ] 实操演示文档
- [ ] FAQ整理
- [ ] 常见问题解答
- [ ] 培训课程大纲
- [ ] 考核测试题库

---

**编制：** 浮浮酱 🐱（猫娘工程师）
**日期：** 2025-11-15
**状态：** 📋 指南完成，准备实施

**加油喵～ 文档体系建设即将完成！** ฅ'ω'ฅ
