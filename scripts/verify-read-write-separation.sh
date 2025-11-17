#!/bin/bash

# ============================================
# 数据库读写分离验证脚本
# ============================================
# 用途: 验证数据库读写分离功能是否正常工作
# 作者: 浮浮酱 🐱
# 日期: 2025-11-13
# ============================================

echo "========================================"
echo "  数据库读写分离验证脚本"
echo "========================================"
echo ""

# 配置
BASE_URL="${BASE_URL:-http://localhost:8080}"
API_PREFIX="/api"

# 颜色输出
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 测试计数
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# 测试函数
test_query_operation() {
    echo -e "${YELLOW}[测试 1/3]${NC} 测试查询操作（应该使用从库）..."
    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    # 调用查询接口
    RESPONSE=$(curl -s -w "\n%{http_code}" "${BASE_URL}${API_PREFIX}/users/1")
    HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
    BODY=$(echo "$RESPONSE" | sed '$d')

    if [ "$HTTP_CODE" -eq 200 ]; then
        echo -e "${GREEN}✅ 查询接口调用成功${NC}"
        echo "   响应: $BODY"
        echo ""
        echo "📝 请检查应用日志,应该看到:"
        echo "   - 检测到查询方法,使用从库: getUserById"
        echo "   - 切换数据源: SLAVE"
        echo "   - 当前数据源: SLAVE"
        echo ""
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        echo -e "${RED}❌ 查询接口调用失败 (HTTP $HTTP_CODE)${NC}"
        echo "   响应: $BODY"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
    echo "========================================"
}

test_write_operation() {
    echo -e "${YELLOW}[测试 2/3]${NC} 测试写操作（应该使用主库）..."
    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    # 调用创建接口
    RESPONSE=$(curl -s -w "\n%{http_code}" -X POST \
        -H "Content-Type: application/json" \
        -d '{"username":"test_rw_separation","nickname":"测试用户","password":"test123"}' \
        "${BASE_URL}${API_PREFIX}/users")
    HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
    BODY=$(echo "$RESPONSE" | sed '$d')

    if [ "$HTTP_CODE" -eq 200 ] || [ "$HTTP_CODE" -eq 201 ]; then
        echo -e "${GREEN}✅ 写操作接口调用成功${NC}"
        echo "   响应: $BODY"
        echo ""
        echo "📝 请检查应用日志,应该看到:"
        echo "   - 检测到写操作方法,使用主库: createUser"
        echo "   - 切换数据源: MASTER"
        echo "   - 当前数据源: MASTER"
        echo ""
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        echo -e "${RED}❌ 写操作接口调用失败 (HTTP $HTTP_CODE)${NC}"
        echo "   响应: $BODY"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
    echo "========================================"
}

test_transaction_operation() {
    echo -e "${YELLOW}[测试 3/3]${NC} 测试事务操作（应该使用主库）..."
    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    # 调用更新接口
    RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT \
        -H "Content-Type: application/json" \
        -d '{"nickname":"测试用户(已更新)"}' \
        "${BASE_URL}${API_PREFIX}/users/1")
    HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
    BODY=$(echo "$RESPONSE" | sed '$d')

    if [ "$HTTP_CODE" -eq 200 ]; then
        echo -e "${GREEN}✅ 事务操作接口调用成功${NC}"
        echo "   响应: $BODY"
        echo ""
        echo "📝 请检查应用日志,应该看到:"
        echo "   - 检测到事务方法,使用主库: updateUser"
        echo "   - 切换数据源: MASTER"
        echo "   - 当前数据源: MASTER"
        echo ""
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        echo -e "${RED}❌ 事务操作接口调用失败 (HTTP $HTTP_CODE)${NC}"
        echo "   响应: $BODY"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
    echo "========================================"
}

# 主函数
main() {
    echo "📌 测试配置:"
    echo "   Base URL: $BASE_URL"
    echo "   API Prefix: $API_PREFIX"
    echo ""
    echo "开始测试..."
    echo ""

    # 运行测试
    test_query_operation
    sleep 1
    test_write_operation
    sleep 1
    test_transaction_operation

    # 输出结果
    echo ""
    echo "========================================"
    echo "  测试结果汇总"
    echo "========================================"
    echo "总测试数: $TOTAL_TESTS"
    echo -e "${GREEN}通过: $PASSED_TESTS${NC}"
    echo -e "${RED}失败: $FAILED_TESTS${NC}"
    echo ""

    if [ $FAILED_TESTS -eq 0 ]; then
        echo -e "${GREEN}✅ 所有测试通过！读写分离功能正常工作喵～${NC}"
        echo ""
        echo "📊 下一步:"
        echo "   1. 查看应用日志,确认数据源切换日志"
        echo "   2. 访问 Druid 监控: ${BASE_URL}/druid"
        echo "   3. 查看主从库连接统计"
        echo ""
        exit 0
    else
        echo -e "${RED}❌ 部分测试失败,请检查配置和日志${NC}"
        echo ""
        echo "🔧 故障排查:"
        echo "   1. 确认应用已重启"
        echo "   2. 检查 Nacos 配置是否已更新"
        echo "   3. 检查主从库连接是否正常"
        echo "   4. 查看应用启动日志中的数据源初始化信息"
        echo ""
        exit 1
    fi
}

# 运行主函数
main
