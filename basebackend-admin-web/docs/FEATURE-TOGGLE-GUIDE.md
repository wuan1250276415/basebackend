# Admin-Web 特性开关集成指南

## 📚 概述

Admin-Web 已集成特性开关功能，支持：
- ✅ 基础特性开关控制
- ✅ 用户级特性开关
- ✅ AB测试/多变体实验
- ✅ 特性开关管理页面
- ✅ React Hooks 和组件

## 🚀 快速开始

### 1. 后端配置

确保后端特性开关服务已启用对应配置：

```yaml
# application.yml
spring:
  profiles:
    active: dev,feature-toggle

feature-toggle:
  enabled: true
  provider: UNLEASH  # 或 FLAGSMITH
  unleash:
    url: http://localhost:4242/api
    api-token: your-token-here
```

### 2. 启动服务

```bash
# 1. 启动 Unleash/Flagsmith
./scripts/start-feature-toggle.sh

# 2. 启动后端
cd basebackend-system-api
mvn spring-boot:run

# 3. 启动前端
cd basebackend-admin-web
npm run dev
```

## 💡 使用方式

### 方式1: 使用 Hook

```tsx
import { useFeatureToggle } from '@/hooks/useFeatureToggle';

function MyComponent() {
  const { enabled, loading } = useFeatureToggle('new-feature');

  if (loading) {
    return <Spin />;
  }

  return (
    <div>
      {enabled ? (
        <NewFeature />
      ) : (
        <OldFeature />
      )}
    </div>
  );
}
```

### 方式2: 使用组件

```tsx
import { FeatureToggle } from '@/components/FeatureToggle';

function MyPage() {
  return (
    <div>
      <h1>我的页面</h1>

      {/* 简单使用 */}
      <FeatureToggle featureName="new-button">
        <Button type="primary">新功能按钮</Button>
      </FeatureToggle>

      {/* 带降级内容 */}
      <FeatureToggle
        featureName="premium-feature"
        fallback={<Alert message="该功能仅对VIP用户开放" type="info" />}
      >
        <PremiumContent />
      </FeatureToggle>
    </div>
  );
}
```

### 方式3: 用户级特性

```tsx
import { useFeatureToggle } from '@/hooks/useFeatureToggle';

function UserFeature() {
  const currentUser = useUserStore((state) => state.currentUser);

  const { enabled } = useFeatureToggle('vip-feature', {
    userId: currentUser.id,
    email: currentUser.email,
  });

  return enabled ? <VIPContent /> : <RegularContent />;
}
```

### 方式4: AB测试

```tsx
import { ABTest } from '@/components/ABTest';

function CheckoutPage() {
  const currentUser = useUserStore((state) => state.currentUser);

  return (
    <ABTest
      featureName="checkout-experiment"
      context={{ userId: currentUser.id }}
      variants={{
        'control': <OldCheckoutFlow />,
        'variant-a': <NewCheckoutFlowA />,
        'variant-b': <NewCheckoutFlowB />,
      }}
      defaultVariant={<OldCheckoutFlow />}
    />
  );
}
```

### 方式5: 批量检查

```tsx
import { useFeatureToggles } from '@/hooks/useFeatureToggle';

function Dashboard() {
  const { features, isEnabled } = useFeatureToggles([
    'dashboard-analytics',
    'dashboard-reports',
    'dashboard-export',
  ]);

  return (
    <div>
      {isEnabled('dashboard-analytics') && <AnalyticsWidget />}
      {isEnabled('dashboard-reports') && <ReportsWidget />}
      {isEnabled('dashboard-export') && <ExportButton />}
    </div>
  );
}
```

## 📊 管理页面

访问 `/feature-toggles` 查看所有特性开关状态：

- 查看所有特性开关列表
- 查看启用/禁用统计
- 搜索特性
- 刷新配置

## 🎯 实际应用场景

### 场景1: 灰度发布新功能

```tsx
// 1. 在Unleash中创建特性 "new-dashboard"
// 2. 配置渐进式发布：1% → 10% → 50% → 100%

function DashboardPage() {
  return (
    <FeatureToggle
      featureName="new-dashboard"
      fallback={<OldDashboard />}
    >
      <NewDashboard />
    </FeatureToggle>
  );
}
```

### 场景2: VIP用户专属功能

```tsx
// 在Unleash中配置策略：
// - userWithId: VIP用户ID列表
// - 或 customField: role=VIP

function SettingsPage() {
  const user = useUserStore((state) => state.currentUser);

  return (
    <FeatureToggle
      featureName="advanced-settings"
      context={{
        userId: user.id,
        properties: { role: user.role }
      }}
      fallback={
        <Alert message="该功能仅对VIP用户开放" type="warning" />
      }
    >
      <AdvancedSettings />
    </FeatureToggle>
  );
}
```

### 场景3: AB测试新UI

```tsx
// 在Unleash中创建特性 "new-ui-experiment"
// 配置变体：control (50%), variant-a (25%), variant-b (25%)

function HomePage() {
  const user = useUserStore((state) => state.currentUser);

  return (
    <ABTest
      featureName="new-ui-experiment"
      context={{ userId: user.id }}
      variants={{
        'control': <CurrentUI />,
        'variant-a': <NewUI_A />,
        'variant-b': <NewUI_B />,
      }}
    />
  );
}
```

### 场景4: 环境隔离

```tsx
// 仅在开发环境显示调试工具
function App() {
  return (
    <div>
      <MainContent />

      <FeatureToggle featureName="debug-tools">
        <DebugPanel />
      </FeatureToggle>
    </div>
  );
}

// 在Unleash中配置：
// - 策略: environment=development
```

## 🔧 API参考

### Hooks

#### useFeatureToggle(featureName, context?, defaultValue?)

检查单个特性是否启用。

**参数**:
- `featureName`: string - 特性名称
- `context`: FeatureContext - 可选，用户上下文
- `defaultValue`: boolean - 可选，默认值（默认false）

**返回**:
- `enabled`: boolean - 特性是否启用
- `loading`: boolean - 是否加载中
- `error`: Error | null - 错误信息
- `refresh`: () => void - 刷新函数

#### useFeatureToggles(featureNames, context?)

批量检查多个特性。

**参数**:
- `featureNames`: string[] - 特性名称列表
- `context`: FeatureContext - 可选，用户上下文

**返回**:
- `features`: Record<string, boolean> - 特性状态映射
- `isEnabled`: (name: string) => boolean - 检查函数
- `loading`: boolean
- `error`: Error | null
- `refresh`: () => void

#### useAllFeatures(context?)

获取所有特性状态。

#### useVariant(featureName, context?)

获取变体信息（用于AB测试）。

### 组件

#### `<FeatureToggle>`

Props:
- `featureName`: string - 特性名称 **(必需)**
- `context`: FeatureContext - 用户上下文
- `children`: ReactNode - 特性启用时渲染
- `fallback`: ReactNode - 特性禁用时渲染
- `showLoading`: boolean - 是否显示加载状态
- `defaultValue`: boolean - 默认值

#### `<ABTest>`

Props:
- `featureName`: string - 实验名称 **(必需)**
- `context`: FeatureContext - 用户上下文
- `variants`: Record<string, ReactNode> - 变体映射 **(必需)**
- `defaultVariant`: ReactNode - 默认变体
- `showLoading`: boolean - 是否显示加载状态

## 📝 最佳实践

### 1. 使用有意义的特性名称

```tsx
// ✅ 好的命名
<FeatureToggle featureName="user-profile-redesign">
<FeatureToggle featureName="payment-new-flow">
<FeatureToggle featureName="dashboard-analytics-v2">

// ❌ 不好的命名
<FeatureToggle featureName="feature1">
<FeatureToggle featureName="test">
<FeatureToggle featureName="new-stuff">
```

### 2. 提供降级方案

```tsx
// ✅ 提供友好的降级内容
<FeatureToggle
  featureName="premium-feature"
  fallback={
    <Alert
      message="该功能即将推出"
      description="升级到VIP可抢先体验"
      type="info"
    />
  }
>
  <PremiumFeature />
</FeatureToggle>

// ❌ 直接返回空
<FeatureToggle featureName="premium-feature">
  <PremiumFeature />
</FeatureToggle>
```

### 3. 及时清理废弃的特性开关

```tsx
// 功能完全发布后，移除特性开关代码
// 之前:
<FeatureToggle featureName="new-dashboard">
  <NewDashboard />
</FeatureToggle>

// 之后:
<NewDashboard />
```

### 4. 使用TypeScript增强类型安全

```tsx
// 定义特性名称常量
export const FEATURE_NAMES = {
  NEW_DASHBOARD: 'new-dashboard',
  PREMIUM_FEATURES: 'premium-features',
  BETA_TOOLS: 'beta-tools',
} as const;

// 使用
<FeatureToggle featureName={FEATURE_NAMES.NEW_DASHBOARD}>
  <NewDashboard />
</FeatureToggle>
```

## 🔗 相关资源

- [后端集成文档](../../docs/FEATURE-TOGGLE-SUMMARY.md)
- [Unleash 文档](https://docs.getunleash.io/)
- [Flagsmith 文档](https://docs.flagsmith.com/)

## ❓ 常见问题

### Q: 如何在开发环境测试特性开关？

A:
1. 启动本地Unleash: `./scripts/start-feature-toggle.sh`
2. 访问 http://localhost:4242
3. 创建特性并配置策略
4. 在代码中使用

### Q: 特性开关加载很慢怎么办？

A:
- 使用 `defaultValue` 提供默认值
- 使用 `showLoading` 显示加载状态
- 检查网络连接和后端服务状态

### Q: 如何在生产环境使用？

A:
1. 部署Unleash/Flagsmith到生产环境（或使用SaaS版本）
2. 配置生产环境的API Token
3. 确保后端服务可以访问特性开关服务

---

**集成完成！** 🎉 现在可以在Admin-Web中使用特性开关功能了。
