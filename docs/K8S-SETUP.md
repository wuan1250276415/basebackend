# Kubernetes集群搭建指南

本文档提供从零搭建Kubernetes集群用于BaseBackend项目部署的完整步骤。

## 📋 目录

- [方案选择](#方案选择)
- [本地开发环境](#本地开发环境)
- [云环境部署](#云环境部署)
- [集群初始化](#集群初始化)
- [必需组件安装](#必需组件安装)

## 🎯 方案选择

### 方案对比

| 方案 | 适用场景 | 优点 | 缺点 |
|------|---------|------|------|
| kind | 本地开发测试 | 快速、轻量 | 仅限本地 |
| minikube | 本地开发 | 易用、功能全 | 资源占用高 |
| k3s | 生产环境 | 轻量、易部署 | 功能略简化 |
| kubeadm | 生产环境 | 标准、灵活 | 配置复杂 |
| 托管K8s | 生产环境 | 免运维、高可用 | 成本较高 |

### 推荐方案

- **本地开发**: kind 或 minikube
- **测试环境**: k3s
- **生产环境**: 云厂商托管K8s（EKS/GKE/AKS）或 kubeadm

## 💻 本地开发环境

### 方式1: 使用kind

kind (Kubernetes in Docker) 是最快捷的本地K8s方案。

#### 安装kind

```bash
# macOS
brew install kind

# Linux
curl -Lo ./kind https://kind.sigs.k8s.io/dl/v0.20.0/kind-linux-amd64
chmod +x ./kind
sudo mv ./kind /usr/local/bin/kind

# Windows
choco install kind
```

#### 创建集群

```bash
# 创建集群配置文件
cat <<EOF > kind-config.yaml
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
name: basebackend
nodes:
  - role: control-plane
    kubeadmConfigPatches:
      - |
        kind: InitConfiguration
        nodeRegistration:
          kubeletExtraArgs:
            node-labels: "ingress-ready=true"
    extraPortMappings:
      - containerPort: 80
        hostPort: 80
        protocol: TCP
      - containerPort: 443
        hostPort: 443
        protocol: TCP
      - containerPort: 30080  # Argo CD HTTP
        hostPort: 30080
        protocol: TCP
      - containerPort: 30443  # Argo CD HTTPS
        hostPort: 30443
        protocol: TCP
  - role: worker
  - role: worker
EOF

# 创建集群
kind create cluster --config kind-config.yaml

# 验证
kubectl cluster-info
kubectl get nodes
```

#### 删除集群

```bash
kind delete cluster --name basebackend
```

### 方式2: 使用minikube

#### 安装minikube

```bash
# macOS
brew install minikube

# Linux
curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
sudo install minikube-linux-amd64 /usr/local/bin/minikube

# Windows
choco install minikube
```

#### 启动集群

```bash
# 启动集群（推荐4核8G内存）
minikube start \
  --cpus=4 \
  --memory=8192 \
  --disk-size=50g \
  --kubernetes-version=v1.28.0 \
  --driver=docker

# 启用插件
minikube addons enable ingress
minikube addons enable metrics-server
minikube addons enable dashboard

# 访问Dashboard
minikube dashboard

# 停止集群
minikube stop

# 删除集群
minikube delete
```

## ☁️ 云环境部署

### AWS EKS

```bash
# 安装eksctl
brew install eksctl

# 创建集群
eksctl create cluster \
  --name basebackend-cluster \
  --region us-west-2 \
  --nodegroup-name standard-workers \
  --node-type t3.medium \
  --nodes 3 \
  --nodes-min 2 \
  --nodes-max 4 \
  --managed

# 配置kubectl
aws eks update-kubeconfig --region us-west-2 --name basebackend-cluster
```

### 阿里云ACK

```bash
# 使用阿里云控制台创建集群
# https://cs.console.aliyun.com/

# 或使用aliyun CLI
aliyun cs POST /clusters --body "$(cat <<EOF
{
  "name": "basebackend-cluster",
  "cluster_type": "ManagedKubernetes",
  "region_id": "cn-hangzhou",
  "vpcid": "vpc-xxx",
  "vswitch_ids": ["vsw-xxx"],
  "num_of_nodes": 3,
  "worker_instance_types": ["ecs.c6.large"]
}
EOF
)"

# 获取kubeconfig
aliyun cs GET /k8s/<cluster-id>/user_config > kubeconfig
export KUBECONFIG=./kubeconfig
```

### 使用k3s（轻量级生产环境）

```bash
# 在服务器上安装k3s
curl -sfL https://get.k3s.io | sh -

# 获取kubeconfig
sudo cat /etc/rancher/k3s/k3s.yaml

# 在本地使用
# 1. 复制kubeconfig内容
# 2. 替换server地址为实际IP
# 3. 保存到本地 ~/.kube/config
```

## 🔧 集群初始化

### 创建命名空间

```bash
# 创建应用命名空间
kubectl create namespace basebackend-dev
kubectl create namespace basebackend-test
kubectl create namespace basebackend-staging
kubectl create namespace basebackend-prod

# 打标签
kubectl label namespace basebackend-dev environment=dev
kubectl label namespace basebackend-test environment=test
kubectl label namespace basebackend-staging environment=staging
kubectl label namespace basebackend-prod environment=prod
```

### 配置资源限制

```bash
# 为每个命名空间配置ResourceQuota
cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: ResourceQuota
metadata:
  name: compute-quota
  namespace: basebackend-prod
spec:
  hard:
    requests.cpu: "10"
    requests.memory: 20Gi
    limits.cpu: "20"
    limits.memory: 40Gi
    persistentvolumeclaims: "10"
EOF
```

### 配置网络策略（可选）

```bash
# 禁止命名空间间通信
cat <<EOF | kubectl apply -f -
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: deny-from-other-namespaces
  namespace: basebackend-prod
spec:
  podSelector: {}
  policyTypes:
    - Ingress
  ingress:
    - from:
        - podSelector: {}
EOF
```

## 📦 必需组件安装

### 1. Metrics Server（必需）

```bash
# 安装Metrics Server（HPA需要）
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml

# 验证
kubectl top nodes
kubectl top pods -A
```

### 2. Ingress Controller

#### NGINX Ingress

```bash
# 安装
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.9.4/deploy/static/provider/cloud/deploy.yaml

# 等待就绪
kubectl wait --namespace ingress-nginx \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=120s

# 验证
kubectl get pods -n ingress-nginx
kubectl get svc -n ingress-nginx
```

### 3. cert-manager（HTTPS证书）

```bash
# 安装
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.2/cert-manager.yaml

# 验证
kubectl get pods -n cert-manager

# 配置Let's Encrypt
cat <<EOF | kubectl apply -f -
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: your-email@example.com
    privateKeySecretRef:
      name: letsencrypt-prod
    solvers:
      - http01:
          ingress:
            class: nginx
EOF
```

### 4. 存储类（如需要持久化存储）

#### 使用local-path-provisioner（本地开发）

```bash
kubectl apply -f https://raw.githubusercontent.com/rancher/local-path-provisioner/v0.0.24/deploy/local-path-storage.yaml

# 设为默认存储类
kubectl patch storageclass local-path -p '{"metadata": {"annotations":{"storageclass.kubernetes.io/is-default-class":"true"}}}'
```

#### 云环境使用云厂商存储类

```bash
# AWS EBS
# 已默认配置

# 阿里云云盘
# 已默认配置

# 验证
kubectl get storageclass
```

### 5. 监控栈（可选但推荐）

#### Prometheus + Grafana

```bash
# 添加Helm仓库
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

# 安装kube-prometheus-stack
helm install prometheus prometheus-community/kube-prometheus-stack \
  --namespace monitoring \
  --create-namespace \
  --set grafana.adminPassword=admin

# 访问Grafana
kubectl port-forward -n monitoring svc/prometheus-grafana 3000:80

# 访问 http://localhost:3000
# 用户名: admin
# 密码: admin
```

## ✅ 验证集群

```bash
# 检查节点
kubectl get nodes

# 检查系统Pod
kubectl get pods -A

# 检查存储类
kubectl get storageclass

# 检查Metrics
kubectl top nodes

# 运行测试Pod
kubectl run test --image=nginx --restart=Never
kubectl get pod test
kubectl delete pod test
```

## 🔐 安全加固

### 配置RBAC

```bash
# 创建只读用户
cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: ServiceAccount
metadata:
  name: developer
  namespace: basebackend-dev
---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: developer-binding
  namespace: basebackend-dev
subjects:
  - kind: ServiceAccount
    name: developer
    namespace: basebackend-dev
roleRef:
  kind: ClusterRole
  name: view
  apiGroup: rbac.authorization.k8s.io
EOF
```

### 配置Pod安全策略

```bash
# 启用Pod Security Admission
cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: Namespace
metadata:
  name: basebackend-prod
  labels:
    pod-security.kubernetes.io/enforce: restricted
    pod-security.kubernetes.io/audit: restricted
    pod-security.kubernetes.io/warn: restricted
EOF
```

## 📊 集群规模建议

### 开发环境
- **节点**: 1-2个
- **配置**: 2核4G/节点
- **总资源**: 4核8G

### 测试环境
- **节点**: 2-3个
- **配置**: 4核8G/节点
- **总资源**: 8-12核16-24G

### 生产环境
- **节点**: 3-5个（建议奇数）
- **配置**: 8核16G/节点
- **总资源**: 24-40核48-80G

## 🐛 常见问题

### Q: kubectl连接超时

```bash
# 检查kubeconfig
kubectl config view

# 检查集群连接
kubectl cluster-info

# 切换context
kubectl config use-context <context-name>
```

### Q: Pod一直处于Pending状态

```bash
# 查看详细信息
kubectl describe pod <pod-name>

# 常见原因
1. 资源不足 → 增加节点或调整资源请求
2. 存储类不存在 → 安装存储提供者
3. 节点污点 → 添加容忍度
```

### Q: 服务无法访问

```bash
# 检查服务
kubectl get svc
kubectl describe svc <svc-name>

# 检查端点
kubectl get endpoints

# 检查网络策略
kubectl get networkpolicy
```

## 📚 参考资料

- [Kubernetes官方文档](https://kubernetes.io/docs/)
- [kind文档](https://kind.sigs.k8s.io/)
- [minikube文档](https://minikube.sigs.k8s.io/)
- [k3s文档](https://k3s.io/)

## 🎓 下一步

集群搭建完成后，继续：
1. [部署Argo CD](./CI-CD-GUIDE.md#argo-cd配置)
2. [配置GitOps](./CI-CD-GUIDE.md#gitops部署)
3. [部署应用](./CI-CD-GUIDE.md#快速开始)
