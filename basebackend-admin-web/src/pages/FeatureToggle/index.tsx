import React, { useState } from 'react';
import {
  Card,
  Table,
  Button,
  Space,
  Tag,
  message,
  Switch,
  Input,
  Alert,
  Statistic,
  Row,
  Col,
} from 'antd';
import {
  ReloadOutlined,
  SearchOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  CloudServerOutlined,
} from '@ant-design/icons';
import { useAllFeatures, useFeatureToggleStatus } from '@/hooks/useFeatureToggle';
import { featureToggleApi } from '@/api/featureToggleApi';
import type { ColumnsType } from 'antd/es/table';

const { Search } = Input;

interface FeatureItem {
  name: string;
  enabled: boolean;
  description?: string;
}

/**
 * 特性开关管理页面
 */
const FeatureTogglePage: React.FC = () => {
  const [searchText, setSearchText] = useState('');
  const { features, loading, error, refresh: refreshFeatures } = useAllFeatures();
  const { status, refresh: refreshStatus } = useFeatureToggleStatus();

  const handleRefresh = async () => {
    try {
      await featureToggleApi.refresh();
      message.success('特性开关配置已刷新');
      refreshFeatures();
      refreshStatus();
    } catch (err) {
      message.error('刷新失败：' + (err as Error).message);
    }
  };

  // 转换为表格数据
  const dataSource: FeatureItem[] = Object.entries(features).map(([name, enabled]) => ({
    name,
    enabled,
    description: `特性开关: ${name}`,
  }));

  // 过滤数据
  const filteredData = searchText
    ? dataSource.filter((item) =>
        item.name.toLowerCase().includes(searchText.toLowerCase())
      )
    : dataSource;

  // 统计数据
  const totalFeatures = dataSource.length;
  const enabledFeatures = dataSource.filter((f) => f.enabled).length;
  const disabledFeatures = totalFeatures - enabledFeatures;

  const columns: ColumnsType<FeatureItem> = [
    {
      title: '特性名称',
      dataIndex: 'name',
      key: 'name',
      width: '40%',
      render: (text: string) => (
        <span style={{ fontFamily: 'monospace', fontWeight: 500 }}>{text}</span>
      ),
    },
    {
      title: '状态',
      dataIndex: 'enabled',
      key: 'enabled',
      width: '20%',
      render: (enabled: boolean) =>
        enabled ? (
          <Tag icon={<CheckCircleOutlined />} color="success">
            启用
          </Tag>
        ) : (
          <Tag icon={<CloseCircleOutlined />} color="default">
            禁用
          </Tag>
        ),
      filters: [
        { text: '启用', value: true },
        { text: '禁用', value: false },
      ],
      onFilter: (value, record) => record.enabled === value,
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true,
    },
  ];

  return (
    <div style={{ padding: '24px' }}>
      {/* 页面标题 */}
      <div style={{ marginBottom: '24px' }}>
        <h1>特性开关管理</h1>
        <p style={{ color: '#666' }}>
          查看和管理所有特性开关状态。特性开关由 {status?.provider} 提供支持。
        </p>
      </div>

      {/* 服务状态卡片 */}
      {status && (
        <Card style={{ marginBottom: '24px' }}>
          <Row gutter={16}>
            <Col span={8}>
              <Statistic
                title="服务状态"
                value={status.available ? '正常运行' : '不可用'}
                valueStyle={{ color: status.available ? '#3f8600' : '#cf1322' }}
                prefix={
                  status.available ? (
                    <CheckCircleOutlined />
                  ) : (
                    <CloseCircleOutlined />
                  )
                }
              />
            </Col>
            <Col span={8}>
              <Statistic
                title="提供商"
                value={status.provider}
                prefix={<CloudServerOutlined />}
              />
            </Col>
            <Col span={8}>
              <Statistic title="总特性数" value={totalFeatures} />
            </Col>
          </Row>
        </Card>
      )}

      {/* 统计卡片 */}
      <Row gutter={16} style={{ marginBottom: '24px' }}>
        <Col span={8}>
          <Card>
            <Statistic
              title="启用的特性"
              value={enabledFeatures}
              valueStyle={{ color: '#3f8600' }}
              prefix={<CheckCircleOutlined />}
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card>
            <Statistic
              title="禁用的特性"
              value={disabledFeatures}
              valueStyle={{ color: '#666' }}
              prefix={<CloseCircleOutlined />}
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card>
            <Statistic
              title="启用率"
              value={totalFeatures > 0 ? ((enabledFeatures / totalFeatures) * 100).toFixed(1) : 0}
              suffix="%"
            />
          </Card>
        </Col>
      </Row>

      {/* 错误提示 */}
      {error && (
        <Alert
          message="加载失败"
          description={error.message}
          type="error"
          closable
          style={{ marginBottom: '16px' }}
        />
      )}

      {/* 操作栏 */}
      <Card style={{ marginBottom: '16px' }}>
        <Space style={{ width: '100%', justifyContent: 'space-between' }}>
          <Search
            placeholder="搜索特性名称"
            allowClear
            enterButton={<SearchOutlined />}
            style={{ width: 400 }}
            onChange={(e) => setSearchText(e.target.value)}
            onSearch={setSearchText}
          />
          <Space>
            <Button icon={<ReloadOutlined />} onClick={refreshFeatures}>
              刷新列表
            </Button>
            <Button type="primary" icon={<ReloadOutlined />} onClick={handleRefresh}>
              刷新配置
            </Button>
          </Space>
        </Space>
      </Card>

      {/* 特性列表 */}
      <Card>
        <Table
          columns={columns}
          dataSource={filteredData}
          loading={loading}
          rowKey="name"
          pagination={{
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 个特性`,
            defaultPageSize: 10,
            pageSizeOptions: ['10', '20', '50', '100'],
          }}
        />
      </Card>

      {/* 底部说明 */}
      <Alert
        message="提示"
        description={
          <div>
            <p>
              • 特性开关状态由 {status?.provider} 服务管理，此页面仅用于查看
            </p>
            <p>
              • 要修改特性开关状态，请访问{' '}
              {status?.provider === 'Unleash' && (
                <a href="http://localhost:4242" target="_blank" rel="noopener noreferrer">
                  Unleash 控制台
                </a>
              )}
              {status?.provider === 'Flagsmith' && (
                <a href="http://localhost:8000" target="_blank" rel="noopener noreferrer">
                  Flagsmith 控制台
                </a>
              )}
            </p>
            <p>• 点击"刷新配置"按钮可以从服务器重新加载最新配置</p>
          </div>
        }
        type="info"
        style={{ marginTop: '24px' }}
      />
    </div>
  );
};

export default FeatureTogglePage;
