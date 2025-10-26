# 工作流前端集成 - README

## 🎉 集成概述

工作流管理系统前端已经完成基础架构搭建，包含：

- ✅ TypeScript 类型定义
- ✅ API 接口层封装
- ✅ Zustand 状态管理
- ✅ 完整的代码示例（待办任务、请假审批）

---

## 📦 已创建的文件

### 1. 基础架构
```
✅ package.json (已更新依赖)
✅ src/types/workflow.ts (类型定义)
✅ src/api/workflow/processDefinition.ts (流程定义 API)
✅ src/api/workflow/processInstance.ts (流程实例 API)
✅ src/api/workflow/task.ts (任务 API)
✅ src/stores/workflow.ts (工作流 Store)
```

### 2. 文档
```
✅ WORKFLOW-IMPLEMENTATION.md (实施指南)
✅ WORKFLOW-CODE-EXAMPLES.md (代码示例)
✅ README-WORKFLOW.md (本文件)
```

---

## 🚀 快速开始

### 第一步：安装依赖

```bash
cd basebackend-admin-web
npm install
```

这将安装以下关键依赖：
- `@antv/x6` - BPMN 流程设计器
- `@formily/core`, `@formily/react`, `@formily/antd-v5` - 动态表单

### 第二步：创建页面目录

```bash
# 在 src/pages 下创建工作流目录结构
mkdir -p src/pages/Workflow/{TaskManagement,ProcessTemplate,ProcessInstance,ProcessDefinition,FormDesigner,ProcessHistory}

# 创建组件目录
mkdir -p src/components/Workflow/{BpmnDesigner,BpmnViewer,FormRenderer,TaskCard,ApprovalHistory}
```

### 第三步：复制示例代码

从 `WORKFLOW-CODE-EXAMPLES.md` 中复制以下页面的代码：

1. **待办任务列表** → `src/pages/Workflow/TaskManagement/TodoList.tsx`
2. **请假申请表单** → `src/pages/Workflow/ProcessTemplate/LeaveApproval.tsx`

### 第四步：更新路由

在 `src/router/index.tsx` 中添加：

```typescript
import TodoList from '@/pages/Workflow/TaskManagement/TodoList'
import LeaveApproval from '@/pages/Workflow/ProcessTemplate/LeaveApproval'

// 在 <Route path="/" element={<Layout />}> 内部添加：
<Route path="workflow/todo" element={<TodoList />} />
<Route path="workflow/template/leave" element={<LeaveApproval />} />
```

### 第五步：启动开发服务器

```bash
npm run dev
```

访问：
- http://localhost:5173/workflow/todo - 待办任务列表
- http://localhost:5173/workflow/template/leave - 请假申请

---

## 📋 完整功能清单

### 已实现（基础架构）
- [x] TypeScript 类型定义
- [x] API 接口层（3个文件）
- [x] Zustand Store
- [x] package.json 依赖更新
- [x] 代码示例（2个完整页面）

### 待实现（页面开发）

#### 优先级 ⭐⭐⭐（核心功能）
- [ ] 待办任务列表 - TodoList.tsx
- [ ] 任务详情 - TaskDetail.tsx
- [ ] 审批表单 - ApprovalForm.tsx
- [ ] 我发起的流程 - MyInitiated.tsx

#### 优先级 ⭐⭐（常用功能）
- [ ] 请假申请 - LeaveApproval.tsx
- [ ] 报销申请 - ExpenseApproval.tsx
- [ ] 采购申请 - PurchaseApproval.tsx
- [ ] 流程实例列表 - ProcessInstance/index.tsx
- [ ] 流程实例详情 - ProcessInstance/Detail.tsx

#### 优先级 ⭐（高级功能）
- [ ] BPMN 流程设计器 - ProcessDefinition/Designer.tsx
- [ ] 流程定义列表 - ProcessDefinition/index.tsx
- [ ] 表单设计器 - FormDesigner/Designer.tsx
- [ ] 流程历史 - ProcessHistory/index.tsx
- [ ] BPMN 查看器组件 - components/BpmnViewer.tsx

---

## 🏗️ 项目结构

```
basebackend-admin-web/
├── package.json (✅ 已更新)
├── WORKFLOW-IMPLEMENTATION.md (✅ 实施指南)
├── WORKFLOW-CODE-EXAMPLES.md (✅ 代码示例)
├── README-WORKFLOW.md (✅ 本文件)
└── src/
    ├── types/
    │   └── workflow.ts (✅ 类型定义)
    ├── api/
    │   └── workflow/ (✅ API接口)
    │       ├── processDefinition.ts
    │       ├── processInstance.ts
    │       └── task.ts
    ├── stores/
    │   └── workflow.ts (✅ 状态管理)
    ├── components/
    │   └── Workflow/ (⏳ 待创建)
    │       ├── BpmnDesigner/
    │       ├── BpmnViewer/
    │       ├── FormRenderer/
    │       ├── TaskCard/
    │       └── ApprovalHistory/
    └── pages/
        └── Workflow/ (⏳ 待创建)
            ├── TaskManagement/
            ├── ProcessTemplate/
            ├── ProcessInstance/
            ├── ProcessDefinition/
            ├── FormDesigner/
            └── ProcessHistory/
```

---

## 💡 开发建议

### 1. 分阶段实施

**阶段1**：核心功能（1周）
- 待办任务管理
- 流程发起（请假、报销）
- 任务审批

**阶段2**：流程监控（3-5天）
- 流程实例列表
- 流程详情查看
- 流程图高亮

**阶段3**：高级功能（1-2周）
- BPMN 流程设计器
- 表单设计器
- 流程历史追踪

### 2. 使用现有组件

充分利用 Ant Design 和 ProComponents：
- `ProTable` - 表格列表
- `ProForm` - 表单页面
- `ProCard` - 卡片布局
- `ProDescriptions` - 详情展示

### 3. 参考示例代码

`WORKFLOW-CODE-EXAMPLES.md` 包含：
- ✅ 待办任务列表（完整代码）
- ✅ 请假申请表单（完整代码）
- 可以直接复制使用或作为模板

### 4. BPMN 设计器可以延后

如果时间紧张：
1. 使用 Camunda Modeler 桌面工具设计流程
2. 前端只实现查看功能（BpmnViewer）
3. 后期再实现在线编辑功能

---

## 🔗 相关资源

### 文档
- AntV X6: https://x6.antv.antgroup.com/
- Formily: https://formilyjs.org/
- Ant Design: https://ant.design/
- Camunda: https://docs.camunda.org/

### 后端 API
- Swagger UI: http://localhost:8085/scheduler/api-docs
- Camunda 管理界面: http://localhost:8085/scheduler/camunda/app/

---

## 📊 预估工作量

| 模块 | 文件数 | 开发时间 | 状态 |
|------|--------|----------|------|
| 基础架构 | 7 | - | ✅ 已完成 |
| 待办任务 | 4 | 2-3天 | ⏳ 待开发 |
| 流程模板 | 3 | 2天 | ⏳ 待开发 |
| 流程监控 | 3 | 2-3天 | ⏳ 待开发 |
| BPMN设计器 | 5 | 3-4天 | ⏳ 待开发 |
| 表单设计器 | 3 | 2-3天 | ⏳ 待开发 |
| 流程历史 | 2 | 1-2天 | ⏳ 待开发 |
| **总计** | **27** | **12-17天** | **4% 完成** |

---

## 🆘 故障排查

### Q1: 依赖安装失败
```bash
# 清除缓存重新安装
rm -rf node_modules package-lock.json
npm install
```

### Q2: TypeScript 报错
确保 `tsconfig.json` 配置了路径别名：
```json
{
  "compilerOptions": {
    "paths": {
      "@/*": ["./src/*"]
    }
  }
}
```

### Q3: API 请求失败
检查后端服务是否启动：
```bash
# 后端应该运行在 http://localhost:8085
curl http://localhost:8085/scheduler/api/workflow/definitions
```

---

## ✅ 下一步行动

1. **运行 `npm install`** 安装依赖
2. **查看代码示例** - `WORKFLOW-CODE-EXAMPLES.md`
3. **创建页面目录** - 按照上面的结构创建
4. **复制示例代码** - 从文档复制到对应文件
5. **更新路由配置** - 添加工作流相关路由
6. **启动开发服务器** - `npm run dev`
7. **测试功能** - 访问待办任务页面

---

## 📞 需要帮助？

如果在实施过程中遇到问题：
1. 查看 `WORKFLOW-IMPLEMENTATION.md` 了解整体架构
2. 参考 `WORKFLOW-CODE-EXAMPLES.md` 中的完整代码
3. 查看 AntV X6 和 Formily 官方文档
4. 检查后端 API 是否正常工作

祝开发顺利！🎉
