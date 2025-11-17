#!/bin/bash

# 项目初始化脚本
# 用于首次设置项目环境

echo "========================================="
echo "  Base Backend 项目初始化"
echo "========================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 检查前置条件
echo -e "${BLUE}1. 检查前置条件${NC}"
echo "-------------------"

check_command() {
    local cmd=$1
    local name=$2
    local version_cmd=$3
    
    echo -n "检查 $name ... "
    if command -v "$cmd" &> /dev/null; then
        version=$($version_cmd 2>&1 | head -n1)
        echo -e "${GREEN}✓ 已安装${NC} ($version)"
        return 0
    else
        echo -e "${RED}✗ 未安装${NC}"
        return 1
    fi
}

all_ok=true

check_command "docker" "Docker" "docker --version" || all_ok=false
check_command "docker-compose" "Docker Compose" "docker-compose --version" || all_ok=false
check_command "java" "Java" "java -version" || all_ok=false
check_command "mvn" "Maven" "mvn --version" || all_ok=false
check_command "git" "Git" "git --version" || all_ok=false

echo ""

if [ "$all_ok" = false ]; then
    echo -e "${RED}✗ 缺少必要的工具，请先安装。${NC}"
    echo ""
    echo "安装指南:"
    echo "  Docker: https://docs.docker.com/get-docker/"
    echo "  Java 17: https://adoptium.net/"
    echo "  Maven: https://maven.apache.org/download.cgi"
    exit 1
fi

# 2. 创建必要的目录
echo -e "${BLUE}2. 创建项目目录${NC}"
echo "-------------------"

directories=(
    "logs"
    "data/mysql"
    "data/redis"
    "data/nacos"
)

for dir in "${directories[@]}"; do
    echo -n "创建 $dir ... "
    if mkdir -p "$dir" 2>/dev/null; then
        echo -e "${GREEN}✓${NC}"
    else
        echo -e "${YELLOW}⚠ 已存在${NC}"
    fi
done

echo ""

# 3. 配置环境变量
echo -e "${BLUE}3. 配置环境变量${NC}"
echo "-------------------"

if [ ! -f "docker/compose/env/.env.dev" ]; then
    echo -n "创建开发环境配置 ... "
    if [ -f "docker/compose/env/.env.example" ]; then
        cp docker/compose/env/.env.example docker/compose/env/.env.dev
        echo -e "${GREEN}✓${NC}"
    else
        echo -e "${RED}✗ 模板文件不存在${NC}"
    fi
else
    echo -e "开发环境配置 ... ${YELLOW}⚠ 已存在${NC}"
fi

echo ""

# 4. 配置 Maven
echo -e "${BLUE}4. 配置 Maven${NC}"
echo "-------------------"

maven_settings="$HOME/.m2/settings.xml"
echo -n "检查 Maven 配置 ... "

if [ -f "$maven_settings" ]; then
    if grep -q "aliyun" "$maven_settings"; then
        echo -e "${GREEN}✓ 已配置阿里云镜像${NC}"
    else
        echo -e "${YELLOW}⚠ 未配置镜像${NC}"
        echo ""
        echo "建议配置阿里云镜像以加速依赖下载："
        echo "  编辑 $maven_settings"
        echo "  添加阿里云镜像配置"
        echo "  参考: docs/development/getting-started.md"
    fi
else
    echo -e "${YELLOW}⚠ 配置文件不存在${NC}"
    echo ""
    echo "建议创建 Maven 配置文件："
    echo "  mkdir -p ~/.m2"
    echo "  参考: docs/development/getting-started.md"
fi

echo ""

# 5. 下载 Maven 依赖
echo -e "${BLUE}5. 下载 Maven 依赖${NC}"
echo "-------------------"

echo "开始下载项目依赖（这可能需要几分钟）..."
if mvn dependency:resolve -q; then
    echo -e "${GREEN}✓ 依赖下载完成${NC}"
else
    echo -e "${RED}✗ 依赖下载失败${NC}"
    echo "请检查网络连接或 Maven 配置"
fi

echo ""

# 6. 启动基础设施
echo -e "${BLUE}6. 启动基础设施${NC}"
echo "-------------------"

read -p "是否启动 Docker 基础设施？(y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "启动 Docker 服务..."
    cd docker/compose
    ./start-all.sh
    cd ../..
    echo -e "${GREEN}✓ 基础设施启动完成${NC}"
else
    echo "跳过基础设施启动"
fi

echo ""

# 7. 导入 Nacos 配置
echo -e "${BLUE}7. 导入 Nacos 配置${NC}"
echo "-------------------"

read -p "是否导入 Nacos 配置？(y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "等待 Nacos 启动（60秒）..."
    sleep 60
    
    echo "导入配置..."
    cd config/nacos-configs
    if [ -f "import-nacos-configs.sh" ]; then
        ./import-nacos-configs.sh
        echo -e "${GREEN}✓ 配置导入完成${NC}"
    else
        echo -e "${RED}✗ 导入脚本不存在${NC}"
    fi
    cd ../..
else
    echo "跳过配置导入"
fi

echo ""

# 8. 完成
echo "========================================="
echo "  初始化完成"
echo "========================================="
echo ""
echo -e "${GREEN}✓ 项目初始化成功！${NC}"
echo ""
echo "下一步："
echo "  1. 编译项目: mvn clean install -DskipTests"
echo "  2. 启动 Gateway: cd basebackend-gateway && mvn spring-boot:run"
echo "  3. 启动 Admin API: cd basebackend-admin-api && mvn spring-boot:run"
echo "  4. 访问 API 文档: http://localhost:8080/doc.html"
echo ""
echo "更多信息请查看: QUICKSTART.md"
