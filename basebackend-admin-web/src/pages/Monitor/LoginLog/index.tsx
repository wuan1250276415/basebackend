import { useState, useEffect } from 'react'
import { Card, Table, Button, Space, Input, Select, message, Modal, Tag, DatePicker } from 'antd'
import { SearchOutlined, DeleteOutlined, EyeOutlined, ClearOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { LoginLog } from '@/types'
import {
  getLoginLogPage,
  getLoginLogById,
  deleteLoginLog,
  deleteLoginLogBatch,
  cleanLoginLog,
} from '@/api/log'
import dayjs from 'dayjs'

const { RangePicker } = DatePicker

const LoginLogPage = () => {
  const [loading, setLoading] = useState(false)
  const [dataSource, setDataSource] = useState<LoginLog[]>([])
  const [total, setTotal] = useState(0)
  const [current, setCurrent] = useState(1)
  const [pageSize, setPageSize] = useState(10)
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([])
  const [detailModalVisible, setDetailModalVisible] = useState(false)
  const [currentLog, setCurrentLog] = useState<LoginLog | null>(null)

  // 搜索条件
  const [username, setUsername] = useState('')
  const [ipAddress, setIpAddress] = useState('')
  const [status, setStatus] = useState<number | undefined>(undefined)
  const [dateRange, setDateRange] = useState<[string, string] | null>(null)

  useEffect(() => {
    loadData()
  }, [current, pageSize])

  const loadData = async () => {
    setLoading(true)
    try {
      const params: any = {
        current,
        size: pageSize,
      }
      if (username) params.username = username
      if (ipAddress) params.ipAddress = ipAddress
      if (status !== undefined) params.status = status
      if (dateRange) {
        params.beginTime = dateRange[0]
        params.endTime = dateRange[1]
      }

      const response = await getLoginLogPage(params)
      const result = response.data
      setDataSource(result.records || [])
      setTotal(result.total || 0)
    } catch (error: any) {
      message.error(error.message || '加载登录日志失败')
    } finally {
      setLoading(false)
    }
  }

  const handleSearch = () => {
    setCurrent(1)
    loadData()
  }

  const handleReset = () => {
    setUsername('')
    setIpAddress('')
    setStatus(undefined)
    setDateRange(null)
    setCurrent(1)
    loadData()
  }

  const handleViewDetail = async (record: LoginLog) => {
    try {
      const response = await getLoginLogById(record.id!)
      setCurrentLog(response.data)
      setDetailModalVisible(true)
    } catch (error: any) {
      message.error(error.message || '获取日志详情失败')
    }
  }

  const handleDelete = (id: string) => {
    Modal.confirm({
      title: '确认删除',
      content: '确定要删除这条登录日志吗?',
      onOk: async () => {
        try {
          await deleteLoginLog(id)
          message.success('删除成功')
          loadData()
        } catch (error: any) {
          message.error(error.message || '删除失败')
        }
      },
    })
  }

  const handleBatchDelete = () => {
    if (selectedRowKeys.length === 0) {
      message.warning('请先选择要删除的日志')
      return
    }
    Modal.confirm({
      title: '确认删除',
      content: `确定要删除选中的 ${selectedRowKeys.length} 条登录日志吗?`,
      onOk: async () => {
        try {
          await deleteLoginLogBatch(selectedRowKeys as string[])
          message.success('批量删除成功')
          setSelectedRowKeys([])
          loadData()
        } catch (error: any) {
          message.error(error.message || '批量删除失败')
        }
      },
    })
  }

  const handleClean = () => {
    Modal.confirm({
      title: '确认清空',
      content: '确定要清空所有登录日志吗? 此操作不可恢复!',
      okType: 'danger',
      onOk: async () => {
        try {
          await cleanLoginLog()
          message.success('清空成功')
          loadData()
        } catch (error: any) {
          message.error(error.message || '清空失败')
        }
      },
    })
  }

  const columns: ColumnsType<LoginLog> = [
    {
      title: '访问编号',
      dataIndex: 'id',
      key: 'id',
      width: 180,
      ellipsis: true,
    },
    {
      title: '用户名',
      dataIndex: 'username',
      key: 'username',
      width: 120,
    },
    {
      title: '登录地址',
      dataIndex: 'ipAddress',
      key: 'ipAddress',
      width: 140,
    },
    {
      title: '登录地点',
      dataIndex: 'loginLocation',
      key: 'loginLocation',
      width: 150,
      ellipsis: true,
    },
    {
      title: '浏览器',
      dataIndex: 'browser',
      key: 'browser',
      width: 120,
      ellipsis: true,
    },
    {
      title: '操作系统',
      dataIndex: 'os',
      key: 'os',
      width: 120,
      ellipsis: true,
    },
    {
      title: '登录状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: number) => (
        <Tag color={status === 1 ? 'success' : 'error'}>
          {status === 1 ? '成功' : '失败'}
        </Tag>
      ),
    },
    {
      title: '提示消息',
      dataIndex: 'msg',
      key: 'msg',
      width: 150,
      ellipsis: true,
    },
    {
      title: '登录时间',
      dataIndex: 'loginTime',
      key: 'loginTime',
      width: 180,
      render: (text: string) => text ? dayjs(text).format('YYYY-MM-DD HH:mm:ss') : '-',
    },
    {
      title: '操作',
      key: 'action',
      width: 150,
      fixed: 'right',
      render: (_: any, record: LoginLog) => (
        <Space size="small">
          <Button
            type="link"
            size="small"
            icon={<EyeOutlined />}
            onClick={() => handleViewDetail(record)}
          >
            详情
          </Button>
          <Button
            type="link"
            size="small"
            danger
            icon={<DeleteOutlined />}
            onClick={() => handleDelete(record.id!)}
          >
            删除
          </Button>
        </Space>
      ),
    },
  ]

  return (
    <Card title="登录日志">
      <Space direction="vertical" size="middle" style={{ display: 'flex' }}>
        {/* 搜索栏 */}
        <Space wrap>
          <Input
            placeholder="用户名"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            style={{ width: 150 }}
            allowClear
          />
          <Input
            placeholder="登录地址"
            value={ipAddress}
            onChange={(e) => setIpAddress(e.target.value)}
            style={{ width: 150 }}
            allowClear
          />
          <Select
            placeholder="登录状态"
            value={status}
            onChange={setStatus}
            style={{ width: 120 }}
            allowClear
          >
            <Select.Option value={1}>成功</Select.Option>
            <Select.Option value={0}>失败</Select.Option>
          </Select>
          <RangePicker
            value={dateRange ? [dayjs(dateRange[0]), dayjs(dateRange[1])] : null}
            onChange={(dates) => {
              if (dates) {
                setDateRange([
                  dates[0]!.format('YYYY-MM-DD HH:mm:ss'),
                  dates[1]!.format('YYYY-MM-DD HH:mm:ss'),
                ])
              } else {
                setDateRange(null)
              }
            }}
            showTime
          />
          <Button
            type="primary"
            icon={<SearchOutlined />}
            onClick={handleSearch}
          >
            搜索
          </Button>
          <Button onClick={handleReset}>重置</Button>
        </Space>

        {/* 操作按钮 */}
        <Space>
          <Button
            danger
            icon={<DeleteOutlined />}
            onClick={handleBatchDelete}
            disabled={selectedRowKeys.length === 0}
          >
            批量删除
          </Button>
          <Button
            danger
            icon={<ClearOutlined />}
            onClick={handleClean}
          >
            清空
          </Button>
        </Space>

        {/* 表格 */}
        <Table
          rowKey="id"
          columns={columns}
          dataSource={dataSource}
          loading={loading}
          rowSelection={{
            selectedRowKeys,
            onChange: setSelectedRowKeys,
          }}
          scroll={{ x: 1500 }}
          pagination={{
            current,
            pageSize,
            total,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条`,
            onChange: (page, size) => {
              setCurrent(page)
              setPageSize(size)
            },
          }}
        />
      </Space>

      {/* 详情弹窗 */}
      <Modal
        title="登录日志详情"
        open={detailModalVisible}
        onCancel={() => setDetailModalVisible(false)}
        footer={[
          <Button key="close" onClick={() => setDetailModalVisible(false)}>
            关闭
          </Button>,
        ]}
        width={600}
      >
        {currentLog && (
          <div style={{ lineHeight: 2 }}>
            <p><strong>访问编号：</strong>{currentLog.id}</p>
            <p><strong>用户名：</strong>{currentLog.username}</p>
            <p><strong>登录地址：</strong>{currentLog.ipAddress}</p>
            <p><strong>登录地点：</strong>{currentLog.loginLocation || '-'}</p>
            <p><strong>浏览器：</strong>{currentLog.browser || '-'}</p>
            <p><strong>操作系统：</strong>{currentLog.os || '-'}</p>
            <p>
              <strong>登录状态：</strong>
              <Tag color={currentLog.status === 1 ? 'success' : 'error'}>
                {currentLog.status === 1 ? '成功' : '失败'}
              </Tag>
            </p>
            <p><strong>提示消息：</strong>{currentLog.msg || '-'}</p>
            <p><strong>登录时间：</strong>{currentLog.loginTime ? dayjs(currentLog.loginTime).format('YYYY-MM-DD HH:mm:ss') : '-'}</p>
          </div>
        )}
      </Modal>
    </Card>
  )
}

export default LoginLogPage
