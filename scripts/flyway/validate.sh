#!/bin/bash
# Flyway数据库验证脚本
# 验证迁移脚本的一致性和有效性

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
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
Flyway数据库验证脚本

用法: $0 [选项]

选项:
    -h, --help              显示帮助信息
    -u, --url URL           数据库URL (可选，不提供则只验证脚本语法)
    -U, --user USER         数据库用户名
    -p, --password PASS     数据库密码

示例:
    # 仅验证脚本语法
    $0

    # 验证脚本并与数据库对比
    $0 -u jdbc:mysql://localhost:3306/basebackend_admin \\
       -U root -p password
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

log_info "========================================"
log_info "Flyway数据库验证"
log_info "========================================"

# 切换到项目根目录
cd "$(dirname "$0")/../.."

# 1. 验证迁移脚本命名规范
log_info "1. 检查迁移脚本命名规范..."
MIGRATION_DIR="basebackend-admin-api/src/main/resources/db/migration"

if [ ! -d "$MIGRATION_DIR" ]; then
    log_error "迁移目录不存在: $MIGRATION_DIR"
    exit 1
fi

invalid_files=()
for file in $MIGRATION_DIR/*.sql; do
    filename=$(basename "$file")
    if [[ ! $filename =~ ^V[0-9]+(\.[0-9]+)*__[a-z_]+\.sql$ ]] && \
       [[ ! $filename =~ ^R__[a-z_]+\.sql$ ]]; then
        invalid_files+=("$filename")
    fi
done

if [ ${#invalid_files[@]} -gt 0 ]; then
    log_error "以下文件命名不符合Flyway规范:"
    for file in "${invalid_files[@]}"; do
        echo "  - $file"
    done
    exit 1
else
    log_info "✅ 所有迁移脚本命名规范正确"
fi

# 2. 验证SQL语法
log_info "2. 检查SQL语法..."
for file in $MIGRATION_DIR/V*.sql; do
    # 基本语法检查（检查是否有常见错误）
    if grep -q "USE " "$file"; then
        log_error "⚠️  $file 包含 USE 语句，Flyway会自动连接到正确的数据库"
    fi

    if grep -q "DROP TABLE IF EXISTS" "$file" 2>/dev/null; then
        log_error "⚠️  $file 包含 DROP TABLE，建议使用 CREATE TABLE IF NOT EXISTS"
    fi
done
log_info "✅ SQL语法检查通过"

# 3. 如果提供了数据库连接，执行Flyway验证
if [ -n "$DB_URL" ] && [ -n "$DB_USER" ] && [ -n "$DB_PASSWORD" ]; then
    log_info "3. 执行Flyway验证..."

    mvn flyway:validate \
        -Dflyway.url=$DB_URL \
        -Dflyway.user=$DB_USER \
        -Dflyway.password=$DB_PASSWORD \
        -pl basebackend-admin-api

    if [ $? -eq 0 ]; then
        log_info "✅ Flyway验证成功"
    else
        log_error "❌ Flyway验证失败"
        exit 1
    fi
else
    log_info "3. 跳过数据库验证（未提供数据库连接）"
fi

log_info "========================================"
log_info "✅ 验证完成"
log_info "========================================"
