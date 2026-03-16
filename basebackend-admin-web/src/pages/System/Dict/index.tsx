/**
 * 字典管理页面
 * 提供字典类型列表展示、新增/编辑字典类型、查看字典数据、刷新缓存等功能
 * 点击字典类型行可展开查看该类型下的字典数据子表
 * 使用 ProTable 实现服务端分页和搜索过滤
 */
import { useRef, useState } from 'react';
import {
  Button,
  Space,
  Modal,
  Tag,
  message,
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  ExclamationCircleOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import {
  ProTable,
  ModalForm,
  ProFormText,
  ProFormSelect,
  ProFormTextArea,
  ProFormDigit,
} from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { dictApi } from '@/api/dictApi';
import { Permission } from '@/components/Permission';
import type { DictTypeDTO, DictDataDTO } from '@/types';

/**
 * 字典数据子表组件
 * 展示指定字典类型下的所有字典数据条目
 */
const DictDataTable: React.FC<{ dictType: string }> = ({ dictType }) => {
  const actionRef = useRef<ActionType>();
  /** 字典数据表单可见状态 */
  const [dataFormOpen, setDataFormOpen] = useState(false);
  /** 当前编辑的字典数据（null 表示新增） */
  const [editingData, setEditingData] = useState<DictDataDTO | null>(null);

  /** 打开新增字典数据表单 */
  const handleAddData = () => {
    setEditingData(null);
    setDataFormOpen(true);
  };

  /** 打开编辑字典数据表单 */
  const handleEditData = (record: DictDataDTO) => {
    setEditingData(record);
    setDataFormOpen(true);
  };

  /** 删除字典数据 */
  const handleDeleteData = (record: DictDataDTO) => {
    Modal.confirm({
      title: '确认删除',
      icon: <ExclamationCircleOutlined />,
      content: `确定要删除字典数据「${record.dictLabel}」吗？`,
      okText: '确定',
      cancelText: '取消',
      onOk: async () => {
        await dictApi.deleteData(record.id);
        message.success('删除成功');
        actionRef.current?.reload();
      },
    });
  };

  /** 提交新增/编辑字典数据表单 */
  const handleDataFormSubmit = async (values: Partial<DictDataDTO>) => {
    const submitData = { ...values, dictType };
    if (editingData) {
      await dictApi.updateData(editingData.id, submitData);
      message.success('更新成功');
    } else {
      await dictApi.createData(submitData);
      message.success('创建成功');
    }
    actionRef.current?.reload();
    return true;
  };

  /** 字典数据表格列定义 */
  const dataColumns: ProColumns<DictDataDTO>[] = [
    {
      title: '字典标签',
      dataIndex: 'dictLabel',
      ellipsis: true,
    },
    {
      title: '字典值',
      dataIndex: 'dictValue',
      ellipsis: true,
    },
    {
      title: '排序',
      dataIndex: 'dictSort',
      width: 80,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (_, record) => (
        <Tag color={record.status === 1 ? 'success' : 'error'}>
          {record.status === 1 ? '启用' : '禁用'}
        </Tag>
      ),
    },
    {
      title: '备注',
      dataIndex: 'remark',
      ellipsis: true,
    },
    {
      title: '操作',
      valueType: 'option',
      width: 120,
      render: (_, record) => (
        <Space>
          <Permission code="system:dict:edit">
            <Button
              type="link"
              icon={<EditOutlined />}
              size="small"
              onClick={() => handleEditData(record)}
            />
          </Permission>
          <Permission code="system:dict:delete">
            <Button
              type="link"
              danger
              icon={<DeleteOutlined />}
              size="small"
              onClick={() => handleDeleteData(record)}
            />
          </Permission>
        </Space>
      ),
    },
  ];

  return (
    <>
      <ProTable<DictDataDTO>
        actionRef={actionRef}
        rowKey="id"
        columns={dataColumns}
        request={async () => {
          const data = await dictApi.dataByType(dictType);
          return { data, success: true, total: data.length };
        }}
        pagination={false}
        search={false}
        size="small"
        toolBarRender={() => [
          <Permission key="add" code="system:dict:add">
            <Button
              type="primary"
              icon={<PlusOutlined />}
              size="small"
              onClick={handleAddData}
            >
              新增数据
            </Button>
          </Permission>,
        ]}
      />

      {/* 新增/编辑字典数据弹窗表单 */}
      <ModalForm<Partial<DictDataDTO>>
        title={editingData ? '编辑字典数据' : '新增字典数据'}
        open={dataFormOpen}
        onOpenChange={setDataFormOpen}
        initialValues={
          editingData
            ? {
                dictLabel: editingData.dictLabel,
                dictValue: editingData.dictValue,
                dictSort: editingData.dictSort,
                status: editingData.status,
                remark: editingData.remark,
              }
            : { status: 1, dictSort: 0 }
        }
        modalProps={{ destroyOnClose: true }}
        onFinish={handleDataFormSubmit}
      >
        <ProFormText
          name="dictLabel"
          label="字典标签"
          placeholder="请输入字典标签"
          rules={[{ required: true, message: '请输入字典标签' }]}
        />
        <ProFormText
          name="dictValue"
          label="字典值"
          placeholder="请输入字典值"
          rules={[{ required: true, message: '请输入字典值' }]}
        />
        <ProFormDigit
          name="dictSort"
          label="排序"
          placeholder="请输入排序号"
          min={0}
          fieldProps={{ precision: 0 }}
          rules={[{ required: true, message: '请输入排序号' }]}
        />
        <ProFormSelect
          name="status"
          label="状态"
          options={[
            { label: '启用', value: 1 },
            { label: '禁用', value: 0 },
          ]}
          rules={[{ required: true, message: '请选择状态' }]}
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

/**
 * 字典类型管理主页面
 * 展示字典类型列表，支持展开行查看字典数据子表
 */
const DictPage: React.FC = () => {
  const actionRef = useRef<ActionType>();
  /** 字典类型表单可见状态 */
  const [typeFormOpen, setTypeFormOpen] = useState(false);
  /** 当前编辑的字典类型（null 表示新增） */
  const [editingType, setEditingType] = useState<DictTypeDTO | null>(null);

  /** 打开新增字典类型表单 */
  const handleAdd = () => {
    setEditingType(null);
    setTypeFormOpen(true);
  };

  /** 打开编辑字典类型表单 */
  const handleEdit = (record: DictTypeDTO) => {
    setEditingType(record);
    setTypeFormOpen(true);
  };

  /** 删除字典类型 */
  const handleDelete = (record: DictTypeDTO) => {
    Modal.confirm({
      title: '确认删除',
      icon: <ExclamationCircleOutlined />,
      content: `确定要删除字典类型「${record.dictName}」吗？`,
      okText: '确定',
      cancelText: '取消',
      onOk: async () => {
        await dictApi.deleteType(record.id);
        message.success('删除成功');
        actionRef.current?.reload();
      },
    });
  };

  /** 刷新字典缓存 */
  const handleRefreshCache = async () => {
    await dictApi.refreshCache();
    message.success('缓存刷新成功');
  };

  /** 提交新增/编辑字典类型表单 */
  const handleTypeFormSubmit = async (values: Partial<DictTypeDTO>) => {
    if (editingType) {
      await dictApi.updateType(editingType.id, values);
      message.success('更新成功');
    } else {
      await dictApi.createType(values);
      message.success('创建成功');
    }
    actionRef.current?.reload();
    return true;
  };

  /** 字典类型表格列定义 */
  const columns: ProColumns<DictTypeDTO>[] = [
    {
      title: '字典名称',
      dataIndex: 'dictName',
      ellipsis: true,
    },
    {
      title: '字典类型',
      dataIndex: 'dictType',
      ellipsis: true,
    },
    {
      title: '状态',
      dataIndex: 'status',
      valueType: 'select',
      valueEnum: {
        0: { text: '禁用', status: 'Error' },
        1: { text: '启用', status: 'Success' },
      },
      width: 100,
    },
    {
      title: '备注',
      dataIndex: 'remark',
      hideInSearch: true,
      ellipsis: true,
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      valueType: 'dateTime',
      hideInSearch: true,
      width: 180,
    },
    {
      title: '操作',
      valueType: 'option',
      width: 120,
      render: (_, record) => (
        <Space>
          <Permission code="system:dict:edit">
            <Button
              type="link"
              icon={<EditOutlined />}
              size="small"
              onClick={() => handleEdit(record)}
            />
          </Permission>
          <Permission code="system:dict:delete">
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
      <ProTable<DictTypeDTO>
        headerTitle="字典管理"
        actionRef={actionRef}
        rowKey="id"
        columns={columns}
        expandable={{
          expandedRowRender: (record) => (
            <DictDataTable dictType={record.dictType} />
          ),
        }}
        request={async (params) => {
          const { current = 1, pageSize = 10, ...query } = params;
          const res = await dictApi.typePage({
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
          <Permission key="add" code="system:dict:add">
            <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
              新增
            </Button>
          </Permission>,
          <Permission key="refresh" code="system:dict:edit">
            <Button icon={<ReloadOutlined />} onClick={handleRefreshCache}>
              刷新缓存
            </Button>
          </Permission>,
        ]}
      />

      {/* 新增/编辑字典类型弹窗表单 */}
      <ModalForm<Partial<DictTypeDTO>>
        title={editingType ? '编辑字典类型' : '新增字典类型'}
        open={typeFormOpen}
        onOpenChange={setTypeFormOpen}
        initialValues={
          editingType
            ? {
                dictName: editingType.dictName,
                dictType: editingType.dictType,
                status: editingType.status,
                remark: editingType.remark,
              }
            : { status: 1 }
        }
        modalProps={{ destroyOnClose: true }}
        onFinish={handleTypeFormSubmit}
      >
        <ProFormText
          name="dictName"
          label="字典名称"
          placeholder="请输入字典名称"
          rules={[{ required: true, message: '请输入字典名称' }]}
        />
        <ProFormText
          name="dictType"
          label="字典类型"
          placeholder="请输入字典类型标识"
          rules={[{ required: true, message: '请输入字典类型标识' }]}
          disabled={!!editingType}
        />
        <ProFormSelect
          name="status"
          label="状态"
          options={[
            { label: '启用', value: 1 },
            { label: '禁用', value: 0 },
          ]}
          rules={[{ required: true, message: '请选择状态' }]}
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

export default DictPage;
