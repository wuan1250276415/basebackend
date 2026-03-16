/**
 * 角色管理页面
 * 提供角色列表展示、新增/编辑角色、分配权限、分配菜单、数据权限配置等功能
 * 使用 ProTable 实现服务端分页和搜索过滤
 */
import { useRef, useState, useCallback } from 'react';
import {
  Button,
  Space,
  Modal,
  Tree,
  Select,
  message,
  Spin,
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  ExclamationCircleOutlined,
  SafetyOutlined,
  MenuOutlined,
  DatabaseOutlined,
} from '@ant-design/icons';
import {
  ProTable,
  DrawerForm,
  ProFormText,
  ProFormDigit,
  ProFormSelect,
  ProFormTextArea,
} from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { roleApi } from '@/api/roleApi';
import { menuApi } from '@/api/menuApi';
import { Permission } from '@/components/Permission';
import type { RoleDTO, PermissionDTO, MenuItem } from '@/types';

/** 数据范围选项 */
const DATA_SCOPE_OPTIONS = [
  { label: '全部数据', value: 1 },
  { label: '自定义数据', value: 2 },
  { label: '本部门数据', value: 3 },
  { label: '本部门及以下数据', value: 4 },
];

/**
 * 将权限列表转换为树形结构（用于 Tree 组件）
 */
const buildPermissionTree = (permissions: PermissionDTO[]) => {
  return permissions.map((p) => ({
    key: p.id,
    title: `${p.permissionName}（${p.permissionKey}）`,
  }));
};

/**
 * 将菜单列表转换为树形结构（用于 Tree 组件）
 */
const buildMenuTree = (menus: MenuItem[]): any[] => {
  return menus.map((m) => ({
    key: m.id,
    title: m.name,
    children: m.children ? buildMenuTree(m.children) : undefined,
  }));
};

const RolePage: React.FC = () => {
  const actionRef = useRef<ActionType>();
  /** 抽屉表单可见状态 */
  const [drawerOpen, setDrawerOpen] = useState(false);
  /** 当前编辑的角色（null 表示新增） */
  const [editingRole, setEditingRole] = useState<RoleDTO | null>(null);

  /** 分配权限弹窗状态 */
  const [permModalOpen, setPermModalOpen] = useState(false);
  const [permTargetRole, setPermTargetRole] = useState<RoleDTO | null>(null);
  const [permTreeData, setPermTreeData] = useState<any[]>([]);
  const [checkedPermIds, setCheckedPermIds] = useState<number[]>([]);
  const [permLoading, setPermLoading] = useState(false);

  /** 分配菜单弹窗状态 */
  const [menuModalOpen, setMenuModalOpen] = useState(false);
  const [menuTargetRole, setMenuTargetRole] = useState<RoleDTO | null>(null);
  const [menuTreeData, setMenuTreeData] = useState<any[]>([]);
  const [checkedMenuIds, setCheckedMenuIds] = useState<number[]>([]);
  const [menuLoading, setMenuLoading] = useState(false);

  /** 数据权限弹窗状态 */
  const [dataScopeModalOpen, setDataScopeModalOpen] = useState(false);
  const [dataScopeRole, setDataScopeRole] = useState<RoleDTO | null>(null);
  const [dataScope, setDataScope] = useState<number>(1);

  /** 打开新增抽屉 */
  const handleAdd = () => {
    setEditingRole(null);
    setDrawerOpen(true);
  };

  /** 打开编辑抽屉 */
  const handleEdit = (record: RoleDTO) => {
    setEditingRole(record);
    setDrawerOpen(true);
  };

  /** 删除角色 */
  const handleDelete = (record: RoleDTO) => {
    Modal.confirm({
      title: '确认删除',
      icon: <ExclamationCircleOutlined />,
      content: `确定要删除角色「${record.roleName}」吗？`,
      okText: '确定',
      cancelText: '取消',
      onOk: async () => {
        await roleApi.delete(record.id);
        message.success('删除成功');
        actionRef.current?.reload();
      },
    });
  };

  /** 提交新增/编辑表单 */
  const handleFormSubmit = async (values: Partial<RoleDTO>) => {
    if (editingRole) {
      await roleApi.update(editingRole.id, values);
      message.success('更新成功');
    } else {
      await roleApi.create(values);
      message.success('创建成功');
    }
    setDrawerOpen(false);
    actionRef.current?.reload();
    return true;
  };

  /** 打开分配权限弹窗 */
  const handleAssignPermissions = useCallback(async (record: RoleDTO) => {
    setPermTargetRole(record);
    setPermLoading(true);
    setPermModalOpen(true);
    try {
      // 并行获取全部权限列表和角色当前权限
      const [allPerms, rolePerms] = await Promise.all([
        menuApi.list(),
        menuApi.getByRole(record.id),
      ]);
      setPermTreeData(buildPermissionTree(allPerms));
      setCheckedPermIds(rolePerms.map((p) => p.id));
    } finally {
      setPermLoading(false);
    }
  }, []);

  /** 提交分配权限 */
  const handlePermSubmit = async () => {
    if (!permTargetRole) return;
    await roleApi.assignPermissions(permTargetRole.id, checkedPermIds);
    message.success('权限分配成功');
    setPermModalOpen(false);
    actionRef.current?.reload();
  };

  /** 打开分配菜单弹窗 */
  const handleAssignMenus = useCallback(async (record: RoleDTO) => {
    setMenuTargetRole(record);
    setMenuLoading(true);
    setMenuModalOpen(true);
    try {
      // 获取全部菜单树（通过用户菜单接口获取完整菜单树）
      const allMenus = await menuApi.getByUser(0);
      setMenuTreeData(buildMenuTree(allMenus));
      // 预选当前角色的菜单
      const roleDetail = await roleApi.getById(record.id);
      setCheckedMenuIds(roleDetail.menuIds ?? []);
    } finally {
      setMenuLoading(false);
    }
  }, []);

  /** 提交分配菜单 */
  const handleMenuSubmit = async () => {
    if (!menuTargetRole) return;
    await roleApi.assignMenus(menuTargetRole.id, checkedMenuIds);
    message.success('菜单分配成功');
    setMenuModalOpen(false);
    actionRef.current?.reload();
  };

  /** 打开数据权限弹窗 */
  const handleDataPermissions = (record: RoleDTO) => {
    setDataScopeRole(record);
    setDataScope(record.dataScope ?? 1);
    setDataScopeModalOpen(true);
  };

  /** 提交数据权限 */
  const handleDataScopeSubmit = async () => {
    if (!dataScopeRole) return;
    await roleApi.manageDataPermissions(dataScopeRole.id, dataScope);
    message.success('数据权限设置成功');
    setDataScopeModalOpen(false);
    actionRef.current?.reload();
  };

  /** 表格列定义 */
  const columns: ProColumns<RoleDTO>[] = [
    {
      title: '角色名称',
      dataIndex: 'roleName',
      ellipsis: true,
    },
    {
      title: '角色标识',
      dataIndex: 'roleKey',
      ellipsis: true,
    },
    {
      title: '排序',
      dataIndex: 'roleSort',
      hideInSearch: true,
      width: 80,
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
      title: '创建时间',
      dataIndex: 'createTime',
      valueType: 'dateTime',
      hideInSearch: true,
      width: 180,
    },
    {
      title: '操作',
      valueType: 'option',
      width: 200,
      render: (_, record) => (
        <Space>
          <Permission code="system:role:edit">
            <Button
              type="link"
              icon={<EditOutlined />}
              size="small"
              onClick={() => handleEdit(record)}
            />
          </Permission>
          <Permission code="system:role:delete">
            <Button
              type="link"
              danger
              icon={<DeleteOutlined />}
              size="small"
              onClick={() => handleDelete(record)}
            />
          </Permission>
          <Permission code="system:role:edit">
            <Button
              type="link"
              icon={<SafetyOutlined />}
              size="small"
              title="分配权限"
              onClick={() => handleAssignPermissions(record)}
            />
          </Permission>
          <Permission code="system:role:edit">
            <Button
              type="link"
              icon={<MenuOutlined />}
              size="small"
              title="分配菜单"
              onClick={() => handleAssignMenus(record)}
            />
          </Permission>
          <Permission code="system:role:edit">
            <Button
              type="link"
              icon={<DatabaseOutlined />}
              size="small"
              title="数据权限"
              onClick={() => handleDataPermissions(record)}
            />
          </Permission>
        </Space>
      ),
    },
  ];

  return (
    <>
      <ProTable<RoleDTO>
        headerTitle="角色管理"
        actionRef={actionRef}
        rowKey="id"
        columns={columns}
        request={async (params) => {
          const { current = 1, pageSize = 10, ...query } = params;
          const res = await roleApi.page({
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
          <Permission key="add" code="system:role:add">
            <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
              新增
            </Button>
          </Permission>,
        ]}
      />

      {/* 新增/编辑角色抽屉表单 */}
      <DrawerForm<Partial<RoleDTO>>
        title={editingRole ? '编辑角色' : '新增角色'}
        open={drawerOpen}
        onOpenChange={setDrawerOpen}
        initialValues={
          editingRole
            ? {
                roleName: editingRole.roleName,
                roleKey: editingRole.roleKey,
                roleSort: editingRole.roleSort,
                status: editingRole.status,
                remark: editingRole.remark,
              }
            : { status: 1, roleSort: 0 }
        }
        drawerProps={{ destroyOnClose: true }}
        onFinish={handleFormSubmit}
      >
        <ProFormText
          name="roleName"
          label="角色名称"
          placeholder="请输入角色名称"
          rules={[{ required: true, message: '请输入角色名称' }]}
        />
        <ProFormText
          name="roleKey"
          label="角色标识"
          placeholder="请输入角色标识"
          rules={[{ required: true, message: '请输入角色标识' }]}
          disabled={!!editingRole}
        />
        <ProFormDigit
          name="roleSort"
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
      </DrawerForm>

      {/* 分配权限弹窗 */}
      <Modal
        title={`分配权限 - ${permTargetRole?.roleName ?? ''}`}
        open={permModalOpen}
        onCancel={() => setPermModalOpen(false)}
        onOk={handlePermSubmit}
        confirmLoading={permLoading}
        destroyOnClose
        width={500}
      >
        <Spin spinning={permLoading}>
          <Tree
            checkable
            checkedKeys={checkedPermIds}
            onCheck={(keys) => setCheckedPermIds(keys as number[])}
            treeData={permTreeData}
            style={{ maxHeight: 400, overflow: 'auto' }}
          />
        </Spin>
      </Modal>

      {/* 分配菜单弹窗 */}
      <Modal
        title={`分配菜单 - ${menuTargetRole?.roleName ?? ''}`}
        open={menuModalOpen}
        onCancel={() => setMenuModalOpen(false)}
        onOk={handleMenuSubmit}
        confirmLoading={menuLoading}
        destroyOnClose
        width={500}
      >
        <Spin spinning={menuLoading}>
          <Tree
            checkable
            checkedKeys={checkedMenuIds}
            onCheck={(keys) => setCheckedMenuIds(keys as number[])}
            treeData={menuTreeData}
            style={{ maxHeight: 400, overflow: 'auto' }}
          />
        </Spin>
      </Modal>

      {/* 数据权限弹窗 */}
      <Modal
        title={`数据权限 - ${dataScopeRole?.roleName ?? ''}`}
        open={dataScopeModalOpen}
        onCancel={() => setDataScopeModalOpen(false)}
        onOk={handleDataScopeSubmit}
        destroyOnClose
        width={400}
      >
        <div style={{ padding: '16px 0' }}>
          <span style={{ marginRight: 12 }}>数据范围：</span>
          <Select
            value={dataScope}
            onChange={setDataScope}
            options={DATA_SCOPE_OPTIONS}
            style={{ width: 200 }}
          />
        </div>
      </Modal>
    </>
  );
};

export default RolePage;
