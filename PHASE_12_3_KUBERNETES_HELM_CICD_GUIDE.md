# Phase 12.3: 容器化与编排实施指南

## 📋 概述

本指南介绍如何实施 Kubernetes 容器化编排，包括集群搭建、Helm Chart 打包、CI/CD 流水线等核心能力，构建现代化的容器化部署平台。

---

## 🏗️ Kubernetes 集群架构

### 架构图

```
┌─────────────────────────────────────────────────────────────────────┐
│                      Kubernetes 集群架构                             │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐           │
│  │  Master 节点  │  │ Worker 节点  │  │ Worker 节点  │           │
│  │              │  │              │  │              │           │
│  │  ┌────────┐  │  │  ┌────────┐  │  │  ┌────────┐  │           │
│  │  │ API    │  │  │  │ Kubelet│  │  │  │ Kubelet│  │           │
│  │  │ Server │  │  │  │        │  │  │  │        │  │           │
│  │  └────────┘  │  │  └────────┘  │  │  └────────┘  │           │
│  │              │  │              │  │              │           │
│  │  ┌────────┐  │  │  ┌────────┐  │  │  ┌────────┐  │           │
│  │  │ Scheduler│  │  │  Containerd│  │  │ Containerd│  │           │
│  │  │         │  │  │  │         │  │  │  │         │  │           │
│  │  └────────┘  │  │  └────────┘  │  │  └────────┘  │           │
│  │              │  │              │  │              │           │
│  │  ┌────────┐  │  │  ┌────────┐  │  │  ┌────────┐  │           │
│  │  │ etcd   │  │  │  │Pod    │  │  │  │Pod    │  │           │
│  │  │        │  │  │  │       │  │  │  │       │  │           │
│  │  └────────┘  │  │  └────────┘  │  │  └────────┘  │           │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘           │
│         │                 │                 │                     │
│  ┌──────▼───────────────▼─────────────────▼───────┐             │
│  │              存储层                              │             │
│  │  ┌────────┐ ┌────────┐ ┌────────┐            │             │
│  │  │ NFS    │ │ Ceph   │ │ 分布式  │            │             │
│  │  │        │ │        │ │ 存储     │            │             │
│  │  └────────┘ └────────┘ └────────┘            │             │
│  └─────────────────────────────────────────────┘             │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                  附加组件层                                   │ │
│  ├─────────────────────────────────────────────────────────────┤ │
│  │ • Ingress Controller (Nginx/Traefik)                         │ │
│  │ • Service Mesh (Istio)                                      │ │
│  │ • Prometheus + Grafana (监控)                               │ │
│  │ • ELK Stack (日志)                                           │ │
│  │ • Harbor (镜像仓库)                                           │ │
│  │ • Registry (私有仓库)                                         │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 集群组件

| 组件 | 作用 | 部署节点 |
|------|------|----------|
| **kube-apiserver** | Kubernetes API 服务 | Master |
| **kube-scheduler** | Pod 调度器 | Master |
| **kube-controller-manager** | 控制器管理 | Master |
| **etcd** | 分布式键值存储 | Master |
| **kubelet** | 节点代理 | Worker |
| **kube-proxy** | 网络代理 | Worker |
| **Container Runtime** | 容器运行时 | Worker |

---

## 🚀 Kubernetes 集群搭建

### 1. 环境准备脚本

```bash
#!/bin/bash
# ===================================================================
# Kubernetes 集群搭建脚本
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

# 安装依赖
install_dependencies() {
    log_info "安装系统依赖..."

    apt-get update
    apt-get install -y \
        apt-transport-https \
        ca-certificates \
        curl \
        gnupg \
        lsb-release \
        conntrack \
        ebtables \
        ethtool \
        socat \
        util-linux

    log_success "依赖安装完成"
}

# 安装 Docker
install_docker() {
    log_info "安装 Docker..."

    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg

    echo "deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null

    apt-get update
    apt-get install -y docker-ce docker-ce-cli containerd.io

    # 配置 Docker
    cat > /etc/docker/daemon.json <<EOF
{
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "100m",
    "max-file": "5"
  },
  "storage-driver": "overlay2",
  "live-restore": true,
  "userland-proxy": false,
  "experimental": false
}
EOF

    systemctl restart docker
    systemctl enable docker

    log_success "Docker 安装完成"
}

# 安装 containerd
install_containerd() {
    log_info "安装 containerd..."

    apt-get update
    apt-get install -y containerd.io

    # 配置 containerd
    mkdir -p /etc/containerd
    containerd config default > /etc/containerd/config.toml

    sed -i 's/SystemdCgroup = false/SystemdCgroup = true/' /etc/containerd/config.toml

    systemctl restart containerd
    systemctl enable containerd

    log_success "containerd 安装完成"
}

# 安装 kubectl
install_kubectl() {
    log_info "安装 kubectl..."

    curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"

    install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl

    log_success "kubectl 安装完成"
}

# 安装 kubeadm
install_kubeadm() {
    log_info "安装 kubeadm 和 kubelet..."

    curl -fsSL https://pkgs.k8s.io/core:/stable:/v1.29/deb/Release.key | \
        gpg --dearmor -o /etc/apt/keyrings/kubernetes-apt-keyring.gpg

    echo "deb [signed-by=/etc/apt/keyrings/kubernetes-apt-keyring.gpg] https://pkgs.k8s.io/core:/stable:/v1.29/deb/ /" | \
        tee /etc/apt/sources.list.d/kubernetes.list

    apt-get update
    apt-get install -y kubelet kubeadm kubectl

    # 锁定版本
    apt-mark hold kubelet kubeadm kubectl

    log_success "kubeadm 安装完成"
}

# 禁用 swap
disable_swap() {
    log_info "禁用 swap..."

    swapoff -a

    # 永久禁用
    sed -i '/ swap / s/^\(.*\)$/#\1/g' /etc/fstab

    log_success "swap 已禁用"
}

# 配置内核参数
configure_kernel() {
    log_info "配置内核参数..."

    cat <<EOF > /etc/modules-load.d/k8s.conf
overlay
br_netfilter
EOF

    cat <<EOF > /etc/sysctl.d/k8s.conf
net.bridge.bridge-nf-call-iptables  = 1
net.bridge.bridge-nf-call-ip6tables = 1
net.ipv4.ip_forward                 = 1
net.netfilter.nf_conntrack_max     = 131072
EOF

    modprobe overlay
    modprobe br_netfilter
    sysctl --system

    log_success "内核参数配置完成"
}

# 初始化 Kubernetes 集群（仅限 Master）
init_kubernetes_master() {
    log_info "初始化 Kubernetes 集群..."

    local POD_NETWORK="10.244.0.0/16"
    local SERVICE_NETWORK="10.96.0.0/12"

    kubeadm init \
        --pod-network-cidr=$POD_NETWORK \
        --service-cidr=$SERVICE_NETWORK \
        --apiserver-advertise-address=$(hostname -I | awk '{print $1}') \
        --kubernetes-version=1.29.0 \
        --cri-socket=unix:///var/run/containerd/containerd.sock

    # 配置 kubectl
    mkdir -p $HOME/.kube
    cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
    chown $(id -u):$(id -g) $HOME/.kube/config

    log_success "Kubernetes 集群初始化完成"
}

# 部署网络插件（Flannel）
deploy_network_plugin() {
    log_info "部署 Flannel 网络插件..."

    kubectl apply -f https://github.com/flannel-io/flannel/releases/download/v0.25.1/kube-flannel.yml

    log_success "Flannel 网络插件部署完成"
}

# 添加 Worker 节点
join_worker_nodes() {
    log_info "提示添加 Worker 节点..."
    log_info "在 Worker 节点上运行以下命令加入集群:"
    log_info "kubeadm join <MASTER_IP>:6443 --token <TOKEN> --discovery-token-ca-cert-hash sha256:<HASH>"

    # 生成加入命令
    local token=$(kubeadm token list --format table | tail -n 1 | awk '{print $1}')
    local hash=$(openssl x509 -pubkey -in /etc/kubernetes/pki/ca.crt | \
        openssl rsa -pubin -outform der 2>/dev/null | \
        openssl dgst -sha256 -hex | sed 's/^.* //')

    echo ""
    echo "============================================"
    echo "Worker 节点加入命令："
    echo "============================================"
    echo "kubeadm join $(hostname -I | awk '{print $1}'):6443 --token $token --discovery-token-ca-cert-hash sha256:$hash"
    echo "============================================"
    echo ""
}

# 验证安装
verify_installation() {
    log_info "验证 Kubernetes 安装..."

    # 检查节点状态
    kubectl get nodes

    # 检查系统组件
    kubectl get pods -n kube-system

    log_success "Kubernetes 安装验证完成"
}

# 主函数
main() {
    local NODE_TYPE=${1:-"master"}

    if [ "$NODE_TYPE" == "master" ]; then
        install_dependencies
        install_docker
        install_containerd
        install_kubectl
        install_kubeadm
        disable_swap
        configure_kernel
        init_kubernetes_master
        deploy_network_plugin
        verify_installation
        join_worker_nodes
    else
        install_dependencies
        install_docker
        install_containerd
        install_kubeadm
        disable_swap
        configure_kernel
        log_info "运行以下命令加入集群:"
        log_info "kubeadm join <MASTER_IP>:6443 --token <TOKEN> --discovery-token-ca-cert-hash sha256:<HASH>"
    fi

    log_success "集群搭建完成！"
}

main "$@"
```

### 2. Kubernetes 高可用集群

```yaml
# ha-cluster.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: haproxy-config
  namespace: kube-system
data:
  haproxy.cfg: |
    global
      daemon
      maxconn 4096

    defaults
      mode http
      timeout connect 5000ms
      timeout client 50000ms
      timeout server 50000ms

    frontend kubernetes-frontend
      bind *:6443
      default_backend kubernetes-backend

    backend kubernetes-backend
      balance roundrobin
      server master1 192.168.1.10:6443 check
      server master2 192.168.1.11:6443 check
      server master3 192.168.1.12:6443 check
```

### 3. Ingress Controller 部署

```yaml
# nginx-ingress.yaml
apiVersion: networking.k8s.io/v1
kind: IngressClass
metadata:
  name: nginx
spec:
  controller: k8s.io/ingress-nginx

---
apiVersion: v1
kind: Namespace
metadata:
  name: ingress-nginx

---
apiVersion: v1
kind: ServiceAccount
metadata:
  name: ingress-nginx
  namespace: ingress-nginx

---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: ingress-nginx
rules:
  - apiGroups: [""]
    resources: ["configmaps", "endpoints", "nodes", "pods", "secrets"]
    verbs: ["list", "watch"]
  - apiGroups: [""]
    resources: ["nodes"]
    verbs: ["get"]
  - apiGroups: [""]
    resources: ["services"]
    verbs: ["get", "list", "watch", "update"]
  - apiGroups: ["networking.k8s.io"]
    resources: ["ingresses"]
    verbs: ["get", "list", "watch"]
  - apiGroups: [""]
    resources: ["events"]
    verbs: ["create", "patch"]
  - apiGroups: ["networking.k8s.io"]
    resources: ["ingresses/status"]
    verbs: ["update"]

---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: ingress-nginx
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: ingress-nginx
subjects:
  - kind: ServiceAccount
    name: ingress-nginx
    namespace: ingress-nginx

---
apiVersion: v1
kind: ConfigMap
metadata:
  name: ingress-nginx-controller
  namespace: ingress-nginx

---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ingress-nginx-controller
  namespace: ingress-nginx
spec:
  replicas: 3
  selector:
    matchLabels:
      app.kubernetes.io/name: ingress-nginx
      app.kubernetes.io/component: controller
  template:
    metadata:
      labels:
        app.kubernetes.io/name: ingress-nginx
        app.kubernetes.io/component: controller
    spec:
      serviceAccountName: ingress-nginx
      containers:
      - name: controller
        image: registry.k8s.io/ingress-nginx/controller:v1.9.4
        args:
        - /nginx-ingress-controller
        - --configmap=$(POD_NAMESPACE)/ingress-nginx-controller
        - --validating-webhook=:8443
        - --validating-webhook-certificate=/usr/local/certificates/cert
        - --validating-webhook-key=/usr/local/certificates/key
        ports:
        - name: http
          containerPort: 80
        - name: https
          containerPort: 443
        - name: webhook
          containerPort: 8443
        livenessProbe:
          httpGet:
            path: /healthz
            port: 10254
            scheme: HTTP
          initialDelaySeconds: 10
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /healthz
            port: 10254
            scheme: HTTP
          initialDelaySeconds: 10
          periodSeconds: 10
        resources:
          limits:
            cpu: 1000m
            memory: 1Gi
          requests:
            cpu: 100m
            memory: 100Mi
        volumeMounts:
        - name: usrlocalcertificates
          mountPath: /usr/local/certificates
          readOnly: true
      volumes:
      - name: usrlocalcertificates
        emptyDir: {}

---
apiVersion: v1
kind: Service
metadata:
  name: ingress-nginx-controller
  namespace: ingress-nginx
spec:
  type: LoadBalancer
  externalTrafficPolicy: Cluster
  ports:
  - name: http
    port: 80
    protocol: TCP
    targetPort: http
  - name: https
    port: 443
    protocol: TCP
    targetPort: https
  selector:
    app.kubernetes.io/name: ingress-nginx
    app.kubernetes.io/component: controller
```

---

## 📦 Helm Chart 打包

### 1. Helm Chart 结构

```bash
# 目录结构示例
basebackend/
├── Chart.yaml              # Chart 元数据
├── values.yaml             # 默认配置值
├── templates/              # 模板目录
│   ├── _helpers.tpl        # 助手函数
│   ├── deployment.yaml     # 部署资源
│   ├── service.yaml        # 服务资源
│   ├── ingress.yaml        # Ingress 资源
│   ├── configmap.yaml      # 配置映射
│   ├── secret.yaml         # 密钥
│   ├── hpa.yaml            # 水平自动伸缩
│   ├── pdb.yaml            # PodDisruptionBudget
│   └── NOTES.txt           # 说明文档
├── charts/                 # 依赖 Chart
├── crds/                   # 自定义资源定义
└── templates.tests/        # 测试模板
```

### 2. Chart.yaml 配置

```yaml
# Chart.yaml
apiVersion: v2
name: basebackend
description: BaseBackend 微服务平台
type: application
version: 1.0.0
appVersion: "1.0.0"
home: https://github.com/basebackend
sources:
  - https://github.com/basebackend/basebackend
maintainers:
  - name: basebackend-team
    email: team@basebackend.com
keywords:
  - microservices
  - spring-cloud
  - spring-boot
dependencies:
  - name: common
    version: "1.x.x"
    repository: https://charts.bitnami.com/bitnami
    alias: common
  - name: postgresql
    version: "12.x.x"
    repository: https://charts.bitnami.com/bitnami
    condition: postgresql.enabled
  - name: redis
    version: "18.x.x"
    repository: https://charts.bitnami.com/bitnami
    condition: redis.enabled
annotations:
  category: Application
  licenses: Apache-2.0
```

### 3. values.yaml 配置

```yaml
# values.yaml
# 全局配置
global:
  imageRegistry: ""
  imagePullSecrets: []
  storageClass: ""

# 镜像配置
image:
  registry: docker.io
  repository: basebackend
  tag: "1.0.0"
  pullPolicy: IfNotPresent
  pullSecrets: []

# 镜像拉取策略
imagePullSecrets: []
nameOverride: ""
fullnameOverride: ""

# 服务配置
service:
  type: ClusterIP
  port: 80
  targetPort: 8080

# Ingress 配置
ingress:
  enabled: true
  className: "nginx"
  annotations:
    cert-manager.io/cluster-issuer: letsencrypt-prod
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/force-ssl-redirect: "true"
  hosts:
    - host: api.basebackend.com
      paths:
        - path: /
          pathType: Prefix
  tls:
    - secretName: basebackend-tls
      hosts:
        - api.basebackend.com

# 资源限制
resources:
  limits:
    cpu: 2000m
    memory: 2Gi
  requests:
    cpu: 1000m
    memory: 1Gi

# 自动伸缩
autoscaling:
  enabled: true
  minReplicas: 2
  maxReplicas: 10
  targetCPUUtilizationPercentage: 70
  targetMemoryUtilizationPercentage: 80

# 健康检查
livenessProbe:
  httpGet:
    path: /actuator/health
    port: 8080
  initialDelaySeconds: 120
  periodSeconds: 30
  timeoutSeconds: 10
  failureThreshold: 3

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 60
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 3

# 部署配置
replicaCount: 3

# 节点选择器
nodeSelector: {}

# 容忍度
tolerations: []

# 亲和性
affinity:
  podAntiAffinity:
    preferredDuringSchedulingIgnoredDuringExecution:
      - weight: 100
        podAffinityTerm:
          labelSelector:
            matchLabels:
              app.kubernetes.io/name: basebackend
          topologyKey: kubernetes.io/hostname

# 配置文件
config:
  # 应用程序配置
  application:
    name: basebackend
    profiles:
      active: prod
  # 数据库配置
  database:
    host: ""
    port: 3306
    username: ""
    password: ""
    database: ""
  # Redis 配置
  redis:
    host: ""
    port: 6379
    password: ""

# 密钥配置
secret:
  enabled: true
  annotations: {}
  labels: {}
  data: {}
  stringData: {}
  type: Opaque

# 配置映射
configmap:
  enabled: true
  data: {}
  annotations: {}

# 服务监控
monitoring:
  enabled: true
  serviceMonitor:
    enabled: true
    interval: 30s
    path: /actuator/prometheus
  podMonitor:
    enabled: false
    path: /metrics

# 权限配置
serviceAccount:
  create: true
  name: ""
  annotations: {}
  automountServiceAccountToken: true

# 安全上下文
securityContext:
  allowPrivilegeEscalation: false
  capabilities:
    drop: ["ALL"]
  readOnlyRootFilesystem: true
  runAsNonRoot: true
  runAsUser: 1001
  fsGroup: 1001

# Pod 安全上下文
podSecurityContext:
  runAsNonRoot: true
  runAsUser: 1001
  fsGroup: 1001

# 网络策略
networkPolicy:
  enabled: true
  egress:
    enabled: false
    rules:
      - toPorts:
          - port: 80
          - port: 443
  ingress:
    enabled: true
    rules:
      - from:
          - namespaceSelector:
              matchLabels:
                name: ingress-nginx
        ports:
          - port: 8080

# PodDisruptionBudget
podDisruptionBudget:
  enabled: true
  minAvailable: 2

# 水平 Pod 自动缩放器
hpa:
  enabled: true
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70

# Vertical Pod Autoscaler
vpa:
  enabled: false
  updateMode: "Auto"
  resources:
    limits:
      cpu: 2000m
      memory: 2Gi
    requests:
      cpu: 1000m
      memory: 1Gi

# Prometheus 配置
prometheus:
  enabled: true
  serviceMonitor:
    enabled: true
    interval: 30s
    path: /actuator/prometheus
    additionalLabels: {}

# 附加容器
sidecarContainers: []

# 附加卷
extraVolumes: []

# 附加卷挂载
extraVolumeMounts: []

# 初始化容器
initContainers: []

# 后置钩子
postStartHook:
  enabled: false
  exec:
    command:
      - /bin/sh
      - -c
      - echo "Application started"

# 前置钩子
preStopHook:
  enabled: false
  exec:
    command:
      - /bin/sh
      - -c
      - echo "Application stopping"

# 测试配置
tests:
  enabled: true
  image:
    repository: curlimages/curl
    tag: "8.4.0"
    pullPolicy: IfNotPresent
```

### 4. Deployment 模板

```yaml
# templates/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ include "basebackend.fullname" . }}
  namespace: {{ .Release.Namespace | quote }}
  labels:
    {{- include "basebackend.labels" . | nindent 4 }}
  annotations:
    {{- with .Values.annotations }}
    {{- toYaml . | nindent 4 }}
    {{- end }}
spec:
  {{- if not .Values.autoscaling.enabled }}
  replicas: {{ .Values.replicaCount }}
  {{- end }}
  selector:
    matchLabels:
      {{- include "basebackend.selectorLabels" . | nindent 6 }}
  template:
    metadata:
      labels:
        {{- include "basebackend.selectorLabels" . | nindent 8 }}
      {{- if .Values.podLabels }}
        {{- toYaml .Values.podLabels | nindent 8 }}
      {{- end }}
      annotations:
        checksum/config: {{ include (print $.Template.BasePath "/configmap.yaml") . | sha256sum }}
        checksum/secret: {{ include (print $.Template.BasePath "/secret.yaml") . | sha256sum }}
        {{- with .Values.podAnnotations }}
        {{- toYaml . | nindent 8 }}
        {{- end }}
    spec:
      {{- if .Values.imagePullSecrets }}
      imagePullSecrets:
        {{- toYaml .Values.imagePullSecrets | nindent 8 }}
      {{- end }}
      serviceAccountName: {{ include "basebackend.serviceAccountName" . }}
      securityContext:
        {{- toYaml .Values.podSecurityContext | nindent 8 }}
      {{- if .Values.initContainers }}
      initContainers:
        {{- tpl (toYaml .Values.initContainers) . | nindent 8 }}
      {{- end }}
      containers:
      - name: basebackend
        image: "{{ .Values.image.registry }}/{{ .Values.image.repository }}:{{ .Values.image.tag }}"
        imagePullPolicy: {{ .Values.image.pullPolicy }}
        ports:
        - name: http
          containerPort: 8080
          protocol: TCP
        - name: metrics
          containerPort: 9090
          protocol: TCP
        {{- if .Values.livenessProbe }}
        livenessProbe:
          {{- toYaml .Values.livenessProbe | nindent 10 }}
        {{- end }}
        {{- if .Values.readinessProbe }}
        readinessProbe:
          {{- toYaml .Values.readinessProbe | nindent 10 }}
        {{- end }}
        {{- if .Values.startupProbe }}
        startupProbe:
          {{- toYaml .Values.startupProbe | nindent 10 }}
        {{- end }}
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: {{ .Values.config.application.profiles.active | quote }}
        - name: APPLICATION_NAME
          value: {{ .Values.config.application.name | quote }}
        {{- with .Values.extraEnvVars }}
        {{- toYaml . | nindent 8 }}
        {{- end }}
        envFrom:
        {{- if .Values.configmap.enabled }}
        - configMapRef:
            name: {{ include "basebackend.fullname" . }}-config
        {{- end }}
        {{- if .Values.secret.enabled }}
        - secretRef:
            name: {{ include "basebackend.fullname" . }}-secret
        {{- end }}
        resources:
          {{- toYaml .Values.resources | nindent 10 }}
        volumeMounts:
        - name: tmp
          mountPath: /tmp
        - name: logs
          mountPath: /app/logs
        {{- if .Values.extraVolumeMounts }}
        {{- toYaml .Values.extraVolumeMounts | nindent 8 }}
        {{- end }}
        {{- if .Values.securityContext }}
        securityContext:
          {{- toYaml .Values.securityContext | nindent 10 }}
        {{- end }}
      {{- if .Values.sidecarContainers }}
      {{- tpl (toYaml .Values.sidecarContainers) . | nindent 6 }}
      {{- end }}
      volumes:
      - name: tmp
        emptyDir: {}
      - name: logs
        emptyDir: {}
      {{- if .Values.extraVolumes }}
      {{- toYaml .Values.extraVolumes | nindent 6 }}
      {{- end }}
      {{- if .Values.nodeSelector }}
      nodeSelector:
        {{- toYaml .Values.nodeSelector | nindent 8 }}
      {{- end }}
      {{- if .Values.tolerations }}
      tolerations:
        {{- toYaml .Values.tolerations | nindent 8 }}
      {{- end }}
      {{- if .Values.affinity }}
      affinity:
        {{- toYaml .Values.affinity | nindent 8 }}
      {{- end }}
      {{- if .Values.topologySpreadConstraints }}
      topologySpreadConstraints:
        {{- toYaml .Values.topologySpreadConstraints | nindent 8 }}
      {{- end }}
```

### 5. Service 模板

```yaml
# templates/service.yaml
apiVersion: v1
kind: Service
metadata:
  name: {{ include "basebackend.fullname" . }}
  namespace: {{ .Release.Namespace | quote }}
  labels:
    {{- include "basebackend.labels" . | nindent 4 }}
  {{- with .Values.service.annotations }}
  annotations:
    {{- toYaml . | nindent 4 }}
  {{- end }}
spec:
  type: {{ .Values.service.type }}
  ports:
  - port: {{ .Values.service.port }}
    targetPort: http
    protocol: TCP
    name: http
  {{- if .Values.metrics.enabled }}
  - port: {{ .Values.metrics.port }}
    targetPort: metrics
    protocol: TCP
    name: metrics
  {{- end }}
  selector:
    {{- include "basebackend.selectorLabels" . | nindent 4 }}
  {{- if eq .Values.service.type "LoadBalancer" }}
  loadBalancerSourceRanges:
    {{- toYaml .Values.service.loadBalancerSourceRanges | nindent 4 }}
  {{- end }}
```

### 6. 助手函数

```yaml
# templates/_helpers.tpl
{{/*
Expand the name of the chart.
*/}}
{{- define "basebackend.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "basebackend.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "basebackend.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "basebackend.labels" -}}
helm.sh/chart: {{ include "basebackend.chart" . }}
{{ include "basebackend.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "basebackend.selectorLabels" -}}
app.kubernetes.io/name: {{ include "basebackend.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "basebackend.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "basebackend.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Return the proper Docker Image Registry Secret Names
*/}}
{{- define "basebackend.imagePullSecrets" -}}
{{- include "common.images.pullSecrets" (dict "images" .Values.image "global" .Values.global) }}
{{- end }}

{{/*
Create the name of the ConfigMap to use
*/}}
{{- define "basebackend.configmapName" -}}
{{- printf "%s-config" (include "basebackend.fullname" .) }}
{{- end }}

{{/*
Create the name of the Secret to use
*/}}
{{- define "basebackend.secretName" -}}
{{- printf "%s-secret" (include "basebackend.fullname" .) }}
{{- end }}

{{/*
Return true if we should create a ConfigMap
*/}}
{{- define "basebackend.createConfigMap" -}}
{{- if and .Values.configmap.enabled (or .Values.configmap.data .Values.config.existingConfigMap) }}
{{- true }}
{{- end }}
{{- end }}

{{/*
Return true if we should create a Secret
*/}}
{{- define "basebackend.createSecret" -}}
{{- if and .Values.secret.enabled (or .Values.secret.data .Values.secret.stringData .Values.config.existingSecret) }}
{{- true }}
{{- end }}
{{- end }}

{{/*
Validate values
*/}}
{{- define "basebackend.validateValues" -}}
{{- if not (hasKey .Values "replicaCount") }}
{{- fail "replicaCount is required" }}
{{- end }}
{{- end }}
```

### 7. 完整性检查

```bash
#!/bin/bash
# helm-validate.sh

log_info() {
    echo -e "\033[0;34m[INFO]\033[0m $1"
}

log_success() {
    echo -e "\033[0;32m[SUCCESS]\033[0m $1"
}

log_error() {
    echo -e "\033[0;31m[ERROR]\033[0m $1"
}

# 验证 Chart 语法
validate_chart() {
    log_info "验证 Chart 语法..."

    if helm lint .; then
        log_success "Chart 语法验证通过"
    else
        log_error "Chart 语法验证失败"
        return 1
    fi
}

# 渲染模板
render_templates() {
    log_info "渲染模板..."

    if helm template basebackend . --dry-run; then
        log_success "模板渲染通过"
    else
        log_error "模板渲染失败"
        return 1
    fi
}

# 安装测试
test_install() {
    log_info "测试安装..."

    if helm install test-release . --dry-run; then
        log_success "安装测试通过"
    else
        log_error "安装测试失败"
        return 1
    fi
}

# 检查依赖
check_dependencies() {
    log_info "检查依赖..."

    if helm dependency list; then
        log_success "依赖检查通过"
    else
        log_error "依赖检查失败"
        return 1
    fi
}

# 检查资源
check_resources() {
    log_info "检查资源..."

    resources=$(helm template basebackend . --show-only templates/deployment.yaml | grep "cpu\|memory" || true)

    if [ -n "$resources" ]; then
        log_success "资源检查通过"
        echo "$resources"
    else
        log_warn "未发现资源限制"
    fi
}

# 主函数
main() {
    echo "========================================"
    echo "      Helm Chart 验证"
    echo "========================================"
    echo ""

    check_dependencies
    echo ""

    validate_chart
    echo ""

    render_templates
    echo ""

    test_install
    echo ""

    check_resources
    echo ""

    log_success "Chart 验证完成！"
}

main "$@"
```

---

## 🔄 CI/CD 流水线

### 1. GitLab CI/CD 配置

```yaml
# .gitlab-ci.yml
variables:
  KUBECONFIG: /tmp/kubeconfig
  HELM_VERSION: "3.12.0"
  DOCKER_DRIVER: overlay2
  DOCKER_TMP_CERT_DIR: /certs/client

stages:
  - validate
  - build
  - test
  - package
  - deploy-dev
  - deploy-staging
  - deploy-prod
  - cleanup

# 默认配置
.default_template: &default
  image: alpine/helm:${HELM_VERSION}
  before_script:
    - apk add --no-cache git curl openssh
    - mkdir -p ~/.ssh
    - echo "${SSH_PRIVATE_KEY}" | tr -d '\r' > ~/.ssh/id_rsa
    - chmod 600 ~/.ssh/id_rsa
    - ssh-keyscan ${K8S_CLUSTER_HOST} >> ~/.ssh/known_hosts
    - kubectl config set-cluster ${K8S_CLUSTER_NAME} --server=${K8S_CLUSTER_HOST}
    - kubectl config set-cluster ${K8S_CLUSTER_NAME} --insecure-skip-tls-verify=true
    - kubectl config set-credentials ${K8S_CLUSTER_USER} --token="${K8S_CLUSTER_TOKEN}"
    - kubectl config set-context ${K8S_CLUSTER_NAME} --cluster=${K8S_CLUSTER_NAME} --user=${K8S_CLUSTER_USER}
    - kubectl config use-context ${K8S_CLUSTER_NAME}
    - helm repo add bitnami https://charts.bitnami.com/bitnami
    - helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
    - helm repo update

# 代码验证
validate:code:
  stage: validate
  image: alpine:latest
  script:
    - apk add --no-cache git
    - git fetch origin ${CI_COMMIT_BRANCH}
    - git diff --name-only origin/${CI_COMMIT_BRANCH}...${CI_COMMIT_SHA} | grep -E '\.(java|xml|yml|yaml)$' || true

validate:chart:
  stage: validate
  <<: *default
  script:
    - helm lint .

validate:yaml:
  stage: validate
  image: alpine/yq:latest
  script:
    - yq eval-all 'select(kind == "sequence" and all(.metadata.labels."app.kubernetes.io/name" == "basebackend"))' charts/*/values.yaml | \
      yq e 'all(. == true)' -

# Docker 构建
build:docker:
  stage: build
  image: docker:latest
  services:
    - docker:dind
  before_script:
    - echo ${DOCKER_REGISTRY_PASSWORD} | docker login -u ${DOCKER_REGISTRY_USER} --password-stdin ${DOCKER_REGISTRY}
  script:
    - docker build -t ${DOCKER_REGISTRY}/${CI_PROJECT_PATH}:${CI_COMMIT_SHA} .
    - docker push ${DOCKER_REGISTRY}/${CI_PROJECT_PATH}:${CI_COMMIT_SHA}
    - docker tag ${DOCKER_REGISTRY}/${CI_PROJECT_PATH}:${CI_COMMIT_SHA} ${DOCKER_REGISTRY}/${CI_PROJECT_PATH}:${CI_COMMIT_BRANCH}
    - docker push ${DOCKER_REGISTRY}/${CI_PROJECT_PATH}:${CI_COMMIT_BRANCH}
  rules:
    - if: $CI_COMMIT_BRANCH
    - if: $CI_MERGE_REQUEST_IID

# 单元测试
test:unit:
  stage: test
  image: maven:3.9-eclipse-temurin-17-jammy
  script:
    - mvn clean test -DskipTests=false
  artifacts:
    reports:
      junit:
        - target/surefire-reports/TEST-*.xml
  coverage: '/Code coverage: \d+\.\d+/'
  rules:
    - if: $CI_COMMIT_BRANCH

# 集成测试
test:integration:
  stage: test
  image: maven:3.9-eclipse-temurin-17-jammy
  script:
    - mvn clean verify -Pintegration-tests
  services:
    - name: mysql:8.0
      alias: mysql
    - name: redis:7
      alias: redis
  variables:
    MYSQL_ROOT_PASSWORD: "root"
    MYSQL_DATABASE: "basebackend_test"
  rules:
    - if: $CI_COMMIT_BRANCH

# 安全扫描
security:scan:
  stage: test
  image: aquasec/trivy:latest
  script:
    - trivy image --exit-code 1 --severity HIGH,CRITICAL ${DOCKER_REGISTRY}/${CI_PROJECT_PATH}:${CI_COMMIT_SHA}
  rules:
    - if: $CI_COMMIT_BRANCH

# 依赖检查
dependency:scan:
  stage: test
  image: owasp/dependency-check:latest
  script:
    - dependency-check.sh --project "basebackend" --scan $(pwd) --enableRetired
  artifacts:
    paths:
      - reports/
  rules:
    - if: $CI_COMMIT_BRANCH

# Helm Package
package:helm:
  stage: package
  <<: *default
  script:
    - helm package .
    - helm repo index --url ${HELM_REPO_URL} .
  artifacts:
    paths:
      - "*.tgz"
      - index.yaml
  rules:
    - if: $CI_COMMIT_TAG
    - if: $CI_COMMIT_BRANCH == "main"

# 部署到开发环境
deploy:dev:
  stage: deploy-dev
  <<: *default
  script:
    - helm upgrade --install basebackend-dev ./charts/basebackend \
      --namespace basebackend-dev \
      --create-namespace \
      --set image.tag=${CI_COMMIT_SHA} \
      --set ingress.hosts[0].host=api-dev.basebackend.com \
      --set resources.limits.cpu=500m \
      --set resources.limits.memory=512Mi \
      --wait --timeout=300s
    - kubectl rollout status deployment/basebackend-dev -n basebackend-dev --timeout=300s
  environment:
    name: development
    url: https://api-dev.basebackend.com
    deployment_tier: development
  rules:
    - if: $CI_COMMIT_BRANCH == "develop"

# 部署到测试环境
deploy:staging:
  stage: deploy-staging
  <<: *default
  script:
    - helm upgrade --install basebackend-staging ./charts/basebackend \
      --namespace basebackend-staging \
      --create-namespace \
      --set image.tag=${CI_COMMIT_SHA} \
      --set ingress.hosts[0].host=api-staging.basebackend.com \
      --set replicaCount=3 \
      --set resources.limits.cpu=1000m \
      --set resources.limits.memory=1Gi \
      --set autoscaling.enabled=true \
      --set autoscaling.minReplicas=3 \
      --set autoscaling.maxReplicas=10 \
      --wait --timeout=600s
    - kubectl rollout status deployment/basebackend-staging -n basebackend-staging --timeout=600s
  environment:
    name: staging
    url: https://api-staging.basebackend.com
    deployment_tier: staging
  rules:
    - if: $CI_COMMIT_BRANCH == "develop"
  when: manual

# 部署到生产环境
deploy:prod:
  stage: deploy-prod
  <<: *default
  script:
    - helm upgrade --install basebackend-prod ./charts/basebackend \
      --namespace basebackend-prod \
      --create-namespace \
      --set image.tag=${CI_COMMIT_SHA} \
      --set ingress.hosts[0].host=api.basebackend.com \
      --set replicaCount=5 \
      --set resources.limits.cpu=2000m \
      --set resources.limits.memory=2Gi \
      --set autoscaling.enabled=true \
      --set autoscaling.minReplicas=5 \
      --set autoscaling.maxReplicas=20 \
      --set monitoring.enabled=true \
      --set networkPolicy.enabled=true \
      --set podDisruptionBudget.enabled=true \
      --wait --timeout=900s
    - kubectl rollout status deployment/basebackend-prod -n basebackend-prod --timeout=900s
  environment:
    name: production
    url: https://api.basebackend.com
    deployment_tier: production
  rules:
    - if: $CI_COMMIT_TAG
  when: manual

# 健康检查
health:check:
  stage: deploy-prod
  image: alpine/curl:latest
  script:
    - curl -f https://api.basebackend.com/actuator/health
  environment:
    name: production
  rules:
    - if: $CI_COMMIT_TAG
  when: on_success

# 回滚
rollback:
  stage: cleanup
  <<: *default
  script:
    - helm rollback basebackend-prod -n basebackend-prod
    - kubectl rollout status deployment/basebackend-prod -n basebackend-prod
  environment:
    name: production
    url: https://api.basebackend.com
  rules:
    - if: $CI_PIPELINE_SOURCE == "web"
  when: manual

# 清理旧的部署
cleanup:old:
  stage: cleanup
  image: alpine/helm:${HELM_VERSION}
  script:
    - helm list -n basebackend-dev --date --old | head -n -5 | awk '{print $1}' | \
      xargs -I {} helm uninstall {} -n basebackend-dev || true
  rules:
    - if: $CI_COMMIT_BRANCH == "develop"
  when: manual
```

### 2. GitHub Actions 配置

```yaml
# .github/workflows/deploy.yml
name: Deploy to Kubernetes

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Cache Maven dependencies
        uses: actions/cache@v4
        with:
          path: ~/.m2
          key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
          restore-keys: ${{ runner.os }}-m2

      - name: Run tests
        run: mvn clean verify

      - name: Upload test results
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: test-results
          path: target/surefire-reports/

  build:
    needs: test
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Log in to Container Registry
        uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Extract metadata
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}
          tags: |
            type=ref,event=branch
            type=ref,event=pr
            type=sha,prefix={{branch}}-

      - name: Build and push Docker image
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
          platforms: linux/amd64,linux/arm64

  validate-helm:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up Helm
        uses: azure/setup-helm@v4
        with:
          version: ${{ env.HELM_VERSION }}

      - name: Set up chart-testing
        uses: helm/chart-testing-action@v2.6.1

      - name: Lint chart
        run: ct lint

      - name: Render chart
        run: helm template basebackend ./charts/basebackend --dry-run

  deploy-dev:
    needs: [build, validate-helm]
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/develop'
    environment: development

    steps:
      - uses: actions/checkout@v4

      - name: Set up kubectl
        uses: azure/setup-kubectl@v4
        with:
          version: 'latest'

      - name: Download Helm chart
        uses: actions/download-artifact@v4
        with:
          name: helm-chart
          path: charts/

      - name: Deploy to Development
        run: |
          helm upgrade --install basebackend-dev ./charts/basebackend \
            --namespace basebackend-dev \
            --create-namespace \
            --set image.tag=${{ github.sha }} \
            --set ingress.hosts[0].host=api-dev.basebackend.com \
            --wait --timeout=300s
        env:
          KUBECONFIG: ${{ secrets.KUBECONFIG_DEV }}

  deploy-staging:
    needs: [build, validate-helm]
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/develop'
    environment: staging

    steps:
      - uses: actions/checkout@v4

      - name: Set up kubectl
        uses: azure/setup-kubectl@v4
        with:
          version: 'latest'

      - name: Deploy to Staging
        run: |
          helm upgrade --install basebackend-staging ./charts/basebackend \
            --namespace basebackend-staging \
            --create-namespace \
            --set image.tag=${{ github.sha }} \
            --set ingress.hosts[0].host=api-staging.basebackend.com \
            --wait --timeout=600s
        env:
          KUBECONFIG: ${{ secrets.KUBECONFIG_STAGING }}

  deploy-prod:
    needs: [build, validate-helm]
    runs-on: ubuntu-latest
    if: startsWith(github.ref, 'refs/tags/')
    environment: production

    steps:
      - uses: actions/checkout@v4

      - name: Set up kubectl
        uses: azure/setup-kubectl@v4
        with:
          version: 'latest'

      - name: Deploy to Production
        run: |
          helm upgrade --install basebackend-prod ./charts/basebackend \
            --namespace basebackend-prod \
            --create-namespace \
            --set image.tag=${{ github.ref_name }} \
            --set ingress.hosts[0].host=api.basebackend.com \
            --wait --timeout=900s
        env:
          KUBECONFIG: ${{ secrets.KUBECONFIG_PROD }}

      - name: Health check
        run: |
          sleep 30
          curl -f https://api.basebackend.com/actuator/health
```

### 3. ArgoCD 配置

```yaml
# argocd-application.yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: basebackend
  namespace: argocd
  finalizers:
    - resources-finalizer.argocd.argoproj.io
spec:
  project: default
  source:
    repoURL: https://github.com/basebackend/basebackend.git
    targetRevision: HEAD
    path: charts/basebackend
    helm:
      valueFiles:
        - values-production.yaml
  destination:
    server: https://kubernetes.default.svc
    namespace: basebackend-prod
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
    syncOptions:
      - CreateNamespace=true
      - PruneLast=true
    retry:
      limit: 5
      backoff:
        duration: 5s
        factor: 2
        maxDuration: 3m

---
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: basebackend-staging
  namespace: argocd
spec:
  project: default
  source:
    repoURL: https://github.com/basebackend/basebackend.git
    targetRevision: develop
    path: charts/basebackend
    helm:
      valueFiles:
        - values-staging.yaml
  destination:
    server: https://kubernetes.default.svc
    namespace: basebackend-staging
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
      allowEmpty: false
```

### 4. Jenkins Pipeline

```groovy
// Jenkinsfile
pipeline {
    agent any

    environment {
        REGISTRY = credentials('docker-registry')
        K8S_DEV = credentials('kubeconfig-dev')
        K8S_STAGING = credentials('kubeconfig-staging')
        K8S_PROD = credentials('kubeconfig-prod')
    }

    options {
        timeout(time: 60, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timestamps()
    }

    stages {
        stage('Validate') {
            parallel {
                stage('Lint Helm') {
                    steps {
                        script {
                            sh 'helm lint charts/basebackend'
                        }
                    }
                }
                stage('Security Scan') {
                    steps {
                        script {
                            sh 'trivy fs --exit-code 1 --severity HIGH,CRITICAL .'
                        }
                    }
                }
            }
        }

        stage('Test') {
            parallel {
                stage('Unit Tests') {
                    steps {
                        script {
                            sh 'mvn clean test'
                        }
                    }
                    post {
                        always {
                            junit 'target/surefire-reports/*.xml'
                        }
                    }
                }
                stage('Integration Tests') {
                    steps {
                        script {
                            sh 'mvn clean verify -Pintegration-tests'
                        }
                    }
                }
            }
        }

        stage('Build & Push') {
            steps {
                script {
                    def image = docker.build("${REGISTRY_USR}/basebackend:${env.BUILD_NUMBER}")
                    docker.withRegistry("https://${REGISTRY_USR}", REGISTRY_PSW) {
                        image.push()
                        image.push('latest')
                    }
                }
            }
        }

        stage('Deploy Dev') {
            when {
                branch 'develop'
            }
            steps {
                script {
                    withKubeConfig([credentialsId: K8S_DEV, serverUrl: K8S_DEV_URL]) {
                        sh '''
                            helm upgrade --install basebackend-dev charts/basebackend \
                                --namespace basebackend-dev \
                                --create-namespace \
                                --set image.tag=${BUILD_NUMBER} \
                                --set ingress.hosts[0].host=api-dev.basebackend.com \
                                --wait --timeout=300s
                        '''
                    }
                }
            }
        }

        stage('Deploy Staging') {
            when {
                branch 'develop'
            }
            steps {
                script {
                    withKubeConfig([credentialsId: K8S_STAGING, serverUrl: K8S_STAGING_URL]) {
                        sh '''
                            helm upgrade --install basebackend-staging charts/basebackend \
                                --namespace basebackend-staging \
                                --create-namespace \
                                --set image.tag=${BUILD_NUMBER} \
                                --set ingress.hosts[0].host=api-staging.basebackend.com \
                                --wait --timeout=600s
                        '''
                    }
                }
            }
            post {
                always {
                    script {
                        sh '''
                            sleep 30
                            curl -f https://api-staging.basebackend.com/actuator/health
                        '''
                    }
                }
            }
        }

        stage('Deploy Prod') {
            when {
                buildingTag()
            }
            steps {
                script {
                    withKubeConfig([credentialsId: K8S_PROD, serverUrl: K8S_PROD_URL]) {
                        sh '''
                            helm upgrade --install basebackend-prod charts/basebackend \
                                --namespace basebackend-prod \
                                --create-namespace \
                                --set image.tag=${TAG_NAME} \
                                --set ingress.hosts[0].host=api.basebackend.com \
                                --wait --timeout=900s
                        '''
                    }
                }
            }
            post {
                always {
                    script {
                        sh '''
                            sleep 60
                            curl -f https://api.basebackend.com/actuator/health
                        '''
                    }
                }
            }
        }
    }

    post {
        always {
            cleanWs()
        }
        success {
            emailext (
                subject: "Deployment Successful: ${env.JOB_NAME} - ${env.BUILD_NUMBER}",
                body: "Deployment to ${env.GIT_BRANCH} completed successfully.",
                to: "${env.CHANGE_AUTHOR_EMAIL}"
            )
        }
        failure {
            emailext (
                subject: "Deployment Failed: ${env.JOB_NAME} - ${env.BUILD_NUMBER}",
                body: "Deployment to ${env.GIT_BRANCH} failed.",
                to: "${env.CHANGE_AUTHOR_EMAIL}"
            )
        }
    }
}
```

---

## 🧪 测试与验证

### 1. 集群验证脚本

```bash
#!/bin/bash
# cluster-validation.sh

log_info() {
    echo -e "\033[0;34m[INFO]\033[0m $1"
}

log_success() {
    echo -e "\033[0;32m[SUCCESS]\033[0m $1"
}

log_error() {
    echo -e "\033[0;31m[ERROR]\033[0m $1"
}

# 检查集群节点
check_nodes() {
    log_info "检查集群节点..."

    nodes=$(kubectl get nodes)
    echo "$nodes"

    if echo "$nodes" | grep -q "Ready"; then
        log_success "所有节点状态正常"
    else
        log_error "节点状态异常"
        return 1
    fi
}

# 检查系统组件
check_system_components() {
    log_info "检查系统组件..."

    components=$(kubectl get pods -n kube-system)
    echo "$components"

    if echo "$components" | grep -q "Running"; then
        log_success "系统组件运行正常"
    else
        log_error "系统组件异常"
        return 1
    fi
}

# 检查 Ingress
check_ingress() {
    log_info "检查 Ingress 控制器..."

    ingress_pods=$(kubectl get pods -n ingress-nginx)
    echo "$ingress_pods"

    if echo "$ingress_pods" | grep -q "Running"; then
        log_success "Ingress 控制器运行正常"
    else
        log_error "Ingress 控制器异常"
        return 1
    fi
}

# 检查存储
check_storage() {
    log_info "检查存储类..."

    storage_classes=$(kubectl get storageclass)
    echo "$storage_classes"

    if kubectl get storageclass | grep -q "default"; then
        log_success "默认存储类已配置"
    else
        log_warn "未找到默认存储类"
    fi
}

# 检查网络插件
check_network_plugin() {
    log_info "检查网络插件..."

    # 检查 Flannel
    if kubectl get pods -n kube-flannel | grep -q "Running"; then
        log_success "Flannel 网络插件运行正常"
        return 0
    fi

    # 检查 Calico
    if kubectl get pods -n kube-system | grep -q "calico"; then
        log_success "Calico 网络插件运行正常"
        return 0
    fi

    log_warn "未检测到网络插件"
}

# 检查监控
check_monitoring() {
    log_info "检查监控组件..."

    # 检查 Prometheus
    if kubectl get pods -n monitoring | grep -q "prometheus"; then
        log_success "Prometheus 运行正常"
    else
        log_warn "Prometheus 未部署"
    fi

    # 检查 Grafana
    if kubectl get pods -n monitoring | grep -q "grafana"; then
        log_success "Grafana 运行正常"
    else
        log_warn "Grafana 未部署"
    fi
}

# 检查服务网格
check_service_mesh() {
    log_info "检查服务网格..."

    if kubectl get pods -n istio-system | grep -q "istiod"; then
        log_success "Istio 服务网格运行正常"
    else
        log_warn "Istio 未部署"
    fi
}

# 性能测试
performance_test() {
    log_info "执行性能测试..."

    # 创建测试命名空间
    kubectl create namespace load-test --dry-run=client -o yaml | kubectl apply -f -

    # 部署测试 Pod
    kubectl run nginx-test --image=nginx --namespace=load-test --restart=Never --dry-run=client -o yaml | kubectl apply -f -

    sleep 5

    # 检查 Pod 是否运行
    if kubectl get pods -n load-test | grep -q "Running"; then
        log_success "性能测试通过"
    else
        log_error "性能测试失败"
    fi

    # 清理
    kubectl delete namespace load-test --ignore-not-found
}

# 压力测试
stress_test() {
    log_info "执行压力测试..."

    # 安装 stress 工具
    kubectl run stress-test --image=progrium/stress --namespace=default --restart=Never \
        -- --cpu 2 --io 1 --vm 2 --vm-bytes 128M --timeout 60s

    # 等待测试完成
    kubectl logs stress-test --follow

    # 清理
    kubectl delete pod stress-test --ignore-not-found
}

# 主函数
main() {
    echo "========================================"
    echo "    Kubernetes 集群验证"
    echo "========================================"
    echo ""

    check_nodes
    echo ""

    check_system_components
    echo ""

    check_ingress
    echo ""

    check_storage
    echo ""

    check_network_plugin
    echo ""

    check_monitoring
    echo ""

    check_service_mesh
    echo ""

    performance_test
    echo ""

    log_success "集群验证完成！"
}

main "$@"
```

---

## 📚 参考资料

1. [Kubernetes 官方文档](https://kubernetes.io/zh/docs/)
2. [Helm 官方文档](https://helm.sh/docs/)
3. [Kubernetes 最佳实践](https://kubernetes.io/zh/docs/concepts/configuration/manage-resources-containers/)
4. [ArgoCD 官方文档](https://argo-cd.readthedocs.io/)

---

## 📋 容器化实施检查清单

### Kubernetes 集群
- [ ] 集群节点准备
- [ ] Kubernetes 安装
- [ ] 网络插件部署
- [ ] Ingress 控制器配置
- [ ] 存储类配置
- [ ] 集群验证

### Helm Chart
- [ ] Chart 结构设计
- [ ] values.yaml 配置
- [ ] 模板编写
- [ ] 助手函数定义
- [ ] 依赖管理
- [ ] Chart 验证

### CI/CD 流水线
- [ ] 代码质量检查
- [ ] 单元测试集成
- [ ] Docker 构建
- [ ] 安全扫描
- [ ] Helm 打包
- [ ] 多环境部署
- [ ] 健康检查
- [ ] 回滚机制

### 监控运维
- [ ] Prometheus 配置
- [ ] Grafana 仪表盘
- [ ] 日志聚合
- [ ] 告警规则
- [ ] 性能测试
- [ ] 压力测试

---

**编制：** 浮浮酱 🐱（猫娘工程师）
**日期：** 2025-11-14
**状态：** 📋 指南完成，准备实施

**加油喵～ 容器化与编排即将完成！** ฅ'ω'ฅ
