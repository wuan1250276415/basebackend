import React, { useMemo, useState } from 'react';
import { Input, Empty } from 'antd';
import * as Icons from '@ant-design/icons';

/**
 * 图标选择器组件
 *
 * 用于菜单管理页面中选择菜单图标，展示常用 Ant Design 图标的网格列表，
 * 支持按名称搜索/过滤，点击选中后返回图标名称字符串。
 *
 * @example
 * <IconSelector value={iconName} onChange={(name) => setIconName(name)} />
 */

/** 常用图标列表（适用于后台管理系统菜单） */
const ICON_LIST: string[] = [
  // 方向类
  'UpOutlined', 'DownOutlined', 'LeftOutlined', 'RightOutlined',
  // 建议/提示类
  'QuestionCircleOutlined', 'InfoCircleOutlined', 'ExclamationCircleOutlined',
  'CheckCircleOutlined', 'CloseCircleOutlined', 'WarningOutlined',
  // 编辑操作类
  'EditOutlined', 'DeleteOutlined', 'PlusOutlined', 'MinusOutlined',
  'CopyOutlined', 'ScissorOutlined', 'SearchOutlined', 'FilterOutlined',
  // 数据展示类
  'AreaChartOutlined', 'BarChartOutlined', 'LineChartOutlined',
  'PieChartOutlined', 'DotChartOutlined', 'FundOutlined',
  // 通用类
  'HomeOutlined', 'SettingOutlined', 'AppstoreOutlined', 'MenuOutlined',
  'DashboardOutlined', 'FormOutlined', 'TableOutlined', 'ProfileOutlined',
  'OrderedListOutlined', 'UnorderedListOutlined',
  // 用户与团队
  'UserOutlined', 'TeamOutlined', 'UserAddOutlined', 'UserDeleteOutlined',
  'UsergroupAddOutlined', 'SolutionOutlined', 'IdcardOutlined',
  // 文件与文件夹
  'FileOutlined', 'FileTextOutlined', 'FolderOutlined', 'FolderOpenOutlined',
  'FileSearchOutlined', 'FileDoneOutlined',
  // 系统管理
  'LockOutlined', 'UnlockOutlined', 'SafetyOutlined', 'KeyOutlined',
  'AuditOutlined', 'SecurityScanOutlined', 'EyeOutlined', 'EyeInvisibleOutlined',
  // 消息与通知
  'BellOutlined', 'MailOutlined', 'MessageOutlined', 'CommentOutlined',
  'NotificationOutlined', 'SoundOutlined',
  // 工具类
  'ToolOutlined', 'BugOutlined', 'CodeOutlined', 'BuildOutlined',
  'ExperimentOutlined', 'ThunderboltOutlined', 'RocketOutlined',
  // 品牌/业务
  'ShopOutlined', 'ShoppingOutlined', 'ShoppingCartOutlined',
  'BankOutlined', 'MoneyCollectOutlined', 'DollarOutlined',
  // 多媒体
  'PictureOutlined', 'CameraOutlined', 'VideoCameraOutlined',
  'PlayCircleOutlined', 'CustomerServiceOutlined',
  // 其他常用
  'CloudOutlined', 'UploadOutlined', 'DownloadOutlined',
  'PrinterOutlined', 'WifiOutlined', 'GlobalOutlined',
  'DesktopOutlined', 'LaptopOutlined', 'MobileOutlined',
  'DatabaseOutlined', 'HddOutlined', 'ClusterOutlined',
  'EnvironmentOutlined', 'CalendarOutlined', 'ClockCircleOutlined',
  'TagOutlined', 'TagsOutlined', 'StarOutlined',
  'HeartOutlined', 'FireOutlined', 'CrownOutlined',
  'TrophyOutlined', 'FlagOutlined', 'LinkOutlined',
  'ApiOutlined', 'NodeIndexOutlined', 'PartitionOutlined',
  'ReadOutlined', 'BookOutlined', 'HistoryOutlined',
  'ReconciliationOutlined', 'ContainerOutlined', 'SwitcherOutlined',
];

/** 图标名称到组件的映射 */
const iconMap = Icons as unknown as Record<string, React.ComponentType>;

interface IconSelectorProps {
  /** 当前选中的图标名称 */
  value?: string;
  /** 选中图标时的回调 */
  onChange?: (iconName: string) => void;
}

const IconSelector: React.FC<IconSelectorProps> = ({ value, onChange }) => {
  const [keyword, setKeyword] = useState('');

  /** 根据关键词过滤图标列表 */
  const filteredIcons = useMemo(() => {
    if (!keyword.trim()) return ICON_LIST;
    const lower = keyword.toLowerCase();
    return ICON_LIST.filter((name) => name.toLowerCase().includes(lower));
  }, [keyword]);

  /** 渲染单个图标 */
  const renderIcon = (name: string) => {
    const IconComponent = iconMap[name];
    if (!IconComponent) return null;
    return <IconComponent />;
  };

  return (
    <div>
      {/* 搜索框 */}
      <Input.Search
        placeholder="搜索图标名称"
        allowClear
        value={keyword}
        onChange={(e) => setKeyword(e.target.value)}
        style={{ marginBottom: 12 }}
      />

      {/* 图标网格 */}
      {filteredIcons.length > 0 ? (
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(8, 1fr)',
            gap: 8,
            maxHeight: 320,
            overflowY: 'auto',
          }}
        >
          {filteredIcons.map((name) => (
            <div
              key={name}
              title={name}
              onClick={() => onChange?.(name)}
              style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                padding: '8px 4px',
                borderRadius: 6,
                cursor: 'pointer',
                fontSize: 22,
                border: value === name ? '2px solid #1677ff' : '1px solid #f0f0f0',
                backgroundColor: value === name ? '#e6f4ff' : '#fafafa',
                transition: 'all 0.2s',
              }}
              onMouseEnter={(e) => {
                if (value !== name) {
                  e.currentTarget.style.borderColor = '#1677ff';
                  e.currentTarget.style.backgroundColor = '#f0f5ff';
                }
              }}
              onMouseLeave={(e) => {
                if (value !== name) {
                  e.currentTarget.style.borderColor = '#f0f0f0';
                  e.currentTarget.style.backgroundColor = '#fafafa';
                }
              }}
            >
              {renderIcon(name)}
              <span
                style={{
                  fontSize: 10,
                  marginTop: 4,
                  color: '#666',
                  maxWidth: '100%',
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                  whiteSpace: 'nowrap',
                  textAlign: 'center',
                }}
              >
                {name.replace('Outlined', '')}
              </span>
            </div>
          ))}
        </div>
      ) : (
        <Empty description="未找到匹配的图标" image={Empty.PRESENTED_IMAGE_SIMPLE} />
      )}
    </div>
  );
};

export default IconSelector;
