#!/bin/bash
# =====================================================================
# 用户服务Nacos配置导入脚本
# 创建时间: 2025-11-15
# 描述: 导入用户服务相关配置到Nacos
# =====================================================================

set -e

# 配置变量
NACOS_ADDR=${NACOS_ADDR:-"localhost:8848"}
NAMESPACE=${NAMESPACE:-"basebackend"}
GROUP=${GROUP:-"DEFAULT_GROUP"}
USERNAME=${NACOS_USERNAME:-"nacos"}
PASSWORD=${NACOS_PASSWORD:-"nacos"}

echo "======================================="
echo "用户服务Nacos配置导入"
echo "======================================="
echo "Nacos地址: ${NACOS_ADDR}"
echo "命名空间: ${NAMESPACE}"
echo "分组: ${GROUP}"

# 检查nacos-cli工具
if ! command -v nacos-cli &> /dev/null; then
    echo "错误: 请先安装nacos-cli工具"
    echo "安装方法: npm install -g nacos-cli"
    exit 1
fi

# 导入用户服务配置
echo "导入用户服务应用配置..."
nacos-cli config import \
    --nacos-addr=${NACOS_ADDR} \
    --username=${USERNAME} \
    --password=${PASSWORD} \
    --tenant=${NAMESPACE} \
    --group=${GROUP} \
    --data-id=basebackend-user-service.yml \
    --data-type=file \
    --data=basebackend-user-service-config.yml

echo "导入用户服务路由配置..."
nacos-cli config import \
    --nacos-addr=${NACOS_ADDR} \
    --username=${USERNAME} \
    --password=${PASSWORD} \
    --tenant=${NAMESPACE} \
    --group=${GROUP} \
    --data-id=basebackend-gateway-routes.yml \
    --data-type=file \
    --data=user-service-routes.yml

echo "导入Sentinel限流规则..."
nacos-cli config import \
    --nacos-addr=${NACOS_ADDR} \
    --username=${USERNAME} \
    --password=${PASSWORD} \
    --tenant=${NAMESPACE} \
    --group=SENTINEL_GROUP \
    --data-id=basebackend-gateway-flow-rules \
    --data-type=json \
    --data=basebackend-gateway-flow-rules.json

echo "======================================="
echo "配置导入完成！"
echo "======================================="
echo "请访问 http://${NACOS_ADDR}/nacos 查看配置"
