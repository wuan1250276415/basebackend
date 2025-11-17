#!/bin/bash

# Nacos 启动脚本
# 需要先下载 Nacos 3.1.0

NACOS_HOME="/opt/nacos"
NACOS_VERSION="3.1.0"

echo "启动 Nacos 服务发现中心..."

# 检查 Nacos 是否已安装
if [ ! -d "$NACOS_HOME" ]; then
    echo "Nacos 未安装，请先下载并安装 Nacos $NACOS_VERSION"
    echo "下载地址: https://github.com/alibaba/nacos/releases/tag/$NACOS_VERSION"
    echo "安装步骤:"
    echo "1. 下载 nacos-server-$NACOS_VERSION.tar.gz"
    echo "2. 解压到 /opt/nacos"
    echo "3. 运行此脚本"
    exit 1
fi

# 启动 Nacos
cd $NACOS_HOME
./bin/startup.sh -m standalone

echo "Nacos 启动完成！"
echo "访问地址: http://localhost:8848/nacos"
echo "默认用户名/密码: nacos/nacos"
