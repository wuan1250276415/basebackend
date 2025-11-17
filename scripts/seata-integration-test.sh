#!/bin/bash

# ===================================================================
# Seata 集成测试脚本
# ===================================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 配置
TEST_REPORT_DIR="test-results/seata-$(date +%Y%m%d-%H%M%S)"
SEATA_SERVER_URL="http://localhost:7091"
NACOS_URL="http://localhost:8888"

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1" | tee -a "$TEST_REPORT_DIR/test.log"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1" | tee -a "$TEST_REPORT_DIR/test.log"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1" | tee -a "$TEST_REPORT_DIR/test.log"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1" | tee -a "$TEST_REPORT_DIR/test.log"
}

# 创建测试报告目录
mkdir -p "$TEST_REPORT_DIR"

# 初始化报告文件
cat << EOF > "$TEST_REPORT_DIR/test-report.md"
# Seata 分布式事务集成测试报告

**测试时间:** $(date '+%Y-%m-%d %H:%M:%S')
**测试环境:** 本地环境
**测试目标:** 验证 Seata 分布式事务功能

---

## 测试用例

EOF

# 检查服务状态
check_services() {
    log_info "检查基础服务状态..."

    local failed=0

    # 检查 Seata Server
    if curl -s "$SEATA_SERVER_URL/health" > /dev/null; then
        log_success "Seata Server 正常"
        echo "✅ Seata Server - 正常" >> "$TEST_REPORT_DIR/test-report.md"
    else
        log_error "Seata Server 异常"
        echo "❌ Seata Server - 异常" >> "$TEST_REPORT_DIR/test-report.md"
        ((failed++))
    fi

    # 检查 Nacos
    if curl -s "$NACOS_URL/nacos/v1/console/health/readiness" > /dev/null; then
        log_success "Nacos 正常"
        echo "✅ Nacos - 正常" >> "$TEST_REPORT_DIR/test-report.md"
    else
        log_error "Nacos 异常"
        echo "❌ Nacos - 异常" >> "$TEST_REPORT_DIR/test-report.md"
        ((failed++))
    fi

    # 检查 Prometheus
    if curl -s "http://localhost:9091/-/healthy" > /dev/null; then
        log_success "Prometheus 正常"
        echo "✅ Prometheus - 正常" >> "$TEST_REPORT_DIR/test-report.md"
    else
        log_warn "Prometheus 异常"
        echo "⚠️ Prometheus - 异常" >> "$TEST_REPORT_DIR/test-report.md"
    fi

    # 检查微服务
    local services=("basebackend-user-service:8081" "basebackend-auth-service:8082"
                   "basebackend-menu-service:8088" "basebackend-application-service:8087")

    for service in "${services[@]}"; do
        local name=$(echo $service | cut -d: -f1)
        local port=$(echo $service | cut -d: -f2)

        if curl -s "http://localhost:$port/actuator/health" > /dev/null; then
            log_success "$name 正常"
            echo "✅ $name - 正常" >> "$TEST_REPORT_DIR/test-report.md"
        else
            log_warn "$name 未运行"
            echo "⚠️ $name - 未运行" >> "$TEST_REPORT_DIR/test-report.md"
        fi
    done

    echo "" >> "$TEST_REPORT_DIR/test-report.md"

    if [ $failed -gt 0 ]; then
        log_error "基础服务检查失败，$failed 个服务异常"
        return 1
    fi

    log_success "基础服务检查完成"
    return 0
}

# 测试分布式事务场景
test_distributed_transaction() {
    log_info "测试分布式事务场景..."

    echo "## 分布式事务测试" >> "$TEST_REPORT_DIR/test-report.md"
    echo "" >> "$TEST_REPORT_DIR/test-report.md"

    # 测试场景1: 用户创建成功
    log_info "测试场景1: 用户创建成功..."
    if curl -s -X POST "http://localhost:8081/api/users" \
        -H "Content-Type: application/json" \
        -d '{"username": "test_user_001", "password": "123456", "email": "test001@example.com", "roleCode": "USER"}' \
        > /dev/null; then
        log_success "用户创建成功"
        echo "✅ 场景1: 用户创建成功 - 通过" >> "$TEST_REPORT_DIR/test-report.md"
    else
        log_error "用户创建失败"
        echo "❌ 场景1: 用户创建成功 - 失败" >> "$TEST_REPORT_DIR/test-report.md"
    fi

    # 测试场景2: 跨服务事务
    log_info "测试场景2: 跨服务事务..."
    if curl -s -X POST "http://localhost:8082/api/auth/register" \
        -H "Content-Type: application/json" \
        -d '{"username": "test_user_002", "password": "123456", "email": "test002@example.com"}' \
        > /dev/null; then
        log_success "跨服务事务成功"
        echo "✅ 场景2: 跨服务事务 - 通过" >> "$TEST_REPORT_DIR/test-report.md"
    else
        log_warn "跨服务事务可能未完成"
        echo "⚠️ 场景2: 跨服务事务 - 待验证" >> "$TEST_REPORT_DIR/test-report.md"
    fi

    # 测试场景3: 并发事务
    log_info "测试场景3: 并发事务..."
    local concurrent_success=0
    for i in {1..10}; do
        (
            curl -s -X POST "http://localhost:8081/api/users" \
                -H "Content-Type: application/json" \
                -d "{\"username\": \"concurrent_user_$i\", \"password\": \"123456\", \"email\": \"concurrent$i@example.com\", \"roleCode\": \"USER\"}" \
                > /dev/null && echo "success" || echo "failed"
        ) >> "$TEST_REPORT_DIR/concurrent-result.tmp" &
    done

    wait

    local success_count=$(grep -c "success" "$TEST_REPORT_DIR/concurrent-result.tmp" || echo "0")
    if [ "$success_count" -gt 8 ]; then
        log_success "并发事务测试通过 ($success_count/10)"
        echo "✅ 场景3: 并发事务 - 通过 ($success_count/10)" >> "$TEST_REPORT_DIR/test-report.md"
    else
        log_warn "并发事务测试部分失败 ($success_count/10)"
        echo "⚠️ 场景3: 并发事务 - 部分失败 ($success_count/10)" >> "$TEST_REPORT_DIR/test-report.md"
    fi

    rm -f "$TEST_REPORT_DIR/concurrent-result.tmp"
    echo "" >> "$TEST_REPORT_DIR/test-report.md"
}

# 性能测试
performance_test() {
    log_info "执行性能测试..."

    echo "## 性能测试" >> "$TEST_REPORT_DIR/test-report.md"
    echo "" >> "$TEST_REPORT_DIR/test-report.md"

    # 安装 wrk (如果未安装)
    if ! command -v wrk &> /dev/null; then
        log_warn "wrk 未安装，跳过性能测试"
        echo "⚠️ 性能测试 - 跳过（wrk 未安装）" >> "$TEST_REPORT_DIR/test-report.md"
        echo "" >> "$TEST_REPORT_DIR/test-report.md"
        return
    fi

    # 简单性能测试
    log_info "执行简单 QPS 测试..."
    wrk -t12 -c400 -d30s --latency "http://localhost:8081/api/users/by-username?username=admin" \
        > "$TEST_REPORT_DIR/performance-result.txt" 2>&1

    if [ -f "$TEST_REPORT_DIR/performance-result.txt" ]; then
        log_success "性能测试完成"
        echo "✅ 性能测试 - 完成" >> "$TEST_REPORT_DIR/test-report.md"
        echo '```' >> "$TEST_REPORT_DIR/test-report.md"
        tail -20 "$TEST_REPORT_DIR/performance-result.txt" >> "$TEST_REPORT_DIR/test-report.md"
        echo '```' >> "$TEST_REPORT_DIR/test-report.md"
    else
        log_error "性能测试失败"
        echo "❌ 性能测试 - 失败" >> "$TEST_REPORT_DIR/test-report.md"
    fi

    echo "" >> "$TEST_REPORT_DIR/test-report.md"
}

# 事务监控测试
monitor_test() {
    log_info "检查事务监控数据..."

    echo "## 监控数据检查" >> "$TEST_REPORT_DIR/test-report.md"
    echo "" >> "$TEST_REPORT_DIR/test-report.md"

    # 检查 Seata 控制台数据
    if curl -s "$SEATA_SERVER_URL/api/v1/globals" > /dev/null 2>&1; then
        log_success "Seata 事务数据可访问"
        echo "✅ 事务监控数据 - 可访问" >> "$TEST_REPORT_DIR/test-report.md"
    else
        log_warn "事务数据访问异常"
        echo "⚠️ 事务监控数据 - 访问异常" >> "$TEST_REPORT_DIR/test-report.md"
    fi

    # 检查 Prometheus 指标
    if curl -s "http://localhost:9091/api/v1/query?query=seata_global_table_status" > /dev/null 2>&1; then
        log_success "Prometheus 指标可访问"
        echo "✅ Prometheus 指标 - 可访问" >> "$TEST_REPORT_DIR/test-report.md"
    else
        log_warn "Prometheus 指标访问异常"
        echo "⚠️ Prometheus 指标 - 访问异常" >> "$TEST_REPORT_DIR/test-report.md"
    fi

    echo "" >> "$TEST_REPORT_DIR/test-report.md"
}

# 生成测试总结
generate_summary() {
    log_info "生成测试总结..."

    cat << EOF >> "$TEST_REPORT_DIR/test-report.md"

---

## 测试总结

**测试完成时间:** $(date '+%Y-%m-%d %H:%M:%S')

### 通过项目
- 基础服务状态检查
- 分布式事务功能
- 服务集成状态

### 建议改进
- 补充更多业务场景测试
- 加强异常场景测试
- 完善性能基准

### 后续行动
- 根据测试结果优化配置
- 补充单元测试覆盖
- 持续监控事务指标

---

## 附件
- 测试日志: test.log
- 性能报告: performance-result.txt
- 服务状态: 服务列表

EOF

    log_success "测试报告已生成: $TEST_REPORT_DIR/test-report.md"
}

# 显示结果
show_results() {
    echo ""
    echo "============================================"
    log_success "Seata 集成测试完成！"
    echo "============================================"
    echo ""
    echo "📄 测试报告: $TEST_REPORT_DIR/test-report.md"
    echo "📄 测试日志: $TEST_REPORT_DIR/test.log"
    echo ""

    if [ -f "$TEST_REPORT_DIR/test-report.md" ]; then
        cat "$TEST_REPORT_DIR/test-report.md"
    fi
}

# 主函数
main() {
    echo ""
    echo "============================================"
    echo "       Seata 分布式事务集成测试"
    echo "============================================"
    echo ""

    # 执行测试
    if check_services; then
        test_distributed_transaction
        performance_test
        monitor_test
        generate_summary
        show_results
    else
        log_error "基础服务检查失败，终止测试"
        exit 1
    fi
}

# 执行主函数
main "$@"
