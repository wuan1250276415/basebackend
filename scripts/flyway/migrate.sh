#!/bin/bash
# Flyway数据库迁移脚本
# 用于生产环境手动执行数据库迁移

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

# 显示帮助
show_help() {
    cat << EOF
Flyway数据库迁移脚本

用法: $0 [选项]

选项:
    -h, --help              显示帮助信息
    -u, --url URL           数据库URL (必需)
    -U, --user USER         数据库用户名 (必需)
    -p, --password PASS     数据库密码 (必需)
    -e, --env ENV           环境名称 (dev/test/staging/prod)
    -b, --backup            执行前自动备份数据库
    --dry-run               模拟执行，不实际修改数据库

示例:
    # 基本用法
    $0 -u jdbc:mysql://localhost:3306/basebackend_admin \\
       -U root -p password

    # 带备份
    $0 -u jdbc:mysql://localhost:3306/basebackend_admin \\
       -U root -p password -b

    # 使用环境变量
    export MYSQL_URL="jdbc:mysql://localhost:3306/basebackend_admin"
    export MYSQL_USER="root"
    export MYSQL_PASSWORD="password"
    $0
EOF
}

# 备份数据库
backup_database() {
    local db_name=$(echo $DB_URL | sed -n 's|.*://[^/]*/\([^?]*\).*|\1|p')
    local backup_file="backup_${db_name}_$(date +%Y%m%d_%H%M%S).sql"

    log_info "正在备份数据库到: $backup_file"

    mysqldump -h localhost -u $DB_USER -p$DB_PASSWORD $db_name > $backup_file

    if [ $? -eq 0 ]; then
        log_info "数据库备份完成: $backup_file"
    else
        log_error "数据库备份失败"
        exit 1
    fi
}

# 解析参数
DB_URL="${MYSQL_URL}"
DB_USER="${MYSQL_USER}"
DB_PASSWORD="${MYSQL_PASSWORD}"
ENV="prod"
DO_BACKUP=false
DRY_RUN=false

while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            exit 0
            ;;
        -u|--url)
            DB_URL="$2"
            shift 2
            ;;
        -U|--user)
            DB_USER="$2"
            shift 2
            ;;
        -p|--password)
            DB_PASSWORD="$2"
            shift 2
            ;;
        -e|--env)
            ENV="$2"
            shift 2
            ;;
        -b|--backup)
            DO_BACKUP=true
            shift
            ;;
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        *)
            log_error "未知选项: $1"
            show_help
            exit 1
            ;;
    esac
done

# 验证必需参数
if [ -z "$DB_URL" ] || [ -z "$DB_USER" ] || [ -z "$DB_PASSWORD" ]; then
    log_error "缺少必需参数"
    show_help
    exit 1
fi

log_info "========================================"
log_info "Flyway数据库迁移"
log_info "========================================"
log_info "数据库URL: $DB_URL"
log_info "用户名: $DB_USER"
log_info "环境: $ENV"
log_info "模拟执行: $DRY_RUN"
log_info "========================================"

# 确认执行
if [ "$DRY_RUN" = false ]; then
    read -p "确认执行迁移? (yes/no): " confirm
    if [ "$confirm" != "yes" ]; then
        log_warn "迁移已取消"
        exit 0
    fi
fi

# 备份数据库
if [ "$DO_BACKUP" = true ] && [ "$DRY_RUN" = false ]; then
    backup_database
fi

# 切换到项目根目录
cd "$(dirname "$0")/../.."

# 执行Flyway迁移
log_info "开始执行数据库迁移..."

FLYWAY_CMD="mvn flyway:migrate \
    -Dflyway.url=$DB_URL \
    -Dflyway.user=$DB_USER \
    -Dflyway.password=$DB_PASSWORD \
    -pl basebackend-admin-api"

if [ "$DRY_RUN" = true ]; then
    log_info "模拟执行命令:"
    echo "$FLYWAY_CMD"
    log_info "使用 --dry-run=false 实际执行迁移"
else
    eval $FLYWAY_CMD

    if [ $? -eq 0 ]; then
        log_info "========================================"
        log_info "✅ 数据库迁移成功完成！"
        log_info "========================================"

        # 显示迁移历史
        log_info "查看迁移历史..."
        mvn flyway:info \
            -Dflyway.url=$DB_URL \
            -Dflyway.user=$DB_USER \
            -Dflyway.password=$DB_PASSWORD \
            -pl basebackend-admin-api
    else
        log_error "========================================"
        log_error "❌ 数据库迁移失败"
        log_error "========================================"
        exit 1
    fi
fi
