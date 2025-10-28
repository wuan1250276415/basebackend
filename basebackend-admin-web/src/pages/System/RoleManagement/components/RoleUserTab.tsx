import React, { useState, useEffect } from 'react'
import { Table, Button, Input, Space, Popconfirm, message } from 'antd'
import { PlusOutlined, SearchOutlined, DeleteOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table/interface'
import { User } from '@/types'
import { getRoleUsers, assignUsersToRole, removeUserFromRole } from '@/api/role'
import UserSelectModal from './UserSelectModal'

interface RoleUserTabProps {
  roleId: string
}

const RoleUserTab: React.FC<RoleUserTabProps> = ({ roleId }) => {
  const [loading, setLoading] = useState(false)
  const [userList, setUserList] = useState<User[]>([])
  const [searchValue, setSearchValue] = useState('')
  const [selectModalVisible, setSelectModalVisible] = useState(false)

  // 加载角色用户列表
  const loadRoleUsers = async (username = '') => {
    if (!roleId) return

    setLoading(true)
    try {
      const res = await getRoleUsers(roleId, username)
      if (res.code === 200) {
        setUserList(res.data || [])
      }
    } catch (error) {
      message.error('加载用户列表失败')
      console.error(error)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadRoleUsers('')
    setSearchValue('')
  }, [roleId])

  // 搜索用户
  const handleSearch = (value: string) => {
    setSearchValue(value)
    loadRoleUsers(value)
  }

  // 关联用户
  const handleAssignUsers = async (userIds: string[]) => {
    try {
      await assignUsersToRole(roleId, userIds)
      message.success(`成功关联 ${userIds.length} 个用户`)
      setSelectModalVisible(false)
      loadRoleUsers(searchValue)
    } catch (error: any) {
      message.error(error.response?.data?.message || '关联用户失败')
    }
  }

  // 取消关联
  const handleRemoveUser = async (userId: string) => {
    try {
      await removeUserFromRole(roleId, userId)
      message.success('已取消关联')
      loadRoleUsers(searchValue)
    } catch (error: any) {
      message.error(error.response?.data?.message || '取消关联失败')
    }
  }

  const columns: ColumnsType<User> = [
    {
      title: '用户名',
      dataIndex: 'username',
      key: 'username',
      width: 120,
    },
    {
      title: '昵称',
      dataIndex: 'nickname',
      key: 'nickname',
      width: 120,
    },
    {
      title: '邮箱',
      dataIndex: 'email',
      key: 'email',
      width: 180,
    },
    {
      title: '手机号',
      dataIndex: 'phone',
      key: 'phone',
      width: 120,
    },
    {
      title: '部门',
      dataIndex: 'deptName',
      key: 'deptName',
      width: 150,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 80,
      render: (status: number) => (
        <span style={{ color: status === 1 ? '#52c41a' : '#ff4d4f' }}>
          {status === 1 ? '启用' : '禁用'}
        </span>
      ),
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 180,
    },
    {
      title: '操作',
      key: 'action',
      width: 100,
      fixed: 'right',
      render: (_, record) => (
        <Popconfirm
          title="确定要取消该用户的角色关联吗？"
          onConfirm={() => handleRemoveUser(record.id!)}
          okText="确定"
          cancelText="取消"
        >
          <Button type="link" danger size="small" icon={<DeleteOutlined />}>
            取消关联
          </Button>
        </Popconfirm>
      ),
    },
  ]

  return (
    <div>
      <Space style={{ marginBottom: 16 }}>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={() => setSelectModalVisible(true)}
        >
          关联用户
        </Button>
        <Input
          placeholder="搜索用户名"
          prefix={<SearchOutlined />}
          allowClear
          style={{ width: 250 }}
          value={searchValue}
          onChange={(e) => setSearchValue(e.target.value)}
          onPressEnter={(e) => handleSearch((e.target as HTMLInputElement).value)}
        />
      </Space>

      <Table
        rowKey="id"
        columns={columns}
        dataSource={userList}
        loading={loading}
        pagination={{
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: (total) => `共 ${total} 条`,
        }}
        scroll={{ x: 1200 }}
      />

      <UserSelectModal
        visible={selectModalVisible}
        onCancel={() => setSelectModalVisible(false)}
        onConfirm={handleAssignUsers}
        excludeUserIds={userList.map((user) => user.id!)}
      />
    </div>
  )
}

export default RoleUserTab
