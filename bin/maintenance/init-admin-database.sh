#!/bin/bash

echo "初始化后台管理数据库..."

# 数据库配置
DB_HOST="localhost"
DB_PORT="3306"
DB_NAME="basebackend_admin"
DB_USER="root"

echo "请确保MySQL服务正在运行，并且已创建数据库 $DB_NAME"
echo "如果数据库不存在，请先执行以下命令："
echo "mysql -u root -p -e \"CREATE DATABASE IF NOT EXISTS $DB_NAME CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;\""
echo ""

# 执行SQL脚本
echo "执行数据库初始化脚本..."
mysql -h $DB_HOST -P $DB_PORT -u $DB_USER -p $DB_NAME < basebackend-admin-api/src/main/resources/db/schema.sql

if [ $? -eq 0 ]; then
    echo "数据库表创建成功"
else
    echo "数据库表创建失败"
    exit 1
fi

echo "插入初始数据..."
mysql -h $DB_HOST -P $DB_PORT -u $DB_USER -p $DB_NAME < basebackend-admin-api/src/main/resources/db/data.sql

if [ $? -eq 0 ]; then
    echo "初始数据插入成功"
    echo "数据库初始化完成！"
    echo ""
    echo "默认管理员账号："
    echo "用户名: admin"
    echo "密码: admin123"
else
    echo "初始数据插入失败"
    exit 1
fi
