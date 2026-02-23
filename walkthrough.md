
# 修复 Dashboard 通知 NaN 及加载异常问题

## 问题描述

用户在 Dashboard 页面遇到以下问题：

1. 请求通知信息返回为空时，页面下标显示 `NaN`。
2. 点击通知图标一直转圈。

## 原因分析

通过代码分析发现，`src/api/notification.ts` 中的 API 请求（如 `getNotifications`、`getUnreadCount`）直接返回了包含 `code`、`message`、`data` 的完整响应对象，而前端组件（如 `NotificationBell.tsx`、`useNotificationPolling.ts`）预期的是直接的数据（数组或数字）。

1. **NaN 问题**：`getUnreadCount` 返回对象 `{ code: 200, data: 0, ... }`，传入 `Math.max(0, count)` 时导致 `NaN`。
2. **转圈问题**：`getNotifications` 返回对象 `{ code: 200, data: [], ... }`，被作为 `dataSource` 传递给 AntD `List` 组件，或在 `.filter()` 调用时出错，导致 UI 渲染异常或无限 Loading。

## 解决过程

### 1. 修正 API 返回值处理

由 `basebackend-admin-web` 的 `src/api/notification.ts` 文件处理。
增加了 `async/await` 处理，将 `request` 返回的 `Result` 对象解包，直接返回 `res.data`。

```typescript
// 修改前
export const getNotifications = (limit?: number) => {
  return request<UserNotificationDTO[]>({ ... });
};

// 修改后
export const getNotifications = async (limit?: number) => {
  const res = await request<Result<UserNotificationDTO[]>>({ ... });
  return res.data || [];
};
```

同样的修正应用于 `getNotificationList` 和 `getUnreadCount`。

### 2. 更新 Dashboard Hooks

更新 `src/pages/Dashboard/hooks/useDashboardData.ts`，去除不再需要的 `.data` 访问，因为 API 现在直接返回数据。

```typescript
// 修改前
const res = await notificationApi.getNotifications(5)
return res.data || []

// 修改后
const data = await notificationApi.getNotifications(5)
return data || []
```

## 验证结果

- **API 响应**：`getNotifications` 现在返回数组，`getUnreadCount` 返回数字。
- **组件行为**：`NotificationBell` 能正确渲染列表，Badge 显示正确数字（0 或具体数值），无 `NaN` 显示。
