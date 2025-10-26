#!/bin/bash
# 创建Flyway迁移脚本ConfigMap
# 用于Kubernetes InitContainer执行数据库迁移

set -e

GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 显示帮助
show_help() {
    cat << EOF
创建Flyway迁移脚本ConfigMap

用法: $0 [选项]

选项:
    -h, --help              显示帮助信息
    -n, --namespace NS      Kubernetes命名空间 (默认: basebackend)
    -d, --dry-run           仅显示配置，不实际创建

示例:
    # 创建ConfigMap到basebackend命名空间
    $0

    # 创建到指定命名空间
    $0 -n production

    # 预览配置
    $0 --dry-run
EOF
}

# 解析参数
NAMESPACE="basebackend"
DRY_RUN=""

while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            exit 0
            ;;
        -n|--namespace)
            NAMESPACE="$2"
            shift 2
            ;;
        -d|--dry-run)
            DRY_RUN="--dry-run=client"
            shift
            ;;
        *)
            log_error "未知选项: $1"
            show_help
            exit 1
            ;;
    esac
done

log_info "========================================"
log_info "创建Flyway迁移脚本ConfigMap"
log_info "========================================"
log_info "命名空间: $NAMESPACE"
if [ -n "$DRY_RUN" ]; then
    log_info "模式: Dry Run (预览)"
fi
log_info "========================================"

# 切换到项目根目录
cd "$(dirname "$0")/../.."

# 检查迁移脚本目录
MIGRATION_DIR="basebackend-admin-api/src/main/resources/db/migration"
if [ ! -d "$MIGRATION_DIR" ]; then
    log_error "迁移脚本目录不存在: $MIGRATION_DIR"
    exit 1
fi

# 检查是否有迁移脚本
if [ -z "$(ls -A "$MIGRATION_DIR"/*.sql 2>/dev/null)" ]; then
    log_error "迁移脚本目录为空: $MIGRATION_DIR"
    exit 1
fi

# 创建或更新ConfigMap
log_info "从目录创建ConfigMap: $MIGRATION_DIR"

kubectl create configmap flyway-migration-scripts \
    --from-file="$MIGRATION_DIR" \
    --namespace="$NAMESPACE" \
    --dry-run=client -o yaml | \
    kubectl apply -f - $DRY_RUN

if [ $? -eq 0 ]; then
    log_info "========================================"
    log_info "✅ ConfigMap创建成功"
    log_info "========================================"
    log_info "命令预览:"
    echo "  kubectl get configmap flyway-migration-scripts -n $NAMESPACE"
    echo "  kubectl describe configmap flyway-migration-scripts -n $NAMESPACE"
    log_info "========================================"
else
    log_error "ConfigMap创建失败"
    exit 1
fi

# 显示ConfigMap信息
if [ -z "$DRY_RUN" ]; then
    log_info ""
    log_info "ConfigMap内容:"
    kubectl get configmap flyway-migration-scripts -n "$NAMESPACE" -o yaml | head -20
fi
