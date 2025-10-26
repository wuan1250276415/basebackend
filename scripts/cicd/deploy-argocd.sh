#!/bin/bash
# 部署Argo CD到Kubernetes集群

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查kubectl是否安装
if ! command -v kubectl &> /dev/null; then
    log_error "kubectl未安装，请先安装kubectl"
    exit 1
fi

# 检查集群连接
if ! kubectl cluster-info &> /dev/null; then
    log_error "无法连接到Kubernetes集群"
    exit 1
fi

log_info "开始部署Argo CD..."

# 创建命名空间
log_info "创建argocd命名空间..."
kubectl create namespace argocd --dry-run=client -o yaml | kubectl apply -f -

# 安装Argo CD
log_info "安装Argo CD..."
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

# 等待Argo CD就绪
log_info "等待Argo CD Pod就绪..."
kubectl wait --for=condition=Ready pods --all -n argocd --timeout=300s

# 获取初始密码
log_info "获取Argo CD管理员密码..."
ARGOCD_PWD=$(kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d)

# 暴露Argo CD服务（可选）
log_info "配置Argo CD服务访问方式..."
cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: Service
metadata:
  name: argocd-server-nodeport
  namespace: argocd
spec:
  type: NodePort
  ports:
    - port: 80
      targetPort: 8080
      nodePort: 30080
      name: http
    - port: 443
      targetPort: 8080
      nodePort: 30443
      name: https
  selector:
    app.kubernetes.io/name: argocd-server
EOF

# 应用AppProject
log_info "创建Argo CD项目..."
kubectl apply -f k8s/argocd/project.yaml

log_info "Argo CD部署完成！"
echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}Argo CD访问信息:${NC}"
echo -e "URL: https://<your-cluster-ip>:30443"
echo -e "用户名: admin"
echo -e "密码: ${ARGOCD_PWD}"
echo -e "${BLUE}========================================${NC}"
echo ""
log_info "请保存以上密码，并建议立即修改！"
echo ""
log_info "后续步骤:"
echo "  1. 访问Argo CD Web UI"
echo "  2. 使用上面的凭证登录"
echo "  3. 应用Application定义:"
echo "     kubectl apply -f k8s/argocd/application-admin-api-dev.yaml"
echo "     kubectl apply -f k8s/argocd/application-admin-api-prod.yaml"
