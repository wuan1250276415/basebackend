#!/bin/bash

# BaseBackend Kubernetes部署脚本
# 使用方法: ./deploy.sh [environment] [action]
# 环境: dev, test, prod
# 操作: install, upgrade, rollback, delete

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 配置变量
ENVIRONMENT=${1:-dev}
ACTION=${2:-install}
NAMESPACE="basebackend"
HELM_RELEASE_NAME="basebackend-admin-api"
HELM_CHART_PATH="../helm/basebackend-admin-api"

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查依赖
check_dependencies() {
    log_info "检查依赖..."

    if ! command -v kubectl &> /dev/null; then
        log_error "kubectl 未安装"
        exit 1
    fi

    if ! command -v helm &> /dev/null; then
        log_error "Helm 未安装"
        exit 1
    fi

    log_success "依赖检查通过"
}

# 检查集群连接
check_cluster() {
    log_info "检查集群连接..."

    if ! kubectl cluster-info &> /dev/null; then
        log_error "无法连接到Kubernetes集群"
        exit 1
    fi

    log_success "集群连接正常"
}

# 创建命名空间
create_namespace() {
    log_info "创建命名空间: $NAMESPACE"

    kubectl create namespace $NAMESPACE --dry-run=client -o yaml | kubectl apply -f -

    log_success "命名空间创建完成"
}

# 部署基础设施
deploy_infrastructure() {
    log_info "部署基础设施组件..."

    # 部署MySQL
    log_info "部署MySQL..."
    kubectl apply -f ../storage/mysql/mysql-deployment.yaml

    # 部署Redis
    log_info "部署Redis..."
    kubectl apply -f ../storage/redis/redis-cluster.yaml

    # 等待基础设施就绪
    log_info "等待基础设施就绪..."
    kubectl wait --for=condition=available --timeout=600s deployment/mysql -n mysql || log_warning "MySQL部署可能存在问题"
    kubectl wait --for=condition=available --timeout=300s deployment/redis-master -n redis || log_warning "Redis部署可能存在问题"

    log_success "基础设施部署完成"
}

# 部署服务网格
deploy_service_mesh() {
    log_info "部署Istio服务网格..."

    # 检查Istio是否已安装
    if ! kubectl get namespace istio-system &> /dev/null; then
        log_warning "Istio未安装，跳过服务网格部署"
        return
    fi

    kubectl apply -f ../istio/gateway.yaml
    kubectl apply -f ../istio/virtualservice.yaml
    kubectl apply -f ../istio/destinationrule.yaml

    log_success "服务网格部署完成"
}

# 部署应用
deploy_application() {
    log_info "部署应用服务..."

    # 更新Helm仓库
    helm repo update

    # 安装或升级应用
    if [ "$ACTION" = "install" ]; then
        log_info "安装应用: $HELM_RELEASE_NAME"
        helm install $HELM_RELEASE_NAME $HELM_CHART_PATH \
            --namespace $NAMESPACE \
            --create-namespace \
            --set replicaCount=3 \
            --set image.tag=latest \
            --set environment=$ENVIRONMENT \
            --wait --timeout=600s
    elif [ "$ACTION" = "upgrade" ]; then
        log_info "升级应用: $HELM_RELEASE_NAME"
        helm upgrade $HELM_RELEASE_NAME $HELM_CHART_PATH \
            --namespace $NAMESPACE \
            --set image.tag=latest \
            --set environment=$ENVIRONMENT \
            --wait --timeout=600s
    fi

    log_success "应用部署完成"
}

# 部署监控
deploy_monitoring() {
    log_info "部署监控组件..."

    if [ "$ENVIRONMENT" != "dev" ]; then
        kubectl apply -f ../monitoring/prometheus/prometheus-deployment.yaml

        # 等待Prometheus就绪
        kubectl wait --for=condition=available --timeout=300s deployment/prometheus -n monitoring || log_warning "Prometheus部署可能存在问题"

        log_success "监控组件部署完成"
    else
        log_info "开发环境跳过监控部署"
    fi
}

# 检查部署状态
check_deployment_status() {
    log_info "检查部署状态..."

    kubectl get pods -n $NAMESPACE

    # 等待Pod就绪
    kubectl wait --for=condition=Ready pod -l app=basebackend-admin-api -n $NAMESPACE --timeout=600s

    log_success "部署状态检查通过"
}

# 执行回滚
rollback_deployment() {
    log_info "执行回滚..."

    # 列出历史版本
    helm history $HELM_RELEASE_NAME -n $NAMESPACE

    # 回滚到上一个版本
    helm rollback $HELM_RELEASE_NAME -n $NAMESPACE

    # 等待回滚完成
    kubectl rollout status deployment/basebackend-admin-api -n $NAMESPACE --timeout=300s

    log_success "回滚完成"
}

# 删除部署
delete_deployment() {
    log_warning "删除部署..."

    read -p "确定要删除部署吗？ (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        helm uninstall $HELM_RELEASE_NAME -n $NAMESPACE || true
        kubectl delete namespace $NAMESPACE || true
        log_success "部署删除完成"
    else
        log_info "取消删除操作"
    fi
}

# 显示使用帮助
show_help() {
    echo "BaseBackend Kubernetes部署脚本"
    echo ""
    echo "使用方法:"
    echo "  $0 [environment] [action]"
    echo ""
    echo "环境:"
    echo "  dev    - 开发环境"
    echo "  test   - 测试环境"
    echo "  prod   - 生产环境"
    echo ""
    echo "操作:"
    echo "  install   - 安装应用"
    echo "  upgrade   - 升级应用"
    echo "  rollback  - 回滚应用"
    echo "  delete    - 删除应用"
    echo ""
    echo "示例:"
    echo "  $0 prod install"
    echo "  $0 prod upgrade"
    echo "  $0 prod rollback"
}

# 主函数
main() {
    echo "================================================"
    echo "  BaseBackend Kubernetes 部署脚本"
    echo "  环境: $ENVIRONMENT"
    echo "  操作: $ACTION"
    echo "================================================"
    echo

    # 显示帮助
    if [ "$ACTION" = "help" ] || [ "$ACTION" = "--help" ] || [ "$ACTION" = "-h" ]; then
        show_help
        exit 0
    fi

    # 参数验证
    if [[ ! "$ENVIRONMENT" =~ ^(dev|test|prod)$ ]]; then
        log_error "无效的环境: $ENVIRONMENT"
        show_help
        exit 1
    fi

    if [[ ! "$ACTION" =~ ^(install|upgrade|rollback|delete)$ ]]; then
        log_error "无效的操作: $ACTION"
        show_help
        exit 1
    fi

    # 执行操作
    case $ACTION in
        install)
            check_dependencies
            check_cluster
            create_namespace
            deploy_infrastructure
            deploy_service_mesh
            deploy_application
            deploy_monitoring
            check_deployment_status
            log_success "部署完成！"
            ;;
        upgrade)
            check_dependencies
            check_cluster
            deploy_application
            check_deployment_status
            log_success "升级完成！"
            ;;
        rollback)
            check_dependencies
            check_cluster
            rollback_deployment
            log_success "回滚完成！"
            ;;
        delete)
            delete_deployment
            ;;
    esac

    echo
    echo "================================================"
    echo "  部署脚本执行完成"
    echo "================================================"
}

# 执行主函数
main "$@"
