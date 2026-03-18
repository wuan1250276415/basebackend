/**
 * 菜单管理页面
 * 提供菜单/权限的树形列表展示、新增/编辑菜单、展开/折叠控制、删除确认等功能
 * 使用 ProTable 的树形数据模式展示菜单层级结构
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
  FolderOutlined,
  MenuOutlined,
  ControlOutlined,
} from '@ant-design/icons';
import {
  ProTable,
  ModalForm,
  ProFormText,
  ProFormDigit,
  ProFormSelect,
} from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import * as Icons from '@ant-design/icons';
import { menuApi } from '@/api/menuApi';
import { Permission } from '@/components/Permission';
import IconSelector from '@/components/IconSelector';
import { listToTree } from '@/utils/tree';
import type { MenuItem } from '@/types';

/** 菜单类型选项 */
const MENU_TYPE_OPTIONS = [
  { label: '目录', value: 0 },
  { label: '菜单', value: 1 },
  { label: '按钮', value: 2 },
];

/** 菜单类型标签颜色映射 */
const TYPE_TAG_MAP: Record<number, { color: string; label: string }> = {
  0: { color: 'blue', label: '目录' },
  1: { color: 'green', label: '菜单' },
  2: { color: 'orange', label: '按钮' },
};

/** 图标名称到组件的映射 */
const iconMap = Icons as unknown as Record<string, React.ComponentType>;

/** 渲染图标组件 */
const renderIcon = (iconName: string) => {
  if (!iconName) return null;
  const IconComponent = iconMap[iconName];
  if (!IconComponent) return null;
  return <IconComponent />;
};

/**
 * 将菜单树转换为 TreeSelect 数据（用于父菜单选择）
 * 仅包含目录和菜单类型，排除按钮类型
 */
const buildTreeSelectData = (menus: MenuItem[]): any[] => {
  return menus
    .filter((m) => m.type !== 2)
    .map((m) => ({
      value: m.id,
      title: m.name,
      children: m.children ? buildTreeSelectData(m.children) : undefined,
    }));
};

/**
 * 检查菜单节点是否有子节点
 */
const hasChildren = (record: MenuItem): boolean => {
  return Array.isArray(record.children) && record.children.length > 0;
};

const MenuPage: React.FC = () => {
  const actionRef = useRef<ActionType>();
  /** 模态表单可见状态 */
  const [modalOpen, setModalOpen] = useState(false);
  /** 当前编辑的菜单（null 表示新增） */
  const [editingMenu, setEditingMenu] = useState<MenuItem | null>(null);
  /** 菜单树数据（用于父菜单选择） */
  const [menuTree, setMenuTree] = useState<MenuItem[]>([]);
  /** 展开的行 key 列表 */
  const [expandedRowKeys, setExpandedRowKeys] = useState<number[]>([]);
  /** 是否全部展开 */
  const [allExpanded, setAllExpanded] = useState(false);
  /** 新增时的默认父菜单 ID */
  const [defaultParentId, setDefaultParentId] = useState<number | undefined>(undefined);

  /** 收集所有节点 ID（用于全部展开） */
  const collectAllKeys = useCallback((menus: MenuItem[]): number[] => {
    const keys: number[] = [];
    const traverse = (items: MenuItem[]) => {
      for (const item of items) {
        if (item.children && item.children.length > 0) {
          keys.push(item.id);
          traverse(item.children);
        }
      }
    };
    traverse(menus);
    return keys;
  }, []);

  /** 切换全部展开/折叠 */
  const toggleExpandAll = useCallback(() => {
    if (allExpanded) {
      setExpandedRowKeys([]);
      setAllExpanded(false);
    } else {
      setExpandedRowKeys(collectAllKeys(menuTree));
      setAllExpanded(true);
    }
  }, [allExpanded, menuTree, collectAllKeys]);

  /** 打开新增表单 */
  const handleAdd = (parentId?: number) => {
    setEditingMenu(null);
    setDefaultParentId(parentId);
    setModalOpen(true);
  };

  /** 打开编辑表单 */
  const handleEdit = (record: MenuItem) => {
    setEditingMenu(record);
    setDefaultParentId(undefined);
    setModalOpen(true);
  };

  /** 删除菜单 */
  const handleDelete = (record: MenuItem) => {
    const hasChild = hasChildren(record);
    Modal.confirm({
      title: '确认删除',
      icon: <ExclamationCircleOutlined />,
      content: hasChild
        ? `菜单「${record.name}」包含子菜单，删除后子菜单也将被删除，确定要继续吗？`
        : `确定要删除菜单「${record.name}」吗？`,
      okText: '确定',
      okButtonProps: hasChild ? { danger: true } : undefined,
      cancelText: '取消',
      onOk: async () => {
        await menuApi.delete(record.id);
        message.success('删除成功');
        actionRef.current?.reload();
      },
    });
  };

  /** 提交新增/编辑表单 */
  const handleFormSubmit = async (values: Record<string, any>) => {
    if (editingMenu) {
      await menuApi.update(editingMenu.id, values);
      message.success('更新成功');
    } else {
      await menuApi.create(values);
      message.success('创建成功');
    }
    setModalOpen(false);
    actionRef.current?.reload();
    return true;
  };

  /** TreeSelect 数据（用于父菜单选择） */
  const treeSelectData = useMemo(() => {
    return [
      { value: 0, title: '根目录', children: buildTreeSelectData(menuTree) },
    ];
  }, [menuTree]);

  /** 表格列定义 */
  const columns: ProColumns<MenuItem>[] = [
    {
      title: '菜单名称',
      dataIndex: 'name',
      ellipsis: true,
      width: 220,
    },
    {
      title: '图标',
      dataIndex: 'icon',
      hideInSearch: true,
      width: 80,
      align: 'center',
      render: (_, record) => renderIcon(record.icon),
    },
    {
      title: '权限标识',
      dataIndex: 'permissionKey',
      ellipsis: true,
      width: 200,
    },
    {
      title: '类型',
      dataIndex: 'type',
      hideInSearch: true,
      width: 80,
      align: 'center',
      render: (_, record) => {
        const tag = TYPE_TAG_MAP[record.type];
        return tag ? <Tag color={tag.color}>{tag.label}</Tag> : '-';
      },
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
      title: '操作',
      valueType: 'option',
      width: 150,
      render: (_, record) => (
        <Space>
          <Permission code="system:menu:edit">
            <Button
              type="link"
              icon={<EditOutlined />}
              size="small"
              onClick={() => handleEdit(record)}
            />
          </Permission>
          {record.type !== 2 && (
            <Permission code="system:menu:add">
              <Button
                type="link"
                icon={<PlusOutlined />}
                size="small"
                title="添加子菜单"
                onClick={() => handleAdd(record.id)}
              />
            </Permission>
          )}
          <Permission code="system:menu:delete">
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
      <ProTable<MenuItem>
        headerTitle="菜单管理"
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
          /* 获取扁平权限列表并转换为树形结构 */
          const list = await menuApi.list();
          /* 将 PermissionDTO 映射为 MenuItem 格式 */
          const menuItems: MenuItem[] = list.map((item: any) => ({
            id: item.id,
            parentId: item.parentId ?? 0,
            name: item.permissionName ?? item.name,
            icon: item.icon ?? '',
            path: item.path ?? '',
            permissionKey: item.permissionKey ?? '',
            type: item.type ?? item.permissionType ?? 0,
            orderNum: item.orderNum ?? 0,
            status: item.status ?? 1,
          }));
          const tree = listToTree(menuItems as unknown as Record<string, unknown>[]) as unknown as MenuItem[];
          setMenuTree(tree);
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
          <Permission key="add" code="system:menu:add">
            <Button type="primary" icon={<PlusOutlined />} onClick={() => handleAdd()}>
              新增
            </Button>
          </Permission>,
        ]}
      />

      {/* 新增/编辑菜单模态表单 */}
      <ModalForm
        title={editingMenu ? '编辑菜单' : '新增菜单'}
        open={modalOpen}
        onOpenChange={setModalOpen}
        initialValues={
          editingMenu
            ? {
                parentId: editingMenu.parentId,
                name: editingMenu.name,
                icon: editingMenu.icon,
                permissionKey: editingMenu.permissionKey,
                type: editingMenu.type,
                path: editingMenu.path,
                orderNum: editingMenu.orderNum,
                status: editingMenu.status,
              }
            : {
                parentId: defaultParentId ?? 0,
                type: 0,
                orderNum: 0,
                status: 1,
              }
        }
        modalProps={{ destroyOnClose: true }}
        onFinish={handleFormSubmit}
        width={600}
      >
        <Form.Item
          name="parentId"
          label="上级菜单"
          rules={[{ required: true, message: '请选择上级菜单' }]}
        >
          <TreeSelect
            treeData={treeSelectData}
            showSearch
            treeDefaultExpandAll
            treeLine
            placeholder="请选择上级菜单"
            fieldNames={{ label: 'title', value: 'value', children: 'children' }}
          />
        </Form.Item>
        <ProFormText
          name="name"
          label="菜单名称"
          placeholder="请输入菜单名称"
          rules={[{ required: true, message: '请输入菜单名称' }]}
        />
        <ProFormSelect
          name="type"
          label="菜单类型"
          options={MENU_TYPE_OPTIONS}
          rules={[{ required: true, message: '请选择菜单类型' }]}
          fieldProps={{
            optionItemRender: (item: any) => {
              const iconEl =
                item.value === 0 ? <FolderOutlined /> :
                item.value === 1 ? <MenuOutlined /> :
                <ControlOutlined />;
              return (
                <Space>
                  {iconEl}
                  {item.label}
                </Space>
              );
            },
          }}
        />
        <ProFormText
          name="icon"
          label="图标"
          placeholder="请选择图标"
        >
          <IconSelector />
        </ProFormText>
        <ProFormText
          name="permissionKey"
          label="权限标识"
          placeholder="请输入权限标识，如 system:menu:list"
        />
        <ProFormText
          name="path"
          label="路由路径"
          placeholder="请输入路由路径，如 /system/menu"
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

export default MenuPage;
