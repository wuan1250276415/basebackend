#!/bin/bash

# 依赖检查脚本
# 检查项目依赖是否有更新、冲突或安全漏洞

echo "========================================="
echo "  Base Backend 依赖检查"
echo "========================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 1. 检查依赖树
echo -e "${BLUE}1. 生成依赖树${NC}"
echo "-------------------"

echo "生成依赖树到 target/dependency-tree.txt ..."
mvn dependency:tree -DoutputFile=target/dependency-tree.txt -q

if [ -f "target/dependency-tree.txt" ]; then
    echo -e "${GREEN}✓ 依赖树生成完成${NC}"
    echo "  文件位置: target/dependency-tree.txt"
else
    echo -e "${RED}✗ 依赖树生成失败${NC}"
fi

echo ""

# 2. 检查依赖冲突
echo -e "${BLUE}2. 检查依赖冲突${NC}"
echo "-------------------"

echo "分析依赖冲突..."
conflicts=$(mvn dependency:tree 2>&1 | grep -i "conflict" | wc -l)

if [ "$conflicts" -eq 0 ]; then
    echo -e "${GREEN}✓ 无依赖冲突${NC}"
else
    echo -e "${YELLOW}⚠ 发现 $conflicts 个潜在冲突${NC}"
    echo "  运行以下命令查看详情:"
    echo "  mvn dependency:tree -Dverbose"
fi

echo ""

# 3. 检查可更新的依赖
echo -e "${BLUE}3. 检查依赖更新${NC}"
echo "-------------------"

echo "检查可更新的依赖（这可能需要几分钟）..."
mvn versions:display-dependency-updates -q > target/dependency-updates.txt 2>&1

if grep -q "No dependencies in Dependencies have newer versions" target/dependency-updates.txt; then
    echo -e "${GREEN}✓ 所有依赖都是最新版本${NC}"
else
    updates=$(grep -c "\\->" target/dependency-updates.txt 2>/dev/null || echo "0")
    if [ "$updates" -gt 0 ]; then
        echo -e "${YELLOW}⚠ 有 $updates 个依赖可以更新${NC}"
        echo "  查看详情: target/dependency-updates.txt"
        echo ""
        echo "  前 5 个可更新的依赖:"
        grep "\\->" target/dependency-updates.txt | head -5
    else
        echo -e "${GREEN}✓ 所有依赖都是最新版本${NC}"
    fi
fi

echo ""

# 4. 检查安全漏洞（可选）
echo -e "${BLUE}4. 安全漏洞检查${NC}"
echo "-------------------"

read -p "是否运行安全漏洞检查？(这可能需要几分钟) (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "运行 OWASP Dependency Check..."
    mvn dependency-check:check -q
    
    if [ -f "target/dependency-check-report/dependency-check-report.html" ]; then
        echo -e "${GREEN}✓ 安全检查完成${NC}"
        echo "  报告位置: target/dependency-check-report/dependency-check-report.html"
        
        # 统计漏洞数量
        if command -v grep &> /dev/null; then
            critical=$(grep -c "severity=\"CRITICAL\"" target/dependency-check-report/dependency-check-report.html 2>/dev/null || echo "0")
            high=$(grep -c "severity=\"HIGH\"" target/dependency-check-report/dependency-check-report.html 2>/dev/null || echo "0")
            
            if [ "$critical" -gt 0 ] || [ "$high" -gt 0 ]; then
                echo -e "${RED}⚠ 发现高危漏洞: Critical=$critical, High=$high${NC}"
            else
                echo -e "${GREEN}✓ 未发现高危漏洞${NC}"
            fi
        fi
    else
        echo -e "${RED}✗ 安全检查失败${NC}"
    fi
else
    echo "跳过安全检查"
fi

echo ""

# 5. 总结
echo "========================================="
echo "  检查完成"
echo "========================================="
echo ""
echo "生成的报告:"
echo "  - 依赖树: target/dependency-tree.txt"
echo "  - 依赖更新: target/dependency-updates.txt"
echo "  - 安全报告: target/dependency-check-report/"
echo ""
echo "建议操作:"
echo "  1. 查看依赖冲突: mvn dependency:tree -Dverbose"
echo "  2. 更新依赖: 修改 pom.xml 中的版本号"
echo "  3. 修复安全漏洞: 升级有漏洞的依赖"
echo ""
