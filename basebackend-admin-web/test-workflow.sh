#!/bin/bash

# 工作流模块快速测试脚本
# 用于验证工作流前端模块的基本功能

echo "========================================"
echo "  工作流模块快速测试"
echo "========================================"
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查项计数
TOTAL_CHECKS=0
PASSED_CHECKS=0
FAILED_CHECKS=0

# 检查函数
check_file() {
  TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
  if [ -f "$1" ]; then
    echo -e "${GREEN}✓${NC} 文件存在: $1"
    PASSED_CHECKS=$((PASSED_CHECKS + 1))
    return 0
  else
    echo -e "${RED}✗${NC} 文件缺失: $1"
    FAILED_CHECKS=$((FAILED_CHECKS + 1))
    return 1
  fi
}

check_dir() {
  TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
  if [ -d "$1" ]; then
    echo -e "${GREEN}✓${NC} 目录存在: $1"
    PASSED_CHECKS=$((PASSED_CHECKS + 1))
    return 0
  else
    echo -e "${RED}✗${NC} 目录缺失: $1"
    FAILED_CHECKS=$((FAILED_CHECKS + 1))
    return 1
  fi
}

echo "1. 检查页面组件..."
echo "-----------------------------------"
check_file "src/pages/Workflow/TaskManagement/TodoList.tsx"
check_file "src/pages/Workflow/TaskManagement/TaskDetail.tsx"
check_file "src/pages/Workflow/TaskManagement/MyInitiated.tsx"
check_file "src/pages/Workflow/ProcessTemplate/index.tsx"
check_file "src/pages/Workflow/ProcessTemplate/LeaveApproval.tsx"
check_file "src/pages/Workflow/ProcessTemplate/ExpenseApproval.tsx"
check_file "src/pages/Workflow/ProcessTemplate/PurchaseApproval.tsx"
check_file "src/pages/Workflow/ProcessInstance/index.tsx"
check_file "src/pages/Workflow/ProcessInstance/Detail.tsx"
check_file "src/pages/Workflow/ProcessDefinition/index.tsx"
check_file "src/pages/Workflow/ProcessHistory/index.tsx"
echo ""

echo "2. 检查API接口..."
echo "-----------------------------------"
check_file "src/api/workflow/processDefinition.ts"
check_file "src/api/workflow/processInstance.ts"
check_file "src/api/workflow/task.ts"
echo ""

echo "3. 检查类型定义..."
echo "-----------------------------------"
check_file "src/types/workflow.ts"
echo ""

echo "4. 检查状态管理..."
echo "-----------------------------------"
check_file "src/stores/workflow.ts"
echo ""

echo "5. 检查路由配置..."
echo "-----------------------------------"
check_file "src/router/index.tsx"
echo ""

echo "6. 检查通用组件..."
echo "-----------------------------------"
check_dir "src/components/Workflow"
check_file "src/components/Workflow/StatusTags.tsx"
check_file "src/components/Workflow/Statistics.tsx"
check_file "src/components/Workflow/EmptyStates.tsx"
check_file "src/components/Workflow/Timeline.tsx"
check_file "src/components/Workflow/index.ts"
echo ""

echo "7. 检查工具函数..."
echo "-----------------------------------"
check_dir "src/utils/workflow"
check_file "src/utils/workflow/dateUtils.ts"
check_file "src/utils/workflow/keyGenerator.ts"
check_file "src/utils/workflow/statusUtils.ts"
check_file "src/utils/workflow/amountUtils.ts"
check_file "src/utils/workflow/index.ts"
echo ""

echo "8. 检查常量配置..."
echo "-----------------------------------"
check_dir "src/constants/workflow"
check_file "src/constants/workflow/index.ts"
echo ""

echo "9. 检查文档文件..."
echo "-----------------------------------"
check_file "WORKFLOW-IMPLEMENTATION.md"
check_file "WORKFLOW-CODE-EXAMPLES.md"
check_file "README-WORKFLOW.md"
check_file "WORKFLOW-MENU-CONFIG-GUIDE.md"
check_file "WORKFLOW-FRONTEND-COMPLETION-REPORT.md"
check_file "WORKFLOW-FRONTEND-FINAL-REPORT.md"
check_file "WORKFLOW-COMPONENT-USAGE-GUIDE.md"
echo ""

echo "========================================"
echo "  测试结果汇总"
echo "========================================"
echo -e "总检查项: ${YELLOW}${TOTAL_CHECKS}${NC}"
echo -e "通过: ${GREEN}${PASSED_CHECKS}${NC}"
echo -e "失败: ${RED}${FAILED_CHECKS}${NC}"
echo ""

if [ $FAILED_CHECKS -eq 0 ]; then
  echo -e "${GREEN}✓ 所有检查项通过！${NC}"
  echo ""
  echo "下一步操作："
  echo "1. 运行 npm install 安装依赖"
  echo "2. 运行 npm run dev 启动开发服务器"
  echo "3. 访问 http://localhost:5173/workflow/todo 查看待办任务"
  echo ""
  exit 0
else
  echo -e "${RED}✗ 部分检查项失败，请检查缺失的文件${NC}"
  echo ""
  exit 1
fi
