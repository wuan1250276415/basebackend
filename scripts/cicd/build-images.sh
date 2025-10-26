#!/bin/bash
# 本地构建Docker镜像脚本

set -e

# 配置
REGISTRY="${DOCKER_REGISTRY:-docker.io}"
USERNAME="${DOCKER_USERNAME:-YOUR_USERNAME}"
VERSION="${VERSION:-$(git rev-parse --short HEAD)}"

# 服务列表
SERVICES=("gateway" "admin-api" "demo-api" "file-service")

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 显示帮助
show_help() {
    cat << EOF
本地Docker镜像构建脚本

用法: $0 [选项] [服务...]

选项:
    -h, --help          显示帮助信息
    -v, --version TAG   指定镜像版本标签 (默认: git短commit)
    -p, --push          构建后推送到镜像仓库
    -r, --registry URL  镜像仓库地址 (默认: docker.io)
    -u, --username      镜像仓库用户名
    --no-cache          不使用缓存构建

服务:
    gateway, admin-api, demo-api, file-service
    留空则构建所有服务

示例:
    # 构建所有服务
    $0

    # 构建指定服务
    $0 admin-api gateway

    # 构建并推送到Docker Hub
    $0 -p -u myusername admin-api

    # 指定版本号构建
    $0 -v v1.0.0 admin-api
EOF
}

# 解析参数
PUSH=false
NO_CACHE=""
BUILD_SERVICES=()

while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            exit 0
            ;;
        -v|--version)
            VERSION="$2"
            shift 2
            ;;
        -p|--push)
            PUSH=true
            shift
            ;;
        -r|--registry)
            REGISTRY="$2"
            shift 2
            ;;
        -u|--username)
            USERNAME="$2"
            shift 2
            ;;
        --no-cache)
            NO_CACHE="--no-cache"
            shift
            ;;
        *)
            BUILD_SERVICES+=("$1")
            shift
            ;;
    esac
done

# 如果没有指定服务，构建所有服务
if [ ${#BUILD_SERVICES[@]} -eq 0 ]; then
    BUILD_SERVICES=("${SERVICES[@]}")
fi

log_info "开始构建Docker镜像"
log_info "版本: $VERSION"
log_info "仓库: $REGISTRY/$USERNAME"
log_info "服务: ${BUILD_SERVICES[*]}"

# 构建镜像
for service in "${BUILD_SERVICES[@]}"; do
    # 映射服务名到模块名
    case "$service" in
        "gateway")
            module="basebackend-gateway"
            port="8080"
            ;;
        "admin-api")
            module="basebackend-admin-api"
            port="8082"
            ;;
        "demo-api")
            module="basebackend-demo-api"
            port="8081"
            ;;
        "file-service")
            module="basebackend-file-service"
            port="8083"
            ;;
        *)
            log_error "未知服务: $service"
            continue
            ;;
    esac

    image_name="$REGISTRY/$USERNAME/basebackend-$service:$VERSION"

    log_info "构建 $service ($module)..."

    # 使用Maven构建
    log_info "  编译Java代码..."
    mvn clean package -pl $module -am -DskipTests -B -q

    # 构建Docker镜像
    log_info "  构建Docker镜像: $image_name"
    docker build $NO_CACHE -t $image_name -f $module/Dockerfile .

    # 打标签
    docker tag $image_name "$REGISTRY/$USERNAME/basebackend-$service:latest"

    log_info "  ✓ $service 构建完成"

    # 推送镜像
    if [ "$PUSH" = true ]; then
        log_info "  推送镜像到仓库..."
        docker push $image_name
        docker push "$REGISTRY/$USERNAME/basebackend-$service:latest"
        log_info "  ✓ $service 推送完成"
    fi
done

log_info "所有镜像构建完成！"

# 显示镜像列表
log_info "\n构建的镜像:"
docker images | grep "basebackend-" | grep "$VERSION"
