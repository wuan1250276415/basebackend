/**
 * 系统配置管理页面
 * 提供系统参数的列表展示、新增、编辑、删除功能
 * 使用 ProTable 实现服务端分页和搜索过滤
 */
import { useRef, useState } from 'react';
import { Button, Space, Modal, Tag, message } from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  ExclamationCircleOutlined,
} from '@ant-design/icons';
import {
  ProTable,
  ModalForm,
  ProFormText,
  ProFormSelect,
  ProFormTextArea,
} from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { configApi } from '@/api/configApi';
import { Permission } from '@/components/Permission';
import type { ConfigDTO } from '@/types';

/**
 * 系统配置管理主页面
 * 展示配置参数列表，支持新增/编辑/删除操作
 */
const ConfigPage: React.FC = () => {
  const actionRef = useRef<ActionType>();
  /** 表单可见状态 */
  const [formOpen, setFormOpen] = useState(false);
  /** 当前编辑的配置（null 表示新增） */
  const [editing, setEditing] = useState<ConfigDTO | null>(null);

  /** 打开新增表单 */
  const handleAdd = () => {
    setEditing(null);
    setFormOpen(true);
  };

  /** 打开编辑表单 */
  const handleEdit = (record: ConfigDTO) => {
    setEditing(record);
    setFormOpen(true);
  };

  /** 删除配置参数 */
  const handleDelete = (record: ConfigDTO) => {
    Modal.confirm({
      title: '确认删除',
      icon: <ExclamationCircleOutlined />,
      content: `确定要删除配置「${record.configName}」吗？`,
      okText: '确定',
      cancelText: '取消',
      onOk: async () => {
        await configApi.delete(record.id);
        message.success('删除成功');
        actionRef.current?.reload();
      },
    });
  };

  /** 提交新增/编辑表单 */
  const handleFormSubmit = async (values: Partial<ConfigDTO>) => {
    if (editing) {
      await configApi.update(editing.id, values);
      message.success('更新成功');
    } else {
      await configApi.create(values);
      message.success('创建成功');
    }
    actionRef.current?.reload();
    return true;
  };

  /** 表格列定义 */
  const columns: ProColumns<ConfigDTO>[] = [
    {
      title: '参数名称',
      dataIndex: 'configName',
      ellipsis: true,
    },
    {
      title: '参数键名',
      dataIndex: 'configKey',
      ellipsis: true,
    },
    {
      title: '参数键值',
      dataIndex: 'configValue',
      hideInSearch: true,
      ellipsis: true,
    },
    {
      title: '类型',
      dataIndex: 'configType',
      valueType: 'select',
      valueEnum: {
        0: { text: '系统内置', status: 'Processing' },
        1: { text: '自定义', status: 'Default' },
      },
      width: 120,
      render: (_, record) => (
        <Tag color={record.configType === 0 ? 'blue' : 'default'}>
          {record.configType === 0 ? '系统内置' : '自定义'}
        </Tag>
      ),
    },
    {
      title: '备注',
      dataIndex: 'remark',
      hideInSearch: true,
      ellipsis: true,
    },
    {
      title: '操作',
      valueType: 'option',
      width: 120,
      render: (_, record) => (
        <Space>
          <Permission code="system:config:edit">
            <Button
              type="link"
              icon={<EditOutlined />}
              size="small"
              onClick={() => handleEdit(record)}
            />
          </Permission>
          <Permission code="system:config:delete">
            <Button
              type="link"
              danger
              icon={<DeleteOutlined />}
              size="small"
              onClick={() => handleDelete(record)}
            />
          </Permission>
        </Space>
      ),
    },
  ];

  return (
    <>
      <ProTable<ConfigDTO>
        headerTitle="系统配置"
        actionRef={actionRef}
        rowKey="id"
        columns={columns}
        request={async (params) => {
          const { current = 1, pageSize = 10, ...query } = params;
          const res = await configApi.page({
            current,
            size: pageSize,
            ...query,
          });
          return {
            data: res.records,
            total: res.total,
            success: true,
          };
        }}
        pagination={{ defaultPageSize: 10, showSizeChanger: true }}
        search={{ labelWidth: 'auto' }}
        toolBarRender={() => [
          <Permission key="add" code="system:config:add">
            <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
              新增
            </Button>
          </Permission>,
        ]}
      />

      {/* 新增/编辑配置参数弹窗表单 */}
      <ModalForm<Partial<ConfigDTO>>
        title={editing ? '编辑配置参数' : '新增配置参数'}
        open={formOpen}
        onOpenChange={setFormOpen}
        initialValues={
          editing
            ? {
                configName: editing.configName,
                configKey: editing.configKey,
                configValue: editing.configValue,
                configType: editing.configType,
                remark: editing.remark,
              }
            : { configType: 1 }
        }
        modalProps={{ destroyOnClose: true }}
        onFinish={handleFormSubmit}
      >
        <ProFormText
          name="configName"
          label="参数名称"
          placeholder="请输入参数名称"
          rules={[{ required: true, message: '请输入参数名称' }]}
        />
        <ProFormText
          name="configKey"
          label="参数键名"
          placeholder="请输入参数键名"
          rules={[{ required: true, message: '请输入参数键名' }]}
        />
        <ProFormText
          name="configValue"
          label="参数键值"
          placeholder="请输入参数键值"
          rules={[{ required: true, message: '请输入参数键值' }]}
        />
        <ProFormSelect
          name="configType"
          label="类型"
          options={[
            { label: '系统内置', value: 0 },
            { label: '自定义', value: 1 },
          ]}
          rules={[{ required: true, message: '请选择配置类型' }]}
        />
        <ProFormTextArea
          name="remark"
          label="备注"
          placeholder="请输入备注"
          fieldProps={{ rows: 3 }}
        />
      </ModalForm>
    </>
  );
};

export default ConfigPage;
