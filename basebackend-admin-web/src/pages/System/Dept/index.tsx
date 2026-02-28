/**
 * 部门管理页面
 * 提供部门的树形列表展示、新增/编辑部门、展开/折叠控制、删除确认等功能
 * 使用 ProTable 的树形数据模式展示部门层级结构
 */
import { useRef, useState, useCallback, useMemo } from 'react';
import {
  Button,
  Space,
  Modal,
  Tag,
  TreeSelect,
  Form,
  message,
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  ExclamationCircleOutlined,
  DownOutlined,
  RightOutlined,
} from '@ant-design/icons';
import {
  ProTable,
  ModalForm,
  ProFormText,
  ProFormDigit,
  ProFormSelect,
} from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { deptApi } from '@/api/deptApi';
import { Permission } from '@/components/Permission';
import type { DeptDTO } from '@/types';

/**
 * 将部门树转换为 TreeSelect 数据（用于父部门选择）
 */
const buildTreeSelectData = (depts: DeptDTO[]): any[] => {
  return depts.map((d) => ({
    value: d.id,
    title: d.deptName,
    children: d.children ? buildTreeSelectData(d.children) : undefined,
  }));
};

/**
 * 检查部门节点是否有子节点
 */
const hasChildren = (record: DeptDTO): boolean => {
  return Array.isArray(record.children) && record.children.length > 0;
};

const DeptPage: React.FC = () => {
  const actionRef = useRef<ActionType>();
  /** 模态表单可见状态 */
  const [modalOpen, setModalOpen] = useState(false);
  /** 当前编辑的部门（null 表示新增） */
  const [editingDept, setEditingDept] = useState<DeptDTO | null>(null);
  /** 部门树数据（用于父部门选择和表格展示） */
  const [deptTree, setDeptTree] = useState<DeptDTO[]>([]);
  /** 展开的行 key 列表 */
  const [expandedRowKeys, setExpandedRowKeys] = useState<number[]>([]);
  /** 是否全部展开 */
  const [allExpanded, setAllExpanded] = useState(false);
  /** 新增时的默认父部门 ID */
  const [defaultParentId, setDefaultParentId] = useState<number | undefined>(undefined);

  /** 收集所有节点 ID（用于全部展开） */
  const collectAllKeys = useCallback((depts: DeptDTO[]): number[] => {
    const keys: number[] = [];
    const traverse = (items: DeptDTO[]) => {
      for (const item of items) {
        if (item.children && item.children.length > 0) {
          keys.push(item.id);
          traverse(item.children);
        }
      }
    };
    traverse(depts);
    return keys;
  }, []);

  /** 切换全部展开/折叠 */
  const toggleExpandAll = useCallback(() => {
    if (allExpanded) {
      setExpandedRowKeys([]);
      setAllExpanded(false);
    } else {
      setExpandedRowKeys(collectAllKeys(deptTree));
      setAllExpanded(true);
    }
  }, [allExpanded, deptTree, collectAllKeys]);

  /** 打开新增表单 */
  const handleAdd = (parentId?: number) => {
    setEditingDept(null);
    setDefaultParentId(parentId);
    setModalOpen(true);
  };

  /** 打开编辑表单 */
  const handleEdit = (record: DeptDTO) => {
    setEditingDept(record);
    setDefaultParentId(undefined);
    setModalOpen(true);
  };

  /** 删除部门 */
  const handleDelete = (record: DeptDTO) => {
    const hasChild = hasChildren(record);
    Modal.confirm({
      title: '确认删除',
      icon: <ExclamationCircleOutlined />,
      content: hasChild
        ? `部门「${record.deptName}」包含子部门，删除后子部门也将被删除，确定要继续吗？`
        : `确定要删除部门「${record.deptName}」吗？`,
      okText: '确定',
      okButtonProps: hasChild ? { danger: true } : undefined,
      cancelText: '取消',
      onOk: async () => {
        await deptApi.delete(record.id);
        message.success('删除成功');
        actionRef.current?.reload();
      },
    });
  };

  /** 提交新增/编辑表单 */
  const handleFormSubmit = async (values: Record<string, any>) => {
    if (editingDept) {
      await deptApi.update(editingDept.id, values);
      message.success('更新成功');
    } else {
      await deptApi.create(values);
      message.success('创建成功');
    }
    setModalOpen(false);
    actionRef.current?.reload();
    return true;
  };

  /** TreeSelect 数据（用于父部门选择） */
  const treeSelectData = useMemo(() => {
    return [
      { value: 0, title: '根部门', children: buildTreeSelectData(deptTree) },
    ];
  }, [deptTree]);

  /** 校验部门名称唯一性 */
  const validateDeptName = async (_: any, value: string) => {
    if (!value) return;
    const parentId = editingDept?.parentId ?? defaultParentId ?? 0;
    try {
      const exists = await deptApi.checkName({ deptName: value, parentId });
      /* 编辑时如果名称未变则跳过校验 */
      if (editingDept && editingDept.deptName === value) return;
      if (exists) {
        throw new Error('同级部门下已存在相同名称');
      }
    } catch (err: any) {
      if (err.message === '同级部门下已存在相同名称') {
        return Promise.reject(err.message);
      }
      /* 接口异常时不阻塞表单提交 */
    }
  };

  /** 表格列定义 */
  const columns: ProColumns<DeptDTO>[] = [
    {
      title: '部门名称',
      dataIndex: 'deptName',
      ellipsis: true,
      width: 220,
    },
    {
      title: '部门编码',
      dataIndex: 'id',
      hideInSearch: true,
      width: 100,
      align: 'center',
    },
    {
      title: '排序',
      dataIndex: 'orderNum',
      hideInSearch: true,
      width: 80,
      align: 'center',
    },
    {
      title: '状态',
      dataIndex: 'status',
      hideInSearch: true,
      width: 80,
      align: 'center',
      render: (_, record) => (
        <Tag color={record.status === 1 ? 'success' : 'error'}>
          {record.status === 1 ? '启用' : '禁用'}
        </Tag>
      ),
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      hideInSearch: true,
      width: 180,
    },
    {
      title: '操作',
      valueType: 'option',
      width: 150,
      render: (_, record) => (
        <Space>
          <Permission code="system:dept:edit">
            <Button
              type="link"
              icon={<EditOutlined />}
              size="small"
              onClick={() => handleEdit(record)}
            />
          </Permission>
          <Permission code="system:dept:add">
            <Button
              type="link"
              icon={<PlusOutlined />}
              size="small"
              title="添加子部门"
              onClick={() => handleAdd(record.id)}
            />
          </Permission>
          <Permission code="system:dept:delete">
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
      <ProTable<DeptDTO>
        headerTitle="部门管理"
        actionRef={actionRef}
        rowKey="id"
        columns={columns}
        search={false}
        pagination={false}
        expandable={{
          expandedRowKeys,
          onExpandedRowsChange: (keys) => {
            setExpandedRowKeys(keys as number[]);
            setAllExpanded(false);
          },
        }}
        request={async () => {
          /* 获取部门树数据 */
          const tree = await deptApi.tree();
          setDeptTree(tree);
          return {
            data: tree,
            success: true,
          };
        }}
        toolBarRender={() => [
          <Button
            key="expand"
            icon={allExpanded ? <DownOutlined /> : <RightOutlined />}
            onClick={toggleExpandAll}
          >
            {allExpanded ? '全部折叠' : '全部展开'}
          </Button>,
          <Permission key="add" code="system:dept:add">
            <Button type="primary" icon={<PlusOutlined />} onClick={() => handleAdd()}>
              新增
            </Button>
          </Permission>,
        ]}
      />

      {/* 新增/编辑部门模态表单 */}
      <ModalForm
        title={editingDept ? '编辑部门' : '新增部门'}
        open={modalOpen}
        onOpenChange={setModalOpen}
        initialValues={
          editingDept
            ? {
                parentId: editingDept.parentId,
                deptName: editingDept.deptName,
                orderNum: editingDept.orderNum,
                status: editingDept.status,
              }
            : {
                parentId: defaultParentId ?? 0,
                orderNum: 0,
                status: 1,
              }
        }
        modalProps={{ destroyOnClose: true }}
        onFinish={handleFormSubmit}
        width={500}
      >
        <Form.Item
          name="parentId"
          label="上级部门"
          rules={[{ required: true, message: '请选择上级部门' }]}
        >
          <TreeSelect
            treeData={treeSelectData}
            showSearch
            treeDefaultExpandAll
            treeLine
            placeholder="请选择上级部门"
            fieldNames={{ label: 'title', value: 'value', children: 'children' }}
          />
        </Form.Item>
        <ProFormText
          name="deptName"
          label="部门名称"
          placeholder="请输入部门名称"
          rules={[
            { required: true, message: '请输入部门名称' },
            { validator: validateDeptName },
          ]}
        />
        <ProFormText
          name="deptCode"
          label="部门编码"
          placeholder="请输入部门编码"
        />
        <ProFormDigit
          name="orderNum"
          label="排序号"
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
      </ModalForm>
    </>
  );
};

export default DeptPage;
