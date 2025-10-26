#!/bin/bash
# Flyway数据库迁移历史查询脚本

set -e

# 颜色输出
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

# 显示帮助
show_help() {
    cat << EOF
Flyway数据库迁移历史查询脚本

用法: $0 [选项]

选项:
    -h, --help              显示帮助信息
    -u, --url URL           数据库URL (必需)
    -U, --user USER         数据库用户名 (必需)
    -p, --password PASS     数据库密码 (必需)

示例:
    $0 -u jdbc:mysql://localhost:3306/basebackend_admin \\
       -U root -p password

    # 使用环境变量
    export MYSQL_URL="jdbc:mysql://localhost:3306/basebackend_admin"
    export MYSQL_USER="root"
    export MYSQL_PASSWORD="password"
    $0
EOF
}

# 解析参数
DB_URL="${MYSQL_URL}"
DB_USER="${MYSQL_USER}"
DB_PASSWORD="${MYSQL_PASSWORD}"

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
log_info "Flyway迁移历史"
log_info "========================================"
log_info "数据库: $DB_URL"
log_info "========================================"

# 切换到项目根目录
cd "$(dirname "$0")/../.."

# 执行Flyway info
mvn flyway:info \
    -Dflyway.url=$DB_URL \
    -Dflyway.user=$DB_USER \
    -Dflyway.password=$DB_PASSWORD \
    -pl basebackend-admin-api

log_info "========================================"
log_info "说明:"
log_info "  Success  - 已成功应用的迁移"
log_info "  Pending  - 待执行的迁移"
log_info "  Missing  - 数据库中记录但文件缺失"
log_info "  Ignored  - 被忽略的迁移"
log_info "  Failed   - 失败的迁移"
log_info "========================================"
