/**
 * 主布局组件
 * 基于 ProLayout 实现企业级后台布局
 * - 深色侧边栏 (navTheme: 'realDark')
 * - 动态菜单：从 authStore.menus 生成，过滤 type=0（目录）和 type=1（菜单）
 * - 顶部右侧：全屏切换、主题切换、用户头像下拉菜单
 * - 内容区：面包屑 + TabBar（占位） + 子路由 Outlet
 */
import React, { useCallback, useEffect, useMemo } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { ProLayout } from '@ant-design/pro-components';
import type { MenuDataItem } from '@ant-design/pro-components';
import {
  FullscreenOutlined,
  FullscreenExitOutlined,
  BulbOutlined,
  BulbFilled,
  UserOutlined,
  HomeOutlined,
  SettingOutlined,
  TeamOutlined,
  MenuOutlined,
  AppstoreOutlined,
  DatabaseOutlined,
  MonitorOutlined,
  FileTextOutlined,
  MessageOutlined,
  DashboardOutlined,
  SafetyOutlined,
  ApartmentOutlined,
  BookOutlined,
  BarChartOutlined,
  CloudServerOutlined,
  ProfileOutlined,
} from '@ant-design/icons';
import { Breadcrumb, Tooltip, theme } from 'antd';
import { useAuthStore } from '@/stores/authStore';
import { useSettingStore } from '@/stores/settingStore';
import { useTabStore } from '@/stores/tabStore';
import TabBar from './TabBar';
import UserDropdown from './UserDropdown';
import type { MenuItem } from '@/types';
import { filterUnsupportedMenus } from '@/router/menuAvailability';

/** 图标名称到 Ant Design 图标组件的映射表 */
const iconMap: Record<string, React.ReactNode> = {
  dashboard: <DashboardOutlined />,
  home: <HomeOutlined />,
  setting: <SettingOutlined />,
  user: <UserOutlined />,
  team: <TeamOutlined />,
  menu: <MenuOutlined />,
  appstore: <AppstoreOutlined />,
  database: <DatabaseOutlined />,
  monitor: <MonitorOutlined />,
  'file-text': <FileTextOutlined />,
  message: <MessageOutlined />,
  safety: <SafetyOutlined />,
  apartment: <ApartmentOutlined />,
  book: <BookOutlined />,
  'bar-chart': <BarChartOutlined />,
  'cloud-server': <CloudServerOutlined />,
  profile: <ProfileOutlined />,
};

/**
 * 根据图标名称获取对应的图标组件
 * 找不到映射时返回 AppstoreOutlined 作为默认图标
 */
function getIcon(iconName?: string): React.ReactNode {
  if (!iconName) return undefined;
  return iconMap[iconName] || <AppstoreOutlined />;
}

/**
 * 将后端 MenuItem[] 转换为 ProLayout 的 MenuDataItem[]
 * 只保留 type=0（目录）和 type=1（菜单），过滤掉 type=2（按钮）
 */
export function convertMenus(menus: MenuItem[]): MenuDataItem[] {
  return menus
    .filter((item) => item.type === 0 || item.type === 1)
    .sort((a, b) => a.orderNum - b.orderNum)
    .map((item) => ({
      path: item.path,
      name: item.name,
      icon: getIcon(item.icon),
      children: item.children ? convertMenus(item.children) : undefined,
    }));
}

/**
 * 根据当前路径生成面包屑数据
 * 递归搜索菜单树，构建从根到当前节点的路径
 */
export function buildBreadcrumb(
  menus: MenuItem[],
  pathname: string,
): { title: string; path?: string }[] {
  for (const menu of menus) {
    if (menu.path === pathname && menu.type !== 2) {
      return [{ title: menu.name }];
    }
    if (menu.children && menu.children.length > 0) {
      const child = buildBreadcrumb(menu.children, pathname);
      if (child.length > 0) {
        return [{ title: menu.name, path: menu.path }, ...child];
      }
    }
  }
  return [];
}

/** 全屏切换 Hook */
function useFullscreen() {
  const [isFullscreen, setIsFullscreen] = React.useState(false);

  React.useEffect(() => {
    const handler = () => setIsFullscreen(!!document.fullscreenElement);
    document.addEventListener('fullscreenchange', handler);
    return () => document.removeEventListener('fullscreenchange', handler);
  }, []);

  const toggle = useCallback(() => {
    if (document.fullscreenElement) {
      document.exitFullscreen();
    } else {
      document.documentElement.requestFullscreen();
    }
  }, []);

  return { isFullscreen, toggle };
}

/**
 * BasicLayout 主布局组件
 */
const BasicLayout: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { token: antdToken } = theme.useToken();

  // 状态管理
  const { menus } = useAuthStore();
  const { theme: appTheme, collapsed, toggleTheme, toggleCollapsed, setCollapsed } = useSettingStore();
  const { addTab } = useTabStore();
  const { isFullscreen, toggle: toggleFullscreen } = useFullscreen();
  const supportedMenus = useMemo(() => filterUnsupportedMenus(menus), [menus]);

  /**
   * 响应式侧边栏：视口宽度 < 768px 时自动折叠为图标模式
   * 使用 matchMedia 监听视口变化，避免频繁 resize 事件
   */
  useEffect(() => {
    const mql = window.matchMedia('(max-width: 767px)');
    const handler = (e: MediaQueryListEvent | MediaQueryList) => {
      if (e.matches) {
        setCollapsed(true);
      }
    };
    // 初始检查
    handler(mql);
    mql.addEventListener('change', handler);
    return () => mql.removeEventListener('change', handler);
  }, [setCollapsed]);

  // 将后端菜单转换为 ProLayout 菜单格式
  const menuData = useMemo(() => convertMenus(supportedMenus), [supportedMenus]);

  // 面包屑数据
  const breadcrumbItems = useMemo(() => {
    const items: { title: React.ReactNode; href?: string }[] = [
      { title: <HomeOutlined />, href: '/' },
    ];
    const trail = buildBreadcrumb(supportedMenus, location.pathname);
    trail.forEach((item) => {
      items.push({ title: item.title, href: item.path });
    });
    return items;
  }, [supportedMenus, location.pathname]);

  /** 在菜单树中查找路径对应的名称 */
  const findMenuName = useCallback(
    (items: MenuItem[], target: string): string => {
      for (const item of items) {
        if (item.path === target) return item.name;
        if (item.children) {
          const found = findMenuName(item.children, target);
          if (found) return found;
        }
      }
      return target;
    },
    [],
  );

  /** 菜单点击导航 */
  const handleMenuClick = useCallback(
    (path: string) => {
      const title = findMenuName(supportedMenus, path);
      addTab({ key: path, title, closable: path !== '/' });
      navigate(path);
    },
    [supportedMenus, addTab, navigate, findMenuName],
  );

  return (
    <ProLayout
      title="BaseBackend"
      logo={false}
      layout="mix"
      navTheme="realDark"
      fixSiderbar
      fixedHeader
      collapsed={collapsed}
      onCollapse={toggleCollapsed}
      siderWidth={220}
      token={{
        sider: {
          colorMenuBackground: '#001529',
          colorTextMenu: 'rgba(255, 255, 255, 0.65)',
          colorTextMenuSelected: '#fff',
          colorBgMenuItemSelected: '#1677ff',
        },
        header: {
          colorBgHeader: '#fff',
          heightLayoutHeader: 48,
        },
      }}
      location={{ pathname: location.pathname }}
      menuDataRender={() => menuData}
      menuItemRender={(item, dom) => (
        <div onClick={() => item.path && handleMenuClick(item.path)}>
          {dom}
        </div>
      )}
      subMenuItemRender={(_item, dom) => dom}
      /* 使用 actionsRender 替代已弃用的 rightContentRender */
      actionsRender={() => [
        /* 全屏切换按钮 */
        <Tooltip key="fullscreen" title={isFullscreen ? '退出全屏' : '全屏'}>
          {isFullscreen ? (
            <FullscreenExitOutlined
              style={{ fontSize: 16, cursor: 'pointer' }}
              onClick={toggleFullscreen}
            />
          ) : (
            <FullscreenOutlined
              style={{ fontSize: 16, cursor: 'pointer' }}
              onClick={toggleFullscreen}
            />
          )}
        </Tooltip>,
        /* 主题切换按钮 */
        <Tooltip
          key="theme"
          title={appTheme === 'dark' ? '切换亮色模式' : '切换暗色模式'}
        >
          {appTheme === 'dark' ? (
            <BulbFilled
              style={{ fontSize: 16, cursor: 'pointer', color: '#fadb14' }}
              onClick={toggleTheme}
            />
          ) : (
            <BulbOutlined
              style={{ fontSize: 16, cursor: 'pointer' }}
              onClick={toggleTheme}
            />
          )}
        </Tooltip>,
        /* 用户头像下拉菜单 */
        <UserDropdown key="user-dropdown" />,
      ]}
      contentStyle={{
        margin: 16,
        padding: 0,
        background: 'transparent',
      }}
      /* 面包屑由内容区手动渲染 */
      breadcrumbRender={() => undefined}
    >
      {/* 内容区：面包屑 + TabBar 占位 + 页面内容 */}
      <div>
        {/* 面包屑导航 */}
        <Breadcrumb items={breadcrumbItems} style={{ marginBottom: 12 }} />

        {/* 多标签页栏 */}
        <TabBar />

        {/* 页面内容容器：圆角卡片 + 阴影效果 */}
        <div
          style={{
            background: antdToken.colorBgContainer,
            borderRadius: 8,
            padding: 24,
            minHeight: 360,
            boxShadow: '0 2px 8px rgba(0, 0, 0, 0.08)',
          }}
        >
          <Outlet />
        </div>
      </div>
    </ProLayout>
  );
};

export default BasicLayout;
