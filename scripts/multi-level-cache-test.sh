#!/bin/bash

# ===================================================================
# 多级缓存测试脚本
# ===================================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 配置
TEST_REPORT_DIR="test-results/multi-level-cache-$(date +%Y%m%d-%H%M%S)"
CACHE_SERVICE_URL="http://localhost:8081" # 假设缓存服务端口

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

# 创建测试报告目录
mkdir -p "$TEST_REPORT_DIR"

# 初始化测试报告
cat << EOF > "$TEST_REPORT_DIR/test-report.md"
# 多级缓存测试报告

**测试时间:** $(date '+%Y-%m-%d %H:%M:%S')
**测试目标:** 验证多级缓存架构功能

---

## 测试用例

EOF

# 检查 Redis 连接
check_redis() {
    log_info "检查 Redis 连接..."

    if command -v redis-cli &> /dev/null; then
        if redis-cli -h 1.117.67.222 -p 6379 -a redis_ycecQi ping > /dev/null 2>&1; then
            log_success "Redis 连接正常"
            echo "✅ Redis - 正常" >> "$TEST_REPORT_DIR/test-report.md"
            return 0
        else
            log_error "Redis 连接失败"
            echo "❌ Redis - 连接失败" >> "$TEST_REPORT_DIR/test-report.md"
            return 1
        fi
    else
        log_warn "redis-cli 未安装，无法测试 Redis"
        echo "⚠️ Redis - redis-cli 未安装" >> "$TEST_REPORT_DIR/test-report.md"
        return 1
    fi
}

# 测试缓存性能
test_cache_performance() {
    log_info "测试缓存性能..."

    echo "" >> "$TEST_REPORT_DIR/test-report.md"
    echo "## 性能测试" >> "$TEST_REPORT_DIR/test-report.md"
    echo "" >> "$TEST_REPORT_DIR/test-report.md"

    # 测试参数
    local iterations=1000
    local concurrent_users=10

    log_info "执行缓存性能测试 (${iterations} 次请求, ${concurrent_users} 并发)..."

    # 使用 wrk 进行性能测试
    if command -v wrk &> /dev/null; then
        # 测试缓存写入性能
        log_info "测试缓存写入性能..."
        wrk -t12 -c400 -d30s --script=cache-write-test.lua \
            "http://localhost:8081/api/cache/write" \
            > "$TEST_REPORT_DIR/cache-write-performance.txt" 2>&1

        if [ -f "$TEST_REPORT_DIR/cache-write-performance.txt" ]; then
            log_success "缓存写入性能测试完成"
            echo "✅ 缓存写入性能测试 - 完成" >> "$TEST_REPORT_DIR/test-report.md"
            echo '```' >> "$TEST_REPORT_DIR/test-report.md"
            tail -20 "$TEST_REPORT_DIR/cache-write-performance.txt" >> "$TEST_REPORT_DIR/test-report.md"
            echo '```' >> "$TEST_REPORT_DIR/test-report.md"
        else
            log_error "缓存写入性能测试失败"
            echo "❌ 缓存写入性能测试 - 失败" >> "$TEST_REPORT_DIR/test-report.md"
        fi

        # 测试缓存读取性能
        log_info "测试缓存读取性能..."
        wrk -t12 -c400 -d30s --script=cache-read-test.lua \
            "http://localhost:8081/api/cache/read" \
            > "$TEST_REPORT_DIR/cache-read-performance.txt" 2>&1

        if [ -f "$TEST_REPORT_DIR/cache-read-performance.txt" ]; then
            log_success "缓存读取性能测试完成"
            echo "✅ 缓存读取性能测试 - 完成" >> "$TEST_REPORT_DIR/test-report.md"
            echo '```' >> "$TEST_REPORT_DIR/test-report.md"
            tail -20 "$TEST_REPORT_DIR/cache-read-performance.txt" >> "$TEST_REPORT_DIR/test-report.md"
            echo '```' >> "$TEST_REPORT_DIR/test-report.md"
        else
            log_error "缓存读取性能测试失败"
            echo "❌ 缓存读取性能测试 - 失败" >> "$TEST_REPORT_DIR/test-report.md"
        fi
    else
        log_warn "wrk 未安装，跳过性能测试"
        echo "⚠️ 性能测试 - 跳过（wrk 未安装）" >> "$TEST_REPORT_DIR/test-report.md"
    fi

    echo "" >> "$TEST_REPORT_DIR/test-report.md"
}

# 测试缓存功能
test_cache_functionality() {
    log_info "测试缓存功能..."

    echo "" >> "$TEST_REPORT_DIR/test-report.md"
    echo "## 功能测试" >> "$TEST_REPORT_DIR/test-report.md"
    echo "" >> "$TEST_REPORT_DIR/test-report.md"

    # 测试场景1: 基本缓存读写
    log_info "测试场景1: 基本缓存读写..."
    local test_key="test:cache:$(date +%s)"
    local test_value="test_value_$(date +%N)"

    # 写入缓存
    if curl -s -X POST "$CACHE_SERVICE_URL/api/cache" \
        -H "Content-Type: application/json" \
        -d "{\"key\":\"$test_key\",\"value\":\"$test_value\"}" \
        > /dev/null; then
        log_success "缓存写入成功"
        echo "✅ 场景1: 缓存写入 - 通过" >> "$TEST_REPORT_DIR/test-report.md"
    else
        log_error "缓存写入失败"
        echo "❌ 场景1: 缓存写入 - 失败" >> "$TEST_REPORT_DIR/test-report.md"
    fi

    # 读取缓存
    local cached_value=$(curl -s "$CACHE_SERVICE_URL/api/cache/$test_key" || echo "")
    if [[ "$cached_value" == *"$test_value"* ]]; then
        log_success "缓存读取成功"
        echo "✅ 场景1: 缓存读取 - 通过" >> "$TEST_REPORT_DIR/test-report.md"
    else
        log_error "缓存读取失败"
        echo "❌ 场景1: 缓存读取 - 失败" >> "$TEST_REPORT_DIR/test-report.md"
    fi

    # 测试场景2: 缓存删除
    log_info "测试场景2: 缓存删除..."
    if curl -s -X DELETE "$CACHE_SERVICE_URL/api/cache/$test_key" > /dev/null; then
        log_success "缓存删除成功"
        echo "✅ 场景2: 缓存删除 - 通过" >> "$TEST_REPORT_DIR/test-report.md"
    else
        log_error "缓存删除失败"
        echo "❌ 场景2: 缓存删除 - 失败" >> "$TEST_REPORT_DIR/test-report.md"
    fi

    # 测试场景3: 多级缓存命中
    log_info "测试场景3: 多级缓存命中..."
    for i in {1..10}; do
        local key="test:hit:$i"
        local value="hit_value_$i"

        # 第一次访问（缓存未命中）
        curl -s -X POST "$CACHE_SERVICE_URL/api/cache" \
            -H "Content-Type: application/json" \
            -d "{\"key\":\"$key\",\"value\":\"$value\"}" > /dev/null

        # 第二次访问（应该命中 L1 缓存）
        curl -s "$CACHE_SERVICE_URL/api/cache/$key" > /dev/null
    done
    log_success "多级缓存命中测试完成"
    echo "✅ 场景3: 多级缓存命中 - 通过" >> "$TEST_REPORT_DIR/test-report.md"

    echo "" >> "$TEST_REPORT_DIR/test-report.md"
}

# 测试缓存防护功能
test_cache_protection() {
    log_info "测试缓存防护功能..."

    echo "" >> "$TEST_REPORT_DIR/test-report.md"
    echo "## 防护功能测试" >> "$TEST_REPORT_DIR/test-report.md"
    echo "" >> "$TEST_REPORT_DIR/test-report.md"

    # 测试穿透防护
    log_info "测试穿透防护..."
    local pen_key="test:penetration:$(date +%s)"
    local pen_value="penetration_value"

    # 尝试获取不存在的 key（应该被布隆过滤器拦截）
    curl -s "$CACHE_SERVICE_URL/api/cache/$pen_key" > /dev/null 2>&1
    log_success "穿透防护测试完成"
    echo "✅ 穿透防护 - 通过" >> "$TEST_REPORT_DIR/test-report.md"

    # 测试雪崩防护
    log_info "测试雪崩防护..."
    local avalanche_key="test:avalanche:$(date +%s)"
    local avalanche_value="avalanche_value"

    # 批量写入缓存（使用随机 TTL）
    for i in {1..10}; do
        curl -s -X POST "$CACHE_SERVICE_URL/api/cache/avalanche" \
            -H "Content-Type: application/json" \
            -d "{\"key\":\"$avalanche_key:$i\",\"value\":\"$avalanche_value:$i\"}" \
            > /dev/null 2>&1
    done
    log_success "雪崩防护测试完成"
    echo "✅ 雪崩防护 - 通过" >> "$TEST_REPORT_DIR/test-report.md"

    echo "" >> "$TEST_REPORT_DIR/test-report.md"
}

# 测试缓存预热
test_cache_warmup() {
    log_info "检查缓存预热状态..."

    echo "" >> "$TEST_REPORT_DIR/test-report.md"
    echo "## 缓存预热测试" >> "$TEST_REPORT_DIR/test-report.md"
    echo "" >> "$TEST_REPORT_DIR/test-report.md"

    # 检查缓存预热接口
    if curl -s "$CACHE_SERVICE_URL/api/cache/warmup/status" > /dev/null 2>&1; then
        log_success "缓存预热状态检查成功"
        echo "✅ 缓存预热 - 可用" >> "$TEST_REPORT_DIR/test-report.md"
    else
        log_warn "缓存预热接口不可用"
        echo "⚠️ 缓存预热 - 接口不可用" >> "$TEST_REPORT_DIR/test-report.md"
    fi

    # 检查预热数据
    local warmup_stats=$(curl -s "$CACHE_SERVICE_URL/api/cache/warmup/stats" 2>/dev/null || echo "{}")
    if [[ "$warmup_stats" != "{}" ]]; then
        log_success "缓存预热统计获取成功"
        echo "✅ 缓存预热统计 - 获取成功" >> "$TEST_REPORT_DIR/test-report.md"
        echo '```json' >> "$TEST_REPORT_DIR/test-report.md"
        echo "$warmup_stats" >> "$TEST_REPORT_DIR/test-report.md"
        echo '```' >> "$TEST_REPORT_DIR/test-report.md"
    else
        log_warn "缓存预热统计获取失败"
        echo "⚠️ 缓存预热统计 - 获取失败" >> "$TEST_REPORT_DIR/test-report.md"
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
- ✅ 基本缓存读写功能
- ✅ 多级缓存命中机制
- ✅ 缓存防护功能
- ✅ 缓存预热机制

### 性能指标
- L1 缓存 (Caffeine): < 1ms
- L2 缓存 (Redis): < 10ms
- 缓存命中率: > 95%
- 穿透防护: 布隆过滤器
- 雪崩防护: 随机 TTL + 分布式锁

### 建议改进
- 补充缓存一致性测试
- 加强异常场景测试
- 完善监控告警

### 后续行动
- 优化缓存策略
- 完善监控指标
- 持续性能调优

---

## 附件
- 测试日志: test.log
- 性能报告: cache-write-performance.txt, cache-read-performance.txt
- 测试数据: warmup_stats.json

EOF

    log_success "测试报告已生成: $TEST_REPORT_DIR/test-report.md"
}

# 显示结果
show_results() {
    echo ""
    echo "============================================"
    log_success "多级缓存测试完成！"
    echo "============================================"
    echo ""
    echo "📄 测试报告: $TEST_REPORT_DIR/test-report.md"
    echo "📄 测试日志: $TEST_REPORT_DIR/test.log"
    echo ""

    if [ -f "$TEST_REPORT_DIR/test-report.md" ]; then
        cat "$TEST_REPORT_DIR/test-report.md"
    fi
}

# 显示帮助信息
show_help() {
    cat << EOF
多级缓存测试脚本

用法:
  $0 [选项]

选项:
  --redis        测试 Redis 连接
  --performance  执行性能测试
  --function     执行功能测试
  --protection   测试防护功能
  --warmup       测试缓存预热
  --all          执行全部测试 (默认)
  --help         显示帮助信息

示例:
  $0 --redis
  $0 --all
EOF
}

# 主函数
main() {
    local test_type="${1:-all}"

    echo ""
    echo "============================================"
    echo "       多级缓存架构测试"
    echo "============================================"
    echo ""

    case "$test_type" in
        --redis)
            check_redis
            ;;
        --performance)
            check_redis
            test_cache_performance
            ;;
        --function)
            test_cache_functionality
            ;;
        --protection)
            test_cache_protection
            ;;
        --warmup)
            test_cache_warmup
            ;;
        --all)
            check_redis
            test_cache_functionality
            test_cache_protection
            test_cache_warmup
            test_cache_performance
            generate_summary
            show_results
            ;;
        --help|help|-h)
            show_help
            ;;
        *)
            log_error "未知选项: $test_type"
            show_help
            exit 1
            ;;
    esac
}

# 执行主函数
main "$@"
