/**
 * 用户管理页面
 * 提供用户列表展示、新增/编辑用户、分配角色、重置密码、状态切换等功能
 * 使用 ProTable 实现服务端分页和搜索过滤
 */
import { useRef, useState } from 'react';
import {
  Button,
  Switch,
  Dropdown,
  Modal,
  Checkbox,
  Space,
  message,
  Input,
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  MoreOutlined,
  ExclamationCircleOutlined,
  UserSwitchOutlined,
  KeyOutlined,
} from '@ant-design/icons';
import { ProTable, DrawerForm, ProFormText, ProFormSelect } from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { userApi } from '@/api/userApi';
import { roleApi } from '@/api/roleApi';
import { Permission } from '@/components/Permission';
import DeptTreeSelect from '@/components/DeptTreeSelect';
import type { UserDTO, UserCreateDTO, RoleDTO } from '@/types';

const UserPage: React.FC = () => {
  const actionRef = useRef<ActionType>();
  /** 抽屉表单可见状态 */
  const [drawerOpen, setDrawerOpen] = useState(false);
  /** 当前编辑的用户（null 表示新增） */
  const [editingUser, setEditingUser] = useState<UserDTO | null>(null);
  /** 分配角色弹窗可见状态 */
  const [roleModalOpen, setRoleModalOpen] = useState(false);
  /** 当前分配角色的用户 */
  const [roleTargetUser, setRoleTargetUser] = useState<UserDTO | null>(null);
  /** 可选角色列表 */
  const [roleList, setRoleList] = useState<RoleDTO[]>([]);
  /** 已选角色 ID 列表 */
  const [selectedRoleIds, setSelectedRoleIds] = useState<number[]>([]);
  /** 角色加载状态 */
  const [roleLoading, setRoleLoading] = useState(false);

  /** 打开新增抽屉 */
  const handleAdd = () => {
    setEditingUser(null);
    setDrawerOpen(true);
  };

  /** 打开编辑抽屉 */
  const handleEdit = (record: UserDTO) => {
    setEditingUser(record);
    setDrawerOpen(true);
  };

  /** 删除用户 */
  const handleDelete = (record: UserDTO) => {
    Modal.confirm({
      title: '确认删除',
      icon: <ExclamationCircleOutlined />,
      content: `确定要删除用户「${record.username}」吗？`,
      okText: '确定',
      cancelText: '取消',
      onOk: async () => {
        await userApi.delete(record.id);
        message.success('删除成功');
        actionRef.current?.reload();
      },
    });
  };

  /** 切换用户状态 */
  const handleStatusChange = async (record: UserDTO, checked: boolean) => {
    const newStatus = checked ? 1 : 0;
    await userApi.changeStatus(record.id, newStatus);
    message.success(checked ? '已启用' : '已禁用');
    actionRef.current?.reload();
  };

  /** 打开分配角色弹窗 */
  const handleAssignRoles = async (record: UserDTO) => {
    setRoleTargetUser(record);
    setSelectedRoleIds(record.roleIds ?? []);
    setRoleLoading(true);
    setRoleModalOpen(true);
    try {
      const res = await roleApi.page({ current: 1, size: 1000 });
      setRoleList(res.records ?? []);
    } finally {
      setRoleLoading(false);
    }
  };

  /** 提交分配角色 */
  const handleRoleSubmit = async () => {
    if (!roleTargetUser) return;
    await userApi.assignRoles(roleTargetUser.id, selectedRoleIds);
    message.success('角色分配成功');
    setRoleModalOpen(false);
    actionRef.current?.reload();
  };

  /** 重置密码 */
  const handleResetPassword = (record: UserDTO) => {
    let newPassword = '';
    Modal.confirm({
      title: '重置密码',
      icon: <KeyOutlined />,
      content: (
        <div style={{ marginTop: 16 }}>
          <p>确定要重置用户「{record.username}」的密码吗？</p>
          <Input.Password
            placeholder="请输入新密码"
            onChange={(e) => {
              newPassword = e.target.value;
            }}
          />
        </div>
      ),
      okText: '确定',
      cancelText: '取消',
      onOk: async () => {
        if (!newPassword) {
          message.warning('请输入新密码');
          return Promise.reject();
        }
        await userApi.resetPassword(record.id, newPassword);
        message.success('密码重置成功');
      },
    });
  };

  /** 提交新增/编辑表单 */
  const handleFormSubmit = async (values: UserCreateDTO & { id?: number }) => {
    if (editingUser) {
      await userApi.update(editingUser.id, values);
      message.success('更新成功');
    } else {
      await userApi.create(values);
      message.success('创建成功');
    }
    setDrawerOpen(false);
    actionRef.current?.reload();
    return true;
  };

  /** 表格列定义 */
  const columns: ProColumns<UserDTO>[] = [
    {
      title: '用户名',
      dataIndex: 'username',
      ellipsis: true,
    },
    {
      title: '昵称',
      dataIndex: 'nickname',
      ellipsis: true,
    },
    {
      title: '部门',
      dataIndex: 'deptName',
      hideInSearch: true,
      ellipsis: true,
    },
    {
      title: '手机号',
      dataIndex: 'phone',
      ellipsis: true,
    },
    {
      title: '邮箱',
      dataIndex: 'email',
      hideInSearch: true,
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
      render: (_, record) => (
        <Permission code="system:user:edit">
          <Switch
            checked={record.status === 1}
            checkedChildren="启用"
            unCheckedChildren="禁用"
            onChange={(checked) => handleStatusChange(record, checked)}
          />
        </Permission>
      ),
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
      width: 150,
      render: (_, record) => (
        <Space>
          <Permission code="system:user:edit">
            <Button
              type="link"
              icon={<EditOutlined />}
              size="small"
              onClick={() => handleEdit(record)}
            />
          </Permission>
          <Permission code="system:user:delete">
            <Button
              type="link"
              danger
              icon={<DeleteOutlined />}
              size="small"
              onClick={() => handleDelete(record)}
            />
          </Permission>
          <Dropdown
            menu={{
              items: [
                {
                  key: 'assignRoles',
                  icon: <UserSwitchOutlined />,
                  label: '分配角色',
                  onClick: () => handleAssignRoles(record),
                },
                {
                  key: 'resetPassword',
                  icon: <KeyOutlined />,
                  label: '重置密码',
                  onClick: () => handleResetPassword(record),
                },
              ],
            }}
            trigger={['click']}
          >
            <Button type="link" icon={<MoreOutlined />} size="small" />
          </Dropdown>
        </Space>
      ),
    },
  ];

  return (
    <>
      <ProTable<UserDTO>
        headerTitle="用户管理"
        actionRef={actionRef}
        rowKey="id"
        columns={columns}
        request={async (params) => {
          const { current = 1, pageSize = 10, ...query } = params;
          const res = await userApi.page({
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
          <Permission key="add" code="system:user:add">
            <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
              新增
            </Button>
          </Permission>,
        ]}
      />

      {/* 新增/编辑用户抽屉表单 */}
      <DrawerForm<UserCreateDTO & { id?: number }>
        title={editingUser ? '编辑用户' : '新增用户'}
        open={drawerOpen}
        onOpenChange={setDrawerOpen}
        initialValues={
          editingUser
            ? {
                username: editingUser.username,
                nickname: editingUser.nickname,
                phone: editingUser.phone,
                email: editingUser.email,
                deptId: editingUser.deptId,
                status: editingUser.status,
              }
            : { status: 1 }
        }
        drawerProps={{ destroyOnClose: true }}
        onFinish={handleFormSubmit}
      >
        <ProFormText
          name="username"
          label="用户名"
          placeholder="请输入用户名"
          rules={[{ required: true, message: '请输入用户名' }]}
          disabled={!!editingUser}
        />
        <ProFormText
          name="nickname"
          label="昵称"
          placeholder="请输入昵称"
          rules={[{ required: true, message: '请输入昵称' }]}
        />
        {!editingUser && (
          <ProFormText.Password
            name="password"
            label="密码"
            placeholder="请输入密码"
            rules={[{ required: true, message: '请输入密码' }]}
          />
        )}
        <ProFormText
          name="deptId"
          label="部门"
          rules={[{ required: false }]}
        >
          <DeptTreeSelect />
        </ProFormText>
        <ProFormText
          name="phone"
          label="手机号"
          placeholder="请输入手机号"
          rules={[
            {
              pattern: /^1[3-9]\d{9}$/,
              message: '请输入正确的手机号',
            },
          ]}
        />
        <ProFormText
          name="email"
          label="邮箱"
          placeholder="请输入邮箱"
          rules={[
            {
              type: 'email',
              message: '请输入正确的邮箱地址',
            },
          ]}
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
      </DrawerForm>

      {/* 分配角色弹窗 */}
      <Modal
        title={`分配角色 - ${roleTargetUser?.username ?? ''}`}
        open={roleModalOpen}
        onCancel={() => setRoleModalOpen(false)}
        onOk={handleRoleSubmit}
        confirmLoading={roleLoading}
        destroyOnClose
      >
        <Checkbox.Group
          value={selectedRoleIds}
          onChange={(values) => setSelectedRoleIds(values as number[])}
          style={{ display: 'flex', flexDirection: 'column', gap: 8 }}
        >
          {roleList.map((role) => (
            <Checkbox key={role.id} value={role.id}>
              {role.roleName}（{role.roleKey}）
            </Checkbox>
          ))}
        </Checkbox.Group>
      </Modal>
    </>
  );
};

export default UserPage;
