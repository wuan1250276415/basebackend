# Phase 12.1: Istio 服务网格实施指南

## 📋 概述

本指南介绍如何实施 Istio 服务网格，实现流量管理、安全通信、熔断降级、可观测性等核心功能，构建现代化的微服务治理平台。

---

## 🏗️ Istio 服务网格架构

### 架构图

```
┌─────────────────────────────────────────────────────────────────────┐
│                      Istio 服务网格架构                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐           │
│  │  数据平面     │  │  控制平面     │  │  监控平面     │           │
│  │              │  │              │  │              │           │
│  │  ┌────────┐  │  │  ┌────────┐  │  │  ┌────────┐  │           │
│  │  │Envoy  │  │  │  │Pilot  │  │  │  │Prometheus│  │           │
│  │  │Proxy  │  │  │  │      │  │  │  │        │  │           │
│  │  └────────┘  │  │  └────────┘  │  │  └────────┘  │           │
│  │              │  │              │  │              │           │
│  │  ┌────────┐  │  │  ┌────────┐  │  │  ┌────────┐  │           │
│  │  │Sidecar│  │  │  │Galley │  │  │  │Grafana │  │           │
│  │  │       │  │  │  │      │  │  │  │        │  │           │
│  │  └────────┘  │  │  └────────┘  │  │  └────────┘  │           │
│  │              │  │              │  │              │           │
│  │  ┌────────┐  │  │  ┌────────┐  │  │  ┌────────┐  │           │
│  │  │Proxy  │  │  │  │Citadel│  │  │  │Jaeger  │  │           │
│  │  │ (L7)  │  │  │  │      │  │  │  │        │  │           │
│  │  └────────┘  │  │  └────────┘  │  │  └────────┘  │           │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘           │
│         │                 │                 │                     │
│  ┌──────▼───────────────▼─────────────────▼───────┐             │
│  │            微服务集群                              │             │
│  │  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐          │             │
│  │  │Service│ │Service│ │Service│ │Service│          │             │
│  │  │  A    │ │  B    │ │  C    │ │  D    │          │             │
│  │  └──────┘ └──────┘ └──────┘ └──────┘          │             │
│  │  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐          │             │
│  │  │Service│ │Service│ │Service│ │Service│          │             │
│  │  │  E    │ │  F    │ │  G    │ │  H    │          │             │
│  │  └──────┘ └──────┘ └──────┘ └──────┘          │             │
│  └─────────────────────────────────────────────┘             │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                    核心功能特性                                │ │
│  ├─────────────────────────────────────────────────────────────┤ │
│  │ • 流量管理: 动态路由、负载均衡、故障注入                        │ │
│  │ • 安全通信: mTLS 加密、身份认证、授权策略                      │ │
│  │ • 可观测性: 链路追踪、指标收集、日志聚合                       │ │
│  │ • 熔断降级: 熔断器、重试策略、超时控制                         │ │
│  │ • 灰度发布: 蓝绿部署、金丝雀发布、A/B 测试                      │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### Istio 核心组件

| 组件 | 功能 | 部署位置 |
|------|------|----------|
| **Pilot** | 服务发现、流量管理、配置分发 | 控制平面 |
| **Galley** | 配置验证、分发 | 控制平面 |
| **Citadel** | 证书管理、身份认证 | 控制平面 |
| **Envoy Proxy** | L7 代理、流量拦截 | 数据平面 |
| **Citadel Agent** | 证书分发、证书轮换 | Sidecar |
| **Mixer** | 遥测收集、策略检查 | 控制平面 |
| **Ingress Gateway** | 入口网关 | 边缘节点 |
| **Egress Gateway** | 出口网关 | 边缘节点 |

---

## 🚀 安装部署

### 1. 环境准备

#### 系统要求
- Kubernetes 1.22+
- Helm 3.8+
- 资源需求：
  - Master 节点：4C 8G 100G
  - Worker 节点：8C 16G 200G
  - 至少 3 个节点

#### 安装步骤

```bash
#!/bin/bash
# ===================================================================
# Istio 安装脚本
# ===================================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 下载 Istio
download_istio() {
    log_info "下载 Istio 1.20.1..."

    cd /tmp
    curl -L https://istio.io/downloadIstio | ISTIO_VERSION=1.20.1 sh -

    cd istio-1.20.1
    export PATH=$PWD/bin:$PATH

    log_success "Istio 下载完成"
}

# 安装 Istio Operator
install_operator() {
    log_info "安装 Istio Operator..."

    istioctl operator init --watched-namespaces istio-system --operator-namespace istio-operator

    log_success "Istio Operator 安装完成"
}

# 创建 Istio 配置
create_istio_config() {
    log_info "创建 Istio 配置..."

    cat <<EOF > /tmp/istio-system.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: istio-system
  labels:
    istio-injection: disabled

---
apiVersion: install.istio.io/v1alpha1
kind: IstioOperator
metadata:
  name: istio-control-plane
  namespace: istio-system
spec:
  profile: default

  # 启用 MeshConfig
  meshConfig:
    # 启用自动 mTLS
    enableAutoMtls: true

    # 启用扩展遥测
    extensionProviders:
    - name: prometheus
      prometheus: {}
    - name: jaeger
      envoyOtelAls:
        service: jaeger-collector.istio-system.svc.cluster.local
        port: 14250
    - name: zipkin
      envoyOtelAls:
        service: zipkin.istio-system.svc.cluster.local
        port: 9411

    # 默认追踪采样
    defaultConfig:
      tracing:
        sampling: 100.0
      # 代理配置
      proxyConfig:
        # 代理并发数
        concurrency: 2
        # 代理日志级别
        logLevel: warning

  # 组件配置
  components:
    pilot:
      enabled: true
      k8s:
        resources:
          requests:
            cpu: 500m
            memory: 2048Mi
          limits:
            cpu: 2000m
            memory: 4096Mi
        # 高可用配置
        hpaSpec:
          minReplicas: 2
          maxReplicas: 5
          metrics:
          - type: Resource
            resource:
              name: cpu
              targetAverageUtilization: 80

    ingressGateways:
    - name: istio-ingressgateway
      enabled: true
      k8s:
        resources:
          requests:
            cpu: 100m
            memory: 128Mi
          limits:
            cpu: 1000m
            memory: 1024Mi
        service:
          type: LoadBalancer
          ports:
          - port: 15021
            targetPort: 15021
            name: status-port
          - port: 80
            targetPort: 8080
            name: http2
          - port: 443
            targetPort: 8443
            name: https
          - port: 31400
            targetPort: 31400
            name: tcp

    egressGateways:
    - name: istio-egressgateway
      enabled: true
      k8s:
        resources:
          requests:
            cpu: 100m
            memory: 128Mi
          limits:
            cpu: 1000m
            memory: 1024Mi

  # 核心配置
  values:
    pilot:
      # 启用 Pilot 遥测
      enableTelemetry: true
      # 自动注入
      enableAutoInjection: true

    global:
      # 多集群配置
      multiCluster:
        clusterName: cluster-1
      # 代理配置
      proxy:
        # 代理镜像
        image: proxyv2
        # CPU 限制
        cpuLimit: 1000m
        # 内存限制
        memoryLimit: 1024Mi

    # 启用 Grafana
    grafana:
      enabled: true
      autoscaleEnabled: true

    # 启用 Jaeger
    jaeger:
      enabled: true
      provider: jaeger

    # 启用 Kiali
    kiali:
      enabled: true
EOF

    log_success "Istio 配置创建完成"
}

# 部署 Istio
deploy_istio() {
    log_info "部署 Istio..."

    kubectl apply -f /tmp/istio-system.yaml

    # 等待 Istio 部署完成
    log_info "等待 Istio 部署完成..."
    kubectl wait --for=condition=Ready pods -n istio-system --all --timeout=600s

    log_success "Istio 部署完成"
}

# 启用 Sidecar 自动注入
enable_sidecar_injection() {
    log_info "启用 Sidecar 自动注入..."

    # 为 basebackend 命名空间启用注入
    kubectl label namespace default istio-injection=enabled --overwrite

    log_success "Sidecar 自动注入已启用"
}

# 安装附加组件
install_addons() {
    log_info "安装附加组件..."

    # 安装 Kiali
    kubectl apply -f https://raw.githubusercontent.com/istio/istio/release-1.20/samples/addons/kiali.yaml

    # 安装其他组件 (如果需要)
    # Jaeger 和 Grafana 已经在 IstioOperator 中配置

    log_success "附加组件安装完成"
}

# 验证安装
verify_installation() {
    log_info "验证 Istio 安装..."

    # 检查 Istio 系统组件
    kubectl get pods -n istio-system

    # 检查 Istio 服务
    kubectl get svc -n istio-system

    # 检查 Ingress Gateway
    kubectl get svc istio-ingressgateway -n istio-system

    log_success "Istio 安装验证完成"
}

# 主函数
main() {
    log_info "开始安装 Istio 服务网格..."

    # 1. 检查 Kubernetes 环境
    if ! kubectl cluster-info > /dev/null 2>&1; then
        log_error "Kubernetes 集群未就绪"
        exit 1
    fi

    # 2. 检查 Helm
    if ! command -v helm &> /dev/null; then
        log_error "Helm 未安装"
        exit 1
    fi

    # 3. 执行安装
    download_istio
    install_operator
    create_istio_config
    deploy_istio
    enable_sidecar_injection
    install_addons

    # 4. 验证
    verify_installation

    log_success "Istio 安装完成！"
    echo ""
    echo "访问地址:"
    echo "  - Kiali:    $(kubectl get svc kiali -n istio-system -o jsonpath='{.status.loadBalancer.ingress[0].ip}'):20001/kiali"
    echo "  - Grafana:  $(kubectl get svc grafana -n istio-system -o jsonpath='{.status.loadBalancer.ingress[0].ip}'):3000"
    echo "  - Jaeger:   $(kubectl get svc jaeger-query -n istio-system -o jsonpath='{.status.loadBalancer.ingress[0].ip}'):16686"
}

main "$@"
```

### 2. Helm 安装方式

```yaml
# istio-values.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: istio-system

---
apiVersion: v1
kind: ConfigMap
metadata:
  name: istio-chart
  namespace: istio-system
data:
  values.yaml: |
    global:
      meshID: mesh1
      multiCluster:
        clusterName: cluster1
      network: network1

    pilot:
      autoscaleEnabled: true
      autoscaleMin: 2
      autoscaleMax: 5

    gateways:
      istio-ingressgateway:
        autoscaleEnabled: true
        type: LoadBalancer

    sidecarInjectorWebhook:
      enableNamespacesByDefault: true

    telemetry:
      v2:
        enabled: true

    prometheus:
      enabled: true
```

```bash
# 使用 Helm 安装
helm repo add istio https://istio-release.storage.googleapis.com/charts
helm repo update

kubectl create namespace istio-system
helm install istio-base istio/base -n istio-system
helm install istiod istio/istiod -n istio-system -f istio-values.yaml
kubectl create namespace istio-ingress
helm install istio-ingress istio/gateway -n istio-ingress
```

---

## 🌊 流量管理

### 1. VirtualService 路由规则

```yaml
# user-service.yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: user-service
  namespace: default
spec:
  hosts:
  - user-service
  - user-service.default.svc.cluster.local
  - user-service.example.com
  http:
  # 主要路由规则
  - match:
    - uri:
        prefix: /api/user
    route:
    - destination:
        host: user-service
        port:
          number: 8080
      weight: 80
    # 金丝雀发布
  - match:
    - uri:
        prefix: /api/user
    headers:
      x-canary:
        exact: "true"
    route:
    - destination:
        host: user-service
        subset: v2
        port:
          number: 8080
      weight: 100

---
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: user-service-destination
  namespace: default
spec:
  host: user-service
  trafficPolicy:
    # 连接池配置
    connectionPool:
      tcp:
        maxConnections: 100
        connectTimeout: 30s
        tcpKeepalive:
          time: 7200s
          interval: 75s
      http:
        http1MaxPendingRequests: 50
        http2MaxRequests: 100
        maxRequestsPerConnection: 2
        maxRetries: 3
        consecutiveGatewayErrors: 5
        interval: 30s
        baseEjectionTime: 30s
        maxEjectionPercent: 50

    # 负载均衡策略
    loadBalancer:
      simple: LEAST_CONN
      consistentHash:
        httpHeaderName: "X-User-ID"

    # 熔断器配置
    outlierDetection:
      consecutiveErrors: 3
      interval: 30s
      baseEjectionTime: 30s
      maxEjectionPercent: 50
      minHealthPercent: 50

    # TLS 配置
    tls:
      mode: ISTIO_MUTUAL

  subsets:
  - name: v1
    labels:
      version: v1.0.0
  - name: v2
    labels:
      version: v2.0.0
```

### 2. Gateway 配置

```yaml
# basebackend-gateway.yaml
apiVersion: networking.istio.io/v1beta1
kind: Gateway
metadata:
  name: basebackend-gateway
  namespace: default
spec:
  selector:
    istio: ingressgateway
  servers:
  # HTTP 服务
  - port:
      number: 80
      name: http
      protocol: HTTP
    hosts:
    - api.basebackend.com
    - "*.basebackend.com"
    # HTTP 重定向到 HTTPS
    redirect:
      port: 443
      redirectCode: 301

  # HTTPS 服务
  - port:
      number: 443
      name: https
      protocol: HTTPS
    tls:
      mode: SIMPLE
      credentialName: basebackend-tls
    hosts:
    - api.basebackend.com
    - admin.basebackend.com

  # 管理后台
  - port:
      number: 8080
      name: admin-http
      protocol: HTTP
    hosts:
    - admin.basebackend.com
    # 基础认证
    tls:
      mode: SIMPLE
      credentialName: admin-tls

---
# 虚拟服务
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: basebackend-vs
  namespace: default
spec:
  hosts:
  - api.basebackend.com
  gateways:
  - basebackend-gateway
  http:
  # 用户服务
  - match:
    - uri:
        prefix: /api/user
    - uri:
        prefix: /api/auth
    route:
    - destination:
        host: user-service
        port:
          number: 8080
    # 重试策略
    retries:
      attempts: 3
      perTryTimeout: 2s
      retryOn: gateway-error,connect-failure,refused-stream
    # 超时配置
    timeout: 10s

  # 订单服务
  - match:
    - uri:
        prefix: /api/order
    route:
    - destination:
        host: order-service
        port:
          number: 8080
    # 故障注入测试
    fault:
      delay:
        percentage:
          value: 0.1
        fixedDelay: 5s

  # 管理服务
  - match:
    - uri:
        prefix: /api/admin
    route:
    - destination:
        host: admin-service
        port:
          number: 8080
    # CORS 配置
    corsPolicy:
      allowOrigins:
      - regex: "https://.*\.basebackend\.com"
      allowMethods:
      - GET
      - POST
      - PUT
      - DELETE
      allowHeaders:
      - Authorization
      - Content-Type
      - X-Requested-With
      allowCredentials: true
      maxAge: "86400"
```

### 3. 服务网格路由策略

```yaml
# advanced-routing.yaml

# 1. 基于权重的流量分配
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: product-service
spec:
  hosts:
  - product-service
  http:
  - route:
    - destination:
        host: product-service
        subset: v1
      weight: 70
    - destination:
        host: product-service
        subset: v2
      weight: 30

# 2. 基于 URL 重写
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: rewrite-service
spec:
  hosts:
  - rewrite-service
  http:
  - match:
    - uri:
        prefix: /old-path
    route:
    - destination:
        host: target-service
    # URL 重写
    rewrite:
      uri: /new-path

# 3. 基于 HTTP 头匹配
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: header-routing
spec:
  hosts:
  - header-service
  http:
  # 移动端流量
  - match:
    - headers:
        user-agent:
          regex: ".*Mobile.*"
    route:
    - destination:
        host: mobile-service
  # PC 端流量
  - match:
    - headers:
        user-agent:
          regex: ".*(?!Mobile).*"
    route:
    - destination:
        host: pc-service

# 4. 基于源地址访问控制
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: ip-based-routing
spec:
  hosts:
  - restricted-service
  http:
  # 允许的 IP
  - match:
    - sourceIp:
        - 10.0.0.0/8
        - 192.168.1.0/24
    route:
    - destination:
        host: restricted-service
  # 其他 IP 重定向
  - route:
    - destination:
        host: forbidden-service

# 5. 超时和重试
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: timeout-retry
spec:
  hosts:
  - slow-service
  http:
  - route:
    - destination:
        host: slow-service
    # 超时配置
    timeout: 5s
    # 重试策略
    retries:
      attempts: 3
      perTryTimeout: 2s
      retryOn: gateway-error,5xx,connect-failure,refused-stream
      # 幂等性要求
      retryPolicy:
        retryOn: 5xx,connect-failure,refused-stream
        numRetries: 3
```

### 4. Egress Gateway 配置

```yaml
# egress-gateway.yaml
apiVersion: networking.istio.io/v1beta1
kind: Gateway
metadata:
  name: istio-egressgateway
spec:
  selector:
    istio: egressgateway
  servers:
  - port:
      number: 443
      name: tls
      protocol: TLS
    hosts:
    - "*.external-service.com"
    tls:
      mode: PASSTHROUGH

---
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: external-services
spec:
  hosts:
  - "*.external-service.com"
  gateways:
  - istio-egressgateway
  tls:
  - match:
    - sniHosts:
      - external-api.com
    route:
    - destination:
        host: external-api.com
  - match:
    - sniHosts:
      - external-db.com
    route:
    - destination:
        host: external-db.com
```

---

## ⚡ 熔断与降级

### 1. DestinationRule 熔断配置

```yaml
# circuit-breaker.yaml
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: user-service-circuit-breaker
spec:
  host: user-service
  trafficPolicy:
    # 连接池配置
    connectionPool:
      tcp:
        maxConnections: 100           # 最大连接数
        connectTimeout: 30s           # 连接超时
        tcpKeepalive:
          time: 7200s                 # TCP 保活时间
          interval: 75s               # 保活间隔
      http:
        http1MaxPendingRequests: 50   # 最大等待请求数
        http2MaxRequests: 100         # 最大并发请求数
        maxRequestsPerConnection: 2   # 每连接最大请求数
        maxRetries: 3                 # 最大重试次数
        consecutiveGatewayErrors: 5   # 连续网关错误数
        consecutiveServerErrors: 5    # 连续服务器错误数
        interval: 30s                 # 间隔时间
        baseEjectionTime: 30s         # 基础隔离时间
        maxEjectionPercent: 50        # 最大隔离百分比
        minHealthPercent: 50          # 最小健康百分比

    # 负载均衡
    loadBalancer:
      simple: LEAST_CONN

    # 熔断器配置
    outlierDetection:
      # 连续错误次数触发熔断
      consecutiveErrors: 3
      # 错误检测间隔
      interval: 30s
      # 最小隔离时间
      baseEjectionTime: 30s
      # 最大隔离百分比
      maxEjectionPercent: 50
      # 最小健康实例百分比
      minHealthPercent: 50

    # 重试策略
    retryPolicy:
      retryOn: 5xx,connect-failure,refused-stream
      numRetries: 3
      perTryTimeout: 2s
      retryRemoteLocalities: true

  subsets:
  - name: v1
    labels:
      version: v1
  - name: v2
    labels:
      version: v2
```

### 2. HTTP 故障注入

```yaml
# fault-injection.yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: fault-injection
spec:
  hosts:
  - user-service
  http:
  # 延迟故障注入
  - match:
    - headers:
        x-fault-injection:
          exact: "delay"
    fault:
      delay:
        percentage:
          value: 10.0               # 10% 请求延迟
        fixedDelay: 5s              # 延迟 5 秒
    route:
    - destination:
        host: user-service

  # 错误故障注入
  - match:
    - headers:
        x-fault-injection:
          exact: "abort"
    fault:
      abort:
        percentage:
          value: 10.0               # 10% 请求中止
        httpStatus: 500             # 返回 500 错误
    route:
    - destination:
        host: user-service

  # 百分比故障注入
  - fault:
      delay:
        percentage:
          value: 0.1                # 0.1% 请求延迟
        fixedDelay: 3s
      abort:
        percentage:
          value: 0.1                # 0.1% 请求中止
        httpStatus: 503
    route:
    - destination:
        host: user-service
```

### 3. HTTP 超时配置

```yaml
# timeout-config.yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: timeout-config
spec:
  hosts:
  - order-service
  http:
  # 全局超时
  - timeout: 10s
    route:
    - destination:
        host: order-service

  # 特定路径超时
  - match:
    - uri:
        prefix: /api/order/create
    timeout: 30s                    # 创建订单超时 30 秒
    route:
    - destination:
        host: order-service

  - match:
    - uri:
        prefix: /api/order/query
    timeout: 5s                     # 查询订单超时 5 秒
    route:
    - destination:
        host: order-service

  # 超时与重试结合
  - route:
    - destination:
        host: external-service
    timeout: 10s
    retries:
      attempts: 3
      perTryTimeout: 2s
      retryOn: gateway-error,5xx,connect-failure
```

### 4. 断路器监控

```yaml
# circuit-breaker-monitor.yaml
apiVersion: v1
kind: ServiceMonitor
metadata:
  name: istio-proxy
  namespace: istio-system
  labels:
    app: istio-proxy
spec:
  selector:
    matchLabels:
      app: istio-proxy
  endpoints:
  - port: http-envoy-prom
    interval: 15s
    path: /stats/prometheus

---
# Prometheus 告警规则
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: istio-alerts
  namespace: istio-system
spec:
  groups:
  - name: istio.rules
    interval: 15s
    rules:
    # 熔断器触发告警
    - alert: IstioCircuitBreakerTriggered
      expr: sum(irate(istio_requests_total{reporter="source",response_code!~"5.*"}[1m])) by (destination_service_name, destination_service_namespace) - sum(irate(istio_requests_total{reporter="source",response_code=~"5.*"}[1m])) by (destination_service_name, destination_service_namespace)
      for: 0s
      labels:
        severity: warning
      annotations:
        summary: "Circuit breaker triggered for {{ $labels.destination_service_name }}"

    # 高错误率告警
    - alert: IstioHighErrorRate
      expr: sum(irate(istio_requests_total{reporter="source",response_code=~"5.*"}[5m])) / sum(irate(istio_requests_total{reporter="source"}[5m])) > 0.1
      for: 5m
      labels:
        severity: critical
      annotations:
        summary: "High error rate detected for {{ $labels.destination_service_name }}"

    # 超时告警
    - alert: IstioRequestTimeout
      expr: sum(irate(istio_requests_total{destination_service_name="user-service"}[5m])) by (response_code) > 0
      for: 0s
      labels:
        severity: warning
      annotations:
        summary: "Request timeout detected for {{ $labels.destination_service_name }}"
```

---

## 🔍 可观测性

### 1. 指标收集

```yaml
# telemetry.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: istio-proxy-extra
  namespace: istio-system
data:
  # 启用详细指标
  mesh: |
    defaultConfig:
      # 指标配置
      metricReportingDuration: 15s
      metricReportingEnabled: true
      # 追踪配置
      tracing:
        sampling: 100.0
        max_path_tag_length: 256
      # 自定义指标
      proxyStatsMatcher:
        inclusionRegexps:
        - "istio_.*"
        - "cluster_.*"
        - "listener_.*"
        - "http.*"
        - "tcp.*"
        exclusionRegexps:
        - "istio_.*_log_.*"
        - "istio_.*_config_.*"

    # 扩展遥测
    extensionProviders:
    - name: prometheus
      prometheus: {}
    - name: jaeger
      envoyOtelAls:
        service: jaeger-collector.istio-system.svc.cluster.local
        port: 14250
    - name: zipkin
      envoyOtelAls:
        service: zipkin.istio-system.svc.cluster.local
        port: 9411
    - name: opencensus
      envoyOtelAls:
        service: lightstep-collector.istio-system.svc.cluster.local
        port: 55678
```

### 2. Grafana 仪表盘

```json
{
  "dashboard": {
    "title": "Istio Service Mesh Dashboard",
    "panels": [
      {
        "title": "Request Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "round(sum(rate(istio_requests_total{destination_service_namespace=~\"$namespace\",destination_service_name=~\"$service\"}[5m])) by (destination_service_name, destination_service_namespace), 0.001)",
            "legendFormat": "{{ destination_service_name }} - rate"
          }
        ]
      },
      {
        "title": "Success Rate",
        "type": "singlestat",
        "targets": [
          {
            "expr": "sum(rate(istio_requests_total{destination_service_namespace=~\"$namespace\",destination_service_name=~\"$service\",response_code!~\"5.*\"}[5m])) / sum(rate(istio_requests_total{destination_service_namespace=~\"$namespace\",destination_service_name=~\"$service\"}[5m])) * 100",
            "legendFormat": "Success Rate"
          }
        ],
        "valueName": "current",
        "thresholds": "95,99,99.9",
        "colorBackground": true
      },
      {
        "title": "Request Duration",
        "type": "graph",
        "targets": [
          {
            "expr": "histogram_quantile(50, sum(rate(istio_request_duration_milliseconds_bucket{destination_service_namespace=~\"$namespace\",destination_service_name=~\"$service\"}[5m])) by (destination_service_name, destination_service_namespace, le))",
            "legendFormat": "p50 - {{ destination_service_name }}"
          },
          {
            "expr": "histogram_quantile(90, sum(rate(istio_request_duration_milliseconds_bucket{destination_service_namespace=~\"$namespace\",destination_service_name=~\"$service\"}[5m])) by (destination_service_name, destination_service_namespace, le))",
            "legendFormat": "p90 - {{ destination_service_name }}"
          },
          {
            "expr": "histogram_quantile(99, sum(rate(istio_request_duration_milliseconds_bucket{destination_service_namespace=~\"$namespace\",destination_service_name=~\"$service\"}[5m])) by (destination_service_name, destination_service_namespace, le))",
            "legendFormat": "p99 - {{ destination_service_name }}"
          }
        ]
      },
      {
        "title": "Error Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "sum(rate(istio_requests_total{destination_service_namespace=~\"$namespace\",destination_service_name=~\"$service\",response_code=~\"5.*\"}[5m])) by (response_code) / sum(rate(istio_requests_total{destination_service_namespace=~\"$namespace\",destination_service_name=~\"$service\"}[5m])) * 100",
            "legendFormat": "{{ response_code }}"
          }
        ]
      }
    ]
  }
}
```

### 3. Jaeger 链路追踪

```yaml
# jaeger-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: jaeger
  namespace: istio-system
spec:
  replicas: 1
  selector:
    matchLabels:
      app: jaeger
  template:
    metadata:
      labels:
        app: jaeger
    spec:
      containers:
      - name: jaeger
        image: jaegertracing/all-in-one:latest
        ports:
        - containerPort: 16686  # Jaeger UI
        - containerPort: 14268  # HTTP collector
        - containerPort: 14250  # gRPC collector
        env:
        - name: COLLECTOR_OTLP_ENABLED
          value: "true"

---
apiVersion: v1
kind: Service
metadata:
  name: jaeger-query
  namespace: istio-system
spec:
  type: LoadBalancer
  selector:
    app: jaeger
  ports:
  - name: http
    port: 16686
    targetPort: 16686
```

### 4. Kiali 服务拓扑

```yaml
# kiali-config.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: kiali-config
  namespace: istio-system
  labels:
    app: kiali
data:
  config.yaml: |
    server:
      port: 20001
      web_root: /kiali
    external_services:
      prometheus:
        url: http://prometheus:9090
      grafana:
        url: http://grafana:3000
      jaeger:
        url: http://jaeger-query:16686
    istio_namespace: istio-system
    deployment:
      accessible_namespaces:
      - "**"
    auth:
      strategy: anonymous
```

### 5. 日志收集

```yaml
# fluentd-config.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: fluentd-istio
  namespace: istio-system
data:
  fluent.conf: |
    <source>
      @type tail
      path /var/log/istio/envoy/access.log
      pos_file /var/log/fluentd-istio-envoy-access.log.pos
      tag istio.access
      format json
      time_key timestamp
      time_format %Y-%m-%dT%H:%M:%S.%NZ
    </source>

    <filter istio.access>
      @type record_transformer
      <record>
        source_service "#{record['downstream_peer'].nil? ? 'unknown' : record['downstream_peer']}"
        source_version "#{record['request_id'].nil? ? 'unknown' : record['request_id']}"
      </record>
    </filter>

    <match istio.access>
      @type elasticsearch
      host elasticsearch.istio-system.svc.cluster.local
      port 9200
      index_name istio-access
      type_name istio-access
      include_timestamp true
      flush_interval 10s
    </match>
```

---

## 🔐 安全策略

### 1. mTLS 双向认证

```yaml
# mtls.yaml
# 全局 mTLS 配置
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: default
  namespace: istio-system
spec:
  mtls:
    mode: STRICT  # 要求 mTLS

---
# 命名空间级别 mTLS
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: namespace-mtls
  namespace: default
spec:
  mtls:
    mode: PERMISSIVE  # 允许明文和 mTLS

---
# 服务级别 mTLS
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: service-mtls
  namespace: default
spec:
  selector:
    matchLabels:
      app: user-service
  mtls:
    mode: STRICT

---
# 端口级别 mTLS
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: port-mtls
  namespace: default
spec:
  selector:
    matchLabels:
      app: order-service
  mtls:
    mode: STRICT
    portLevelMtls:
      8080:
        mode: STRICT
      9090:
        mode: PERMISSIVE
```

### 2. 授权策略

```yaml
# authorization.yaml

# 基于角色的访问控制
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: user-service-rbac
  namespace: default
spec:
  selector:
    matchLabels:
      app: user-service
  rules:
  # 允许管理后台访问
  - from:
    - source:
        principals: ["cluster.local/ns/istio-system/sa/admin-service"]
    to:
    - operation:
        methods: ["GET", "POST", "PUT", "DELETE"]
  # 允许内网服务访问
  - from:
    - source:
        namespaces: ["default"]
    to:
    - operation:
        methods: ["GET", "POST"]
  # 拒绝未授权访问
  - to:
    - operation:
        methods: ["DELETE"]
  # 条件拒绝
  when:
  - key: source.ip
    notValues: ["10.0.0.0/8"]

---
# 基于 JWT 的访问控制
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: jwt-auth
  namespace: default
spec:
  selector:
    matchLabels:
      app: order-service
  rules:
  - from:
    - source:
        requestPrincipals: ["https://auth.basebackend.com/*"]
    to:
    - operation:
        methods: ["POST"]
        paths: ["/api/order/create"]
  - from:
    - source:
        requestPrincipals: ["https://auth.basebackend.com/*"]
    to:
    - operation:
        methods: ["GET"]
    when:
    - key: request.auth.claims[role]
      values: ["user", "admin"]

---
# 基于属性的访问控制 (ABAC)
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: abac-policy
  namespace: default
spec:
  selector:
    matchLabels:
      app: product-service
  rules:
  - from:
    - source:
        remoteIpBlocks: ["10.0.0.0/8"]
    to:
    - operation:
        methods: ["GET"]
        paths: ["/api/product/list"]
  - from:
    - source:
        remoteIpBlocks: ["192.168.0.0/16"]
    to:
    - operation:
        methods: ["GET", "POST"]
        paths: ["/api/product/*"]
    when:
    - key: request.headers[user-agent]
      values: ["internal-service"]
```

### 3. 安全策略示例

```yaml
# security-policies.yaml

# 1. 命名空间隔离
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: namespace-isolation
  namespace: default
spec:
  rules:
  # 仅允许同命名空间访问
  - from:
    - source:
        namespaces: ["default"]
  # 拒绝跨命名空间访问
  - to:
    - operation:
        methods: ["GET", "POST"]

---
# 2. 速率限制
apiVersion: networking.istio.io/v1beta1
kind: EnvoyFilter
metadata:
  name: rate-limit-filter
  namespace: default
spec:
  configPatches:
  - applyTo: HTTP_FILTER
    match:
      context: SIDECAR_INBOUND
      listener:
        filterChain:
          filter:
            name: "envoy.filters.network.http_connection_manager"
    patch:
      operation: INSERT_BEFORE
      value:
        name: envoy.filters.http.local_ratelimit
        typed_config:
          "@type": type.googleapis.com/udpa.type.v1.TypedStruct
          value:
            "@type": type.googleapis.com/udpa.type.v1.TypedStruct
            value:
              "@type": type.googleapis.com/envoy.extensions.filters.http.local_ratelimit.v3.LocalRateLimit
              stat_prefix: rate_limiter
              token_bucket:
                max_tokens: 1000
                tokens_per_fill: 100
                fill_interval: 60s

---
# 3. 请求头安全
apiVersion: networking.istio.io/v1beta1
kind: EnvoyFilter
metadata:
  name: security-headers
  namespace: default
spec:
  configPatches:
  - applyTo: HTTP_FILTER
    match:
      context: SIDECAR_INBOUND
      listener:
        filterChain:
          filter:
            name: "envoy.filters.network.http_connection_manager"
    patch:
      operation: INSERT_BEFORE
      value:
        name: envoy.filters.http.lua
        typed_config:
          "@type": type.googleapis.com/envoy.extensions.filters.http.lua.v3.Lua
          inline_code: |
            function envoy_on_response(response_handle)
              response_handle:headers():add("X-Frame-Options", "DENY")
              response_handle:headers():add("X-Content-Type-Options", "nosniff")
              response_handle:headers():add("X-XSS-Protection", "1; mode=block")
              response_handle:headers():add("Strict-Transport-Security", "max-age=31536000; includeSubDomains")
              response_handle:headers():add("Content-Security-Policy", "default-src 'self'")
            end
```

---

## 🚦 灰度发布

### 1. 蓝绿部署

```yaml
# blue-green-deployment.yaml

# 1. 创建蓝版本
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-service-blue
  namespace: default
  labels:
    app: user-service
    version: blue
spec:
  replicas: 3
  selector:
    matchLabels:
      app: user-service
      version: blue
  template:
    metadata:
      labels:
        app: user-service
        version: blue
      annotations:
        sidecar.istio.io/inject: "true"
    spec:
      containers:
      - name: user-service
        image: basebackend/user-service:v1.0.0
        ports:
        - containerPort: 8080
        env:
        - name: VERSION
          value: "blue"

---
# 2. 创建绿版本
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-service-green
  namespace: default
  labels:
    app: user-service
    version: green
spec:
  replicas: 3
  selector:
    matchLabels:
      app: user-service
      version: green
  template:
    metadata:
      labels:
        app: user-service
        version: green
      annotations:
        sidecar.istio.io/inject: "true"
    spec:
      containers:
      - name: user-service
        image: basebackend/user-service:v2.0.0
        ports:
        - containerPort: 8080
        env:
        - name: VERSION
          value: "green"

---
# 3. 蓝绿路由策略
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: user-service-blue-green
spec:
  hosts:
  - user-service
  http:
  # 蓝版本流量
  - match:
    - headers:
        x-version:
          exact: "blue"
    route:
    - destination:
        host: user-service
        subset: blue
  # 绿版本流量
  - match:
    - headers:
        x-version:
          exact: "green"
    route:
    - destination:
        host: user-service
        subset: green
  # 默认路由到蓝版本
  - route:
    - destination:
        host: user-service
        subset: blue

---
# 4. DestinationRule
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: user-service-destination
spec:
  host: user-service
  subsets:
  - name: blue
    labels:
      version: blue
  - name: green
    labels:
      version: green
```

### 2. 金丝雀发布

```yaml
# canary-deployment.yaml

# 金丝雀发布策略
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: user-service-canary
spec:
  hosts:
  - user-service
  http:
  # 金丝雀路由
  - match:
    - headers:
        x-canary-user:
          exact: "true"
    route:
    - destination:
        host: user-service
        subset: v2
      weight: 100
  # 百分比流量分配
  - route:
    - destination:
        host: user-service
        subset: v1
      weight: 95
    - destination:
        host: user-service
        subset: v2
      weight: 5

---
# 监控金丝雀发布
apiVersion: argoproj.io/v1alpha1
kind: Rollout
metadata:
  name: user-service-rollout
spec:
  replicas: 10
  strategy:
    canary:
      steps:
      - setWeight: 10
      - pause: {duration: 30s}
      - setWeight: 50
      - pause: {duration: 60s}
      - setWeight: 100
      analysis:
        templates:
        - templateName: success-rate
        args:
        - name: service-name
          value: user-service
  selector:
    matchLabels:
      app: user-service
  template:
    metadata:
      labels:
        app: user-service
    spec:
      containers:
      - name: user-service
        image: basebackend/user-service:latest

---
# 分析模板
apiVersion: argoproj.io/v1alpha1
kind: AnalysisTemplate
metadata:
  name: success-rate
spec:
  args:
  - name: service-name
  metrics:
  - name: success-rate
    successCondition: result[0] >= 0.95
    interval: 60s
    count: 3
    provider:
      prometheus:
        address: http://prometheus:9090
        query: |
          sum(rate(istio_requests_total{destination_service_name="{{args.service-name}}",response_code!~"5.*"}[5m])) /
          sum(rate(istio_requests_total{destination_service_name="{{args.service-name}}"}[5m]))
```

### 3. A/B 测试

```yaml
# ab-testing.yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: user-service-ab
spec:
  hosts:
  - user-service
  http:
  # 用户组 A - 基于 Cookie
  - match:
    - headers:
        cookie:
          regex: ".*ab_test=A.*"
    route:
    - destination:
        host: user-service
        subset: version-a
  # 用户组 B - 基于 Cookie
  - match:
    - headers:
        cookie:
          regex: ".*ab_test=B.*"
    route:
    - destination:
        host: user-service
        subset: version-b
  # 默认路由
  - route:
    - destination:
        host: user-service
        subset: version-a
      weight: 50
    - destination:
        host: user-service
        subset: version-b
      weight: 50

---
# 版本追踪
apiVersion: telemetry.istio.io/v1alpha1
kind: Telemetry
metadata:
  name: version-tracker
spec:
  metrics:
  - providers:
    - name: prometheus
    overrides:
    - match:
        metric: ALL_METRICS
      tagOverrides:
        version:
          value: "{{.wasm | default \"unknown\"}}"
```

---

## 🧪 测试与验证

### 1. Istio 安装验证

```bash
#!/bin/bash
# istio-verification.sh

log_info() {
    echo -e "\033[0;34m[INFO]\033[0m $1"
}

log_success() {
    echo -e "\033[0;32m[SUCCESS]\033[0m $1"
}

log_error() {
    echo -e "\033[0;31m[ERROR]\033[0m $1"
}

# 检查 Istio 系统组件
check_istio_system() {
    log_info "检查 Istio 系统组件..."

    pods=$(kubectl get pods -n istio-system)
    if kubectl get pods -n istio-system | grep -q "Running"; then
        log_success "Istio 系统组件运行正常"
        echo "$pods"
    else
        log_error "Istio 系统组件异常"
        return 1
    fi
}

# 检查 Istio 服务
check_istio_services() {
    log_info "检查 Istio 服务..."

    services=$(kubectl get svc -n istio-system)
    echo "$services"
}

# 检查入口网关
check_ingress_gateway() {
    log_info "检查入口网关..."

    gateway=$(kubectl get svc istio-ingressgateway -n istio-system)
    echo "$gateway"
}

# 测试默认路由
test_default_routing() {
    log_info "测试默认路由..."

    # 创建测试服务
    kubectl apply -f - <<EOF
apiVersion: v1
kind: Service
metadata:
  name: test-service
spec:
  selector:
    app: test
  ports:
  - port: 80
    targetPort: 80
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: test-deployment
spec:
  replicas: 1
  selector:
    matchLabels:
      app: test
  template:
    metadata:
      labels:
        app: test
      annotations:
        sidecar.istio.io/inject: "true"
    spec:
      containers:
      - name: test
        image: nginx
        ports:
        - containerPort: 80
EOF

    # 等待部署完成
    kubectl wait --for=condition=ready pods -l app=test --timeout=60s

    # 创建路由规则
    kubectl apply -f - <<EOF
apiVersion: networking.istio.io/v1beta1
kind: Gateway
metadata:
  name: test-gateway
spec:
  selector:
    istio: ingressgateway
  servers:
  - port:
      number: 80
      name: http
      protocol: HTTP
    hosts:
    - test.example.com
---
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: test-vs
spec:
  hosts:
  - test.example.com
  gateways:
  - test-gateway
  http:
  - route:
    - destination:
        host: test-service
EOF

    log_success "测试路由规则创建完成"
}

# 检查 mTLS
check_mtls() {
    log_info "检查 mTLS 配置..."

    mtls_policy=$(kubectl get peerauthentication default -n istio-system 2>/dev/null)
    if [ -n "$mtls_policy" ]; then
        log_success "mTLS 策略已配置"
        echo "$mtls_policy"
    else
        log_warn "未发现 mTLS 策略"
    fi
}

# 检查遥测
check_telemetry() {
    log_info "检查遥测配置..."

    telemetry=$(kubectl get telemetry default -n default 2>/dev/null)
    if [ -n "$telemetry" ]; then
        log_success "遥测配置已启用"
        echo "$telemetry"
    else
        log_warn "遥测配置未启用"
    fi
}

# 主函数
main() {
    echo "========================================"
    echo "      Istio 安装验证脚本"
    echo "========================================"
    echo ""

    check_istio_system
    echo ""

    check_istio_services
    echo ""

    check_ingress_gateway
    echo ""

    check_mtls
    echo ""

    check_telemetry
    echo ""

    log_success "Istio 验证完成！"
}

main "$@"
```

### 2. 流量管理测试

```bash
#!/bin/bash
# traffic-management-test.sh

# 安装测试应用
kubectl apply -f samples/bookinfo/platform/kube/bookinfo.yaml

# 等待部署完成
kubectl wait --for=condition=ready pods -l app=reviews --timeout=120s

# 测试默认路由
log_info "测试默认路由..."
curl -s http://$GATEWAY_URL/productpage | grep "Titanium"

# 测试金丝雀发布
log_info "测试金丝雀发布..."

# 应用金丝雀规则
cat <<EOF | kubectl apply -f -
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: reviews
spec:
  hosts:
  - reviews
  http:
  - route:
    - destination:
        host: reviews
        subset: v1
      weight: 50
    - destination:
        host: reviews
        subset: v2
      weight: 50
EOF

# 测试流量分配
log_info "测试流量分配..."
for i in {1..10}; do
    curl -s -o /dev/null -w "%{http_code}\n" http://$GATEWAY_URL/productpage
    sleep 1
done

# 测试熔断器
log_info "测试熔断器..."
cat <<EOF | kubectl apply -f -
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: reviews
spec:
  host: reviews
  trafficPolicy:
    connectionPool:
      tcp:
        maxConnections: 1
      http:
        http1MaxPendingRequests: 1
        maxRequestsPerConnection: 1
EOF

# 测试故障注入
log_info "测试故障注入..."
cat <<EOF | kubectl apply -f -
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: ratings
spec:
  hosts:
  - ratings
  http:
  - fault:
      abort:
        percentage:
          value: 100.0
        httpStatus: 500
    route:
    - destination:
        host: ratings
EOF

log_success "流量管理测试完成"
```

---

## 📊 监控告警

### 1. Prometheus 配置

```yaml
# prometheus-config.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: prometheus-config
  namespace: istio-system
data:
  prometheus.yml: |
    global:
      scrape_interval: 15s
      evaluation_interval: 15s

    scrape_configs:
    # Istio Proxy 指标
    - job_name: 'istio-proxy'
      scrape_interval: 5s
      kubernetes_sd_configs:
      - role: endpoints
        namespaces:
          names:
          - default
      relabel_configs:
      - source_labels: [__meta_kubernetes_endpoints_name]
        regex: '.*'  # 匹配所有服务
        action: keep
      - source_labels: [__address__]
        regex: '([^:]+)(?::\d+)?'
        target_label: '__address__'
        replacement: '${1}:15090'
      - regex: '__meta_kubernetes_(.+)'
        target_label: '__tmp_istio_proxy'
        action: labeldrop

    # Istio Citadel 指标
    - job_name: 'istio-citadel'
      kubernetes_sd_configs:
      - role: endpoints
        namespaces:
          names:
          - istio-system
      relabel_configs:
      - source_labels: [__meta_kubernetes_service_name, __meta_kubernetes_endpoint_port_name]
        action: keep
        regex: istio-citadel;http-monitoring

    # Istio Pilot 指标
    - job_name: 'istio-pilot'
      kubernetes_sd_configs:
      - role: endpoints
        namespaces:
          names:
          - istio-system
      relabel_configs:
      - source_labels: [__meta_kubernetes_service_name, __meta_kubernetes_endpoint_port_name]
        action: keep
        regex: istio-pilot;http-monitoring

    # Istio Galley 指标
    - job_name: 'istio-galley'
      kubernetes_sd_configs:
      - role: endpoints
        namespaces:
          names:
          - istio-system
      relabel_configs:
      - source_labels: [__meta_kubernetes_service_name, __meta_kubernetes_endpoint_port_name]
        action: keep
        regex: istio-galley;http-monitoring

  # 告警规则
  alerting.yml: |
    groups:
    - name: istio.rules
      rules:
      - alert: IstioProxyHighMemory
        expr: sum(container_memory_working_set_bytes{container="istio-proxy"}) by (pod) > 1024Mi
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "High memory usage detected"

      - alert: IstioProxyHighCPU
        expr: sum(rate(container_cpu_usage_seconds_total{container="istio-proxy"}[5m])) by (pod) > 0.05
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "High CPU usage detected"

      - alert: IstioCircuitBreakerOpen
        expr: sum(rate(istio_requests_total{response_code!~"5.*"}[1m])) by (destination_service_name) - sum(rate(istio_requests_total{response_code=~"5.*"}[1m])) by (destination_service_name)
        for: 0s
        labels:
          severity: warning
        annotations:
          summary: "Circuit breaker triggered"

      - alert: IstioHighErrorRate
        expr: sum(rate(istio_requests_total{response_code=~"5.*"}[5m])) / sum(rate(istio_requests_total[5m])) > 0.1
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High error rate detected"

      - alert: IstioRequestLatencyHigh
        expr: histogram_quantile(0.99, sum(rate(istio_request_duration_milliseconds_bucket[5m])) by (le)) > 1000
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Request latency is high"
```

### 2. Grafana 仪表盘

```json
{
  "dashboard": {
    "title": "Istio Service Mesh Overview",
    "panels": [
      {
        "title": "Overall Success Rate",
        "type": "stat",
        "targets": [
          {
            "expr": "sum(irate(istio_requests_total{response_code!~\"5.*\"}[5m])) / sum(irate(istio_requests_total[5m])) * 100",
            "legendFormat": "Success Rate"
          }
        ]
      },
      {
        "title": "Request Rate by Service",
        "type": "graph",
        "targets": [
          {
            "expr": "sum(rate(istio_requests_total[5m])) by (destination_service_name)",
            "legendFormat": "{{ destination_service_name }}"
          }
        ]
      },
      {
        "title": "Request Duration (P50/P90/P99)",
        "type": "graph",
        "targets": [
          {
            "expr": "histogram_quantile(50, sum(rate(istio_request_duration_milliseconds_bucket[5m])) by (le))",
            "legendFormat": "P50"
          },
          {
            "expr": "histogram_quantile(90, sum(rate(istio_request_duration_milliseconds_bucket[5m])) by (le))",
            "legendFormat": "P90"
          },
          {
            "expr": "histogram_quantile(99, sum(rate(istio_request_duration_milliseconds_bucket[5m])) by (le))",
            "legendFormat": "P99"
          }
        ]
      },
      {
        "title": "Traffic Distribution",
        "type": "piechart",
        "targets": [
          {
            "expr": "sum(rate(istio_requests_total[5m])) by (destination_service_name)",
            "legendFormat": "{{ destination_service_name }}"
          }
        ]
      },
      {
        "title": "Error Rate by Service",
        "type": "graph",
        "targets": [
          {
            "expr": "sum(rate(istio_requests_total{response_code=~\"5.*\"}[5m])) by (destination_service_name) / sum(rate(istio_requests_total[5m])) by (destination_service_name) * 100",
            "legendFormat": "{{ destination_service_name }}"
          }
        ]
      }
    ]
  }
}
```

---

## 📚 参考资料

1. [Istio 官方文档](https://istio.io/latest/docs/)
2. [Istio 流量管理](https://istio.io/latest/docs/concepts/traffic-management/)
3. [Istio 安全](https://istio.io/latest/docs/concepts/security/)
4. [Istio 可观测性](https://istio.io/latest/docs/concepts/observability/)
5. [Kubernetes 服务网格](https://kubernetes.io/zh/docs/concepts/cluster-administration/manage-deployment/)

---

## 📋 Istio 实施检查清单

### 安装部署
- [ ] Kubernetes 集群准备就绪
- [ ] Istio Operator 安装
- [ ] Istio 核心组件部署
- [ ] Sidecar 自动注入启用
- [ ] 附加组件安装 (Kiali, Grafana, Jaeger)

### 流量管理
- [ ] Gateway 配置
- [ ] VirtualService 路由规则
- [ ] DestinationRule 负载均衡
- [ ] 熔断器配置
- [ ] 故障注入测试
- [ ] 超时和重试配置

### 安全配置
- [ ] mTLS 双向认证
- [ ] PeerAuthentication 策略
- [ ] AuthorizationPolicy 授权
- [ ] 基于角色的访问控制
- [ ] 基于 JWT 的认证
- [ ] 零信任网络策略

### 可观测性
- [ ] Prometheus 指标收集
- [ ] Grafana 仪表盘
- [ ] Jaeger 链路追踪
- [ ] Kiali 服务拓扑
- [ ] 日志聚合
- [ ] 告警规则配置

### 高级特性
- [ ] 金丝雀发布
- [ ] 蓝绿部署
- [ ] A/B 测试
- [ ] 流量镜像
- [ ] 多集群部署
- [ ] 边缘网关配置

---

**编制：** 浮浮酱 🐱（猫娘工程师）
**日期：** 2025-11-14
**状态：** 📋 指南完成，准备实施

**加油喵～ Istio 服务网格即将部署完成！** ฅ'ω'ฅ
