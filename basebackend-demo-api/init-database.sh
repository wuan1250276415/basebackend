#!/bin/bash

# 数据库初始化脚本
# 使用方法: ./init-database.sh

echo "======================================"
echo "BaseBackend Demo 数据库初始化"
echo "======================================"
echo ""

# 数据库配置
DB_HOST="localhost"
DB_PORT="3306"
DB_USER="root"
DB_PASSWORD="mysql_S4deB5"
DB_NAME="basebackend_demo"

# 检查MySQL是否安装
if ! command -v mysql &> /dev/null; then
    echo "错误: 未检测到MySQL客户端，请先安装MySQL"
    echo ""
    echo "Ubuntu/Debian: sudo apt-get install mysql-client"
    echo "CentOS/RHEL: sudo yum install mysql"
    echo "macOS: brew install mysql-client"
    exit 1
fi

# 检查SQL文件是否存在
SQL_FILE="$(dirname "$0")/src/main/resources/db/schema.sql"
if [ ! -f "$SQL_FILE" ]; then
    echo "错误: 找不到SQL文件: $SQL_FILE"
    exit 1
fi

echo "配置信息:"
echo "  数据库主机: $DB_HOST:$DB_PORT"
echo "  数据库用户: $DB_USER"
echo "  数据库名称: $DB_NAME"
echo ""

# 测试连接
echo "正在测试数据库连接..."
if mysql -h $DB_HOST -P $DB_PORT -u $DB_USER -p$DB_PASSWORD -e "SELECT 1" &> /dev/null; then
    echo "✓ 数据库连接成功"
else
    echo "✗ 数据库连接失败"
    echo ""
    echo "请检查:"
    echo "  1. MySQL服务是否已启动"
    echo "  2. 数据库主机和端口是否正确"
    echo "  3. 用户名和密码是否正确"
    echo ""
    echo "如果使用Docker启动MySQL:"
    echo "  docker run -d --name mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=$DB_PASSWORD mysql:8.0"
    exit 1
fi

echo ""
echo "正在执行SQL脚本..."
echo "======================================"

# 执行SQL脚本
mysql -h $DB_HOST -P $DB_PORT -u $DB_USER -p$DB_PASSWORD < "$SQL_FILE"

if [ $? -eq 0 ]; then
    echo "======================================"
    echo "✓ 数据库初始化成功!"
    echo ""
    echo "已创建:"
    echo "  - 数据库: $DB_NAME"
    echo "  - 表: demo_user (用户表)"
    echo "  - 表: demo_article (文章表)"
    echo ""
    echo "测试数据:"
    echo "  - 5个测试用户"
    echo "  - 10篇测试文章"
    echo ""
    echo "现在可以启动Demo API了:"
    echo "  cd basebackend-demo-api"
    echo "  mvn spring-boot:run"
else
    echo "======================================"
    echo "✗ 数据库初始化失败"
    echo ""
    echo "请检查SQL脚本中的错误"
    exit 1
fi
