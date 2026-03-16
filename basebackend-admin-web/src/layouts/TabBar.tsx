/**
 * 多标签页栏组件
 * 渲染已打开的页面标签，支持切换和关闭
 * - 从 tabStore 读取标签列表和激活状态
 * - 点击标签切换页面（通过 react-router-dom 导航）
 * - 支持关闭单个标签（首页标签不可关闭）
 * - 使用 Ant Design Tabs 的 editable-card 类型
 */
import React, { useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Tabs } from 'antd';
import { useTabStore } from '@/stores/tabStore';

/**
 * TabBar 多标签页栏
 */
const TabBar: React.FC = () => {
  const navigate = useNavigate();
  const { tabs, activeKey, setActiveTab, removeTab } = useTabStore();

  /** 标签页切换：更新激活状态并导航 */
  const handleChange = useCallback(
    (key: string) => {
      setActiveTab(key);
      navigate(key);
    },
    [setActiveTab, navigate],
  );

  /** 标签页编辑（关闭）：仅处理 remove 操作 */
  const handleEdit = useCallback(
    (targetKey: React.MouseEvent | React.KeyboardEvent | string, action: 'add' | 'remove') => {
      if (action === 'remove') {
        removeTab(targetKey as string);
        // 如果关闭的是当前激活标签，导航到新的激活标签
        const { activeKey: newActiveKey } = useTabStore.getState();
        if (targetKey === activeKey) {
          navigate(newActiveKey);
        }
      }
    },
    [removeTab, activeKey, navigate],
  );

  /** 将 TabItem[] 转换为 Tabs 的 items 格式 */
  const items = tabs.map((tab) => ({
    key: tab.key,
    label: tab.title,
    closable: tab.closable,
  }));

  return (
    <Tabs
      type="editable-card"
      hideAdd
      activeKey={activeKey}
      onChange={handleChange}
      onEdit={handleEdit}
      items={items}
      size="small"
      style={{ marginBottom: 12 }}
    />
  );
};

export default TabBar;
