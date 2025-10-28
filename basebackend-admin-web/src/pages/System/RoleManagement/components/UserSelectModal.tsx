import React, { useState, useEffect } from 'react'
import { Modal, Table, Input, Space, message } from 'antd'
import { SearchOutlined } from '@ant-design/icons'
import type { ColumnsType, TableRowSelection } from 'antd/es/table/interface'
import { User } from '@/types'
import request from '@/utils/request'

interface UserSelectModalProps {
  visible: boolean
  onCancel: () => void
  onConfirm: (selectedUserIds: string[]) => void
  excludeUserIds?: string[] // 已关联的用户ID，需要排除
}

const UserSelectModal: React.FC<UserSelectModalProps> = ({
  visible,
  onCancel,
  onConfirm,
  excludeUserIds = [],
}) => {
  const [loading, setLoading] = useState(false)
  const [userList, setUserList] = useState<User[]>([])
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([])
  const [searchValue, setSearchValue] = useState('')
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  })

  // 加载用户列表
  const loadUserList = async (page = 1, size = 10, username = '') => {
    setLoading(true)
    try {
      const res = await request.get('/admin/users', {
        params: {
          current: page,
          size,
          username,
        },
      })

      if (res.code === 200) {
        const data = res.data
        // 过滤掉已关联的用户
        const filteredUsers = (data.records || []).filter(
          (user: User) => !excludeUserIds.includes(user.id!)
        )
        setUserList(filteredUsers)
        setPagination({
          current: data.current,
          pageSize: data.size,
          total: data.total,
        })
      }
    } catch (error) {
      message.error('加载用户列表失败')
      console.error(error)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (visible) {
      setSelectedRowKeys([])
      loadUserList(1, 10, '')
    }
  }, [visible, excludeUserIds])

  // 搜索
  const handleSearch = (value: string) => {
    setSearchValue(value)
    loadUserList(1, pagination.pageSize, value)
  }

  // 分页变化
  const handleTableChange = (newPagination: any) => {
    loadUserList(newPagination.current, newPagination.pageSize, searchValue)
  }

  // 行选择
  const rowSelection: TableRowSelection<User> = {
    selectedRowKeys,
    onChange: (selectedKeys) => {
      setSelectedRowKeys(selectedKeys)
    },
  }

  // 确认选择
  const handleConfirm = () => {
    if (selectedRowKeys.length === 0) {
      message.warning('请至少选择一个用户')
      return
    }
    onConfirm(selectedRowKeys as string[])
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
      width: 150,
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
      width: 120,
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
  ]

  return (
    <Modal
      title="选择用户"
      open={visible}
      onCancel={onCancel}
      onOk={handleConfirm}
      width={900}
      destroyOnClose
    >
      <Space direction="vertical" style={{ width: '100%' }} size="middle">
        <Input
          placeholder="搜索用户名"
          prefix={<SearchOutlined />}
          allowClear
          onPressEnter={(e) => handleSearch((e.target as HTMLInputElement).value)}
          onChange={(e) => {
            if (!e.target.value) {
              handleSearch('')
            }
          }}
          style={{ width: 300 }}
        />

        <Table
          rowKey="id"
          columns={columns}
          dataSource={userList}
          loading={loading}
          rowSelection={rowSelection}
          pagination={{
            ...pagination,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条`,
          }}
          onChange={handleTableChange}
          scroll={{ y: 400 }}
        />

        <div style={{ color: '#999', fontSize: 12 }}>
          已选择 {selectedRowKeys.length} 个用户
        </div>
      </Space>
    </Modal>
  )
}

export default UserSelectModal
