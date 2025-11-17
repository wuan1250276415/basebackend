#!/bin/bash

# ===================================================================
# Profile Service 数据库初始化脚本
# ===================================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 配置
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-123456}"
DB_NAME="basebackend_profile"
SQL_FILE="basebackend-profile-service/src/main/resources/db/migration/V1__init_profile_service.sql"

# 日志函数
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

# 检查 MySQL 连接
check_mysql() {
    log_info "检查 MySQL 连接..."

    if mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASSWORD" -e "SELECT 1;" > /dev/null 2>&1; then
        log_success "MySQL 连接成功"
        return 0
    else
        log_error "MySQL 连接失败"
        echo "请检查："
        echo "  1. MySQL 是否启动"
        echo "  2. 用户名密码是否正确"
        echo "  3. 主机和端口是否正确"
        return 1
    fi
}

# 检查 SQL 文件
check_sql_file() {
    log_info "检查 SQL 文件..."

    if [ -f "$SQL_FILE" ]; then
        log_success "找到 SQL 文件: $SQL_FILE"
        return 0
    else
        log_error "SQL 文件不存在: $SQL_FILE"
        return 1
    fi
}

# 创建数据库和表
init_database() {
    log_info "初始化数据库: $DB_NAME"

    # 执行 SQL 脚本
    if mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASSWORD" < "$SQL_FILE"; then
        log_success "数据库初始化成功"
        return 0
    else
        log_error "数据库初始化失败"
        return 1
    fi
}

# 验证数据库
verify_database() {
    log_info "验证数据库..."

    # 检查数据库是否存在
    db_exists=$(mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASSWORD" -e "SHOW DATABASES LIKE '$DB_NAME';" | grep -c "$DB_NAME" || echo "0")

    if [ "$db_exists" -eq "0" ]; then
        log_error "数据库 $DB_NAME 不存在"
        return 1
    fi

    # 检查表是否存在
    table_exists=$(mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASSWORD" -D"$DB_NAME" -e "SHOW TABLES LIKE 'user_preference';" | grep -c "user_preference" || echo "0")

    if [ "$table_exists" -eq "0" ]; then
        log_error "表 user_preference 不存在"
        return 1
    fi

    # 检查表结构
    log_info "检查表结构..."
    mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASSWORD" -D"$DB_NAME" -e "DESCRIBE user_preference;"

    # 检查索引
    log_info "检查索引..."
    mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASSWORD" -D"$DB_NAME" -e "SHOW INDEX FROM user_preference;"

    log_success "数据库验证完成"
    return 0
}

# 插入测试数据
insert_test_data() {
    log_info "插入测试数据..."

    mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" << EOF
-- 插入用户偏好设置（用户ID: 1）
INSERT INTO user_preference (
    user_id,
    theme,
    language,
    timezone,
    email_notification,
    system_notification,
    page_size
) VALUES (
    1,
    'light',
    'zh-CN',
    'Asia/Shanghai',
    1,
    1,
    10
);

-- 查看插入的数据
SELECT * FROM user_preference WHERE user_id = 1;
EOF

    log_success "测试数据插入完成"
}

# 显示使用指南
show_usage() {
    echo ""
    echo "==================================================================="
    echo "              Profile Service 数据库初始化"
    echo "==================================================================="
    echo ""
    echo "配置选项："
    echo "  DB_HOST     MySQL 主机 (默认: localhost)"
    echo "  DB_PORT     MySQL 端口 (默认: 3306)"
    echo "  DB_USER     MySQL 用户 (默认: root)"
    echo "  DB_PASSWORD MySQL 密码 (默认: 123456)"
    echo ""
    echo "示例："
    echo "  # 使用默认配置"
    echo "  ./scripts/init-profile-database.sh"
    echo ""
    echo "  # 指定配置"
    echo "  DB_HOST=192.168.1.100 DB_USER=admin DB_PASSWORD=secret ./scripts/init-profile-database.sh"
    echo ""
    echo "==================================================================="
    echo ""
}

# 主函数
main() {
    show_usage

    # 检查参数
    if [ "$1" == "-h" ] || [ "$1" == "--help" ]; then
        return 0
    fi

    echo "开始初始化数据库..."
    echo ""

    # 检查先决条件
    check_mysql || exit 1
    check_sql_file || exit 1

    # 初始化数据库
    init_database || exit 1

    # 验证数据库
    verify_database || exit 1

    # 插入测试数据（可选）
    read -p "是否插入测试数据？ (y/N): " -n 1 -r
    echo ""
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        insert_test_data
    fi

    echo ""
    echo "==================================================================="
    log_success "数据库初始化完成！"
    echo "==================================================================="
    echo ""
    echo "数据库信息："
    echo "  数据库名: $DB_NAME"
    echo "  表名: user_preference"
    echo "  字符集: utf8mb4"
    echo "  排序规则: utf8mb4_general_ci"
    echo ""
    echo "后续步骤："
    echo "  1. 启动 profile-service"
    echo "  2. 运行集成测试"
    echo "  3. 验证 API 调用"
    echo ""
}

# 运行主函数
main "$@"
