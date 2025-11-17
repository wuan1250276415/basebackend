#!/bin/bash
# =====================================================================
# Nacos配置导入脚本 - 权限服务
# 创建时间: 2025-11-15
# 描述: 将权限服务配置导入到Nacos
# =====================================================================

set -e

echo "======================================="
echo "Nacos配置导入 - 权限服务"
echo "======================================="

# 配置变量
NACOS_SERVER="127.0.0.1:8848"
NAMESPACE="basebackend"
GROUP="DEFAULT_GROUP"
CONFIG_FILE="basebackend-auth-service.yml"

# 检查Nacos服务
echo "检查Nacos服务..."
if ! curl -f "http://${NACOS_SERVER}/nacos/v1/console/health/readiness" > /dev/null 2>&1; then
    echo "❌ Nacos服务不可用"
    echo "请先启动Nacos: cd nacos/bin && sh startup.sh -m standalone"
    exit 1
fi
echo "✅ Nacos服务正常"

# 读取配置文件内容
echo "读取配置文件: ${CONFIG_FILE}"
if [ ! -f "${CONFIG_FILE}" ]; then
    echo "❌ 配置文件不存在: ${CONFIG_FILE}"
    exit 1
fi

CONFIG_CONTENT=$(cat ${CONFIG_FILE})

# 上传到Nacos
echo "上传配置到Nacos..."
RESULT=$(curl -X POST "http://${NACOS_SERVER}/nacos/v1/cs/configs" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "tenant=${NAMESPACE}&groupId=${GROUP}&configId=${CONFIG_FILE}&content=${CONFIG_CONTENT}&appName=basebackend-auth-service&desc=权限服务配置")

echo "上传结果: ${RESULT}"

# 验证配置
echo "验证配置..."
sleep 2
QUERY_RESULT=$(curl -s "http://${NACOS_SERVER}/nacos/v1/cs/configs?tenant=${NAMESPACE}&groupId=${GROUP}&dataId=${CONFIG_FILE}")
if [ -n "$QUERY_RESULT" ]; then
    echo "✅ 配置上传成功"
    echo "配置信息:"
    echo "  命名空间: ${NAMESPACE}"
    echo "  分组: ${GROUP}"
    echo "  数据ID: ${CONFIG_FILE}"
    echo "  服务名称: basebackend-auth-service"
else
    echo "❌ 配置上传失败或验证失败"
    exit 1
fi

echo "======================================="
echo "Nacos配置导入完成!"
echo "======================================="
echo "可在Nacos控制台查看配置:"
echo "访问地址: http://${NACOS_SERVER}/nacos/index.html"
echo "======================================="
