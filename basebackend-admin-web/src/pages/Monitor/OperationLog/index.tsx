import { useState, useEffect } from 'react'
import { Card, Table, Button, Space, Input, Select, message, Modal, Tag, DatePicker } from 'antd'
import { SearchOutlined, DeleteOutlined, EyeOutlined, ClearOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { OperationLog } from '@/types'
import {
  getOperationLogPage,
  getOperationLogById,
  deleteOperationLog,
  deleteOperationLogBatch,
  cleanOperationLog,
} from '@/api/log'
import dayjs from 'dayjs'

const { RangePicker } = DatePicker

const OperationLogPage = () => {
  const [loading, setLoading] = useState(false)
  const [dataSource, setDataSource] = useState<OperationLog[]>([])
  const [total, setTotal] = useState(0)
  const [current, setCurrent] = useState(1)
  const [pageSize, setPageSize] = useState(10)
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([])
  const [detailModalVisible, setDetailModalVisible] = useState(false)
  const [currentLog, setCurrentLog] = useState<OperationLog | null>(null)

  // 搜索条件
  const [username, setUsername] = useState('')
  const [operation, setOperation] = useState('')
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
      if (operation) params.operation = operation
      if (status !== undefined) params.status = status
      if (dateRange) {
        params.beginTime = dateRange[0]
        params.endTime = dateRange[1]
      }

      const response = await getOperationLogPage(params)
      const result = response.data
      setDataSource(result.records || [])
      setTotal(result.total || 0)
    } catch (error: any) {
      message.error(error.message || '加载操作日志失败')
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
    setOperation('')
    setStatus(undefined)
    setDateRange(null)
    setCurrent(1)
    loadData()
  }

  const handleViewDetail = async (record: OperationLog) => {
    try {
      const response = await getOperationLogById(record.id!)
      setCurrentLog(response.data)
      setDetailModalVisible(true)
    } catch (error: any) {
      message.error(error.message || '获取日志详情失败')
    }
  }

  const handleDelete = (id: string) => {
    Modal.confirm({
      title: '确认删除',
      content: '确定要删除这条操作日志吗?',
      onOk: async () => {
        try {
          await deleteOperationLog(id)
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
      content: `确定要删除选中的 ${selectedRowKeys.length} 条操作日志吗?`,
      onOk: async () => {
        try {
          await deleteOperationLogBatch(selectedRowKeys as string[])
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
      content: '确定要清空所有操作日志吗? 此操作不可恢复!',
      okType: 'danger',
      onOk: async () => {
        try {
          await cleanOperationLog()
          message.success('清空成功')
          loadData()
        } catch (error: any) {
          message.error(error.message || '清空失败')
        }
      },
    })
  }

  const columns: ColumnsType<OperationLog> = [
    {
      title: '日志编号',
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
      title: '操作模块',
      dataIndex: 'operation',
      key: 'operation',
      width: 150,
    },
    {
      title: '请求方法',
      dataIndex: 'method',
      key: 'method',
      width: 250,
      ellipsis: true,
    },
    {
      title: 'IP地址',
      dataIndex: 'ipAddress',
      key: 'ipAddress',
      width: 140,
    },
    {
      title: '操作地点',
      dataIndex: 'location',
      key: 'location',
      width: 150,
      ellipsis: true,
    },
    {
      title: '耗时(ms)',
      dataIndex: 'time',
      key: 'time',
      width: 100,
      render: (time: number) => time || '-',
    },
    {
      title: '操作状态',
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
      title: '操作时间',
      dataIndex: 'operationTime',
      key: 'operationTime',
      width: 180,
      render: (text: string) => text ? dayjs(text).format('YYYY-MM-DD HH:mm:ss') : '-',
    },
    {
      title: '操作',
      key: 'action',
      width: 150,
      fixed: 'right',
      render: (_: any, record: OperationLog) => (
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
    <Card title="操作日志">
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
            placeholder="操作模块"
            value={operation}
            onChange={(e) => setOperation(e.target.value)}
            style={{ width: 150 }}
            allowClear
          />
          <Select
            placeholder="操作状态"
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
          scroll={{ x: 1600 }}
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
        title="操作日志详情"
        open={detailModalVisible}
        onCancel={() => setDetailModalVisible(false)}
        footer={[
          <Button key="close" onClick={() => setDetailModalVisible(false)}>
            关闭
          </Button>,
        ]}
        width={800}
      >
        {currentLog && (
          <div style={{ lineHeight: 2 }}>
            <p><strong>日志编号：</strong>{currentLog.id}</p>
            <p><strong>用户名：</strong>{currentLog.username}</p>
            <p><strong>操作模块：</strong>{currentLog.operation}</p>
            <p><strong>请求方法：</strong><code style={{ padding: '2px 6px', background: '#f5f5f5', borderRadius: 3 }}>{currentLog.method}</code></p>
            <p><strong>请求参数：</strong></p>
            {currentLog.params && (
              <pre style={{
                padding: 12,
                background: '#f5f5f5',
                borderRadius: 4,
                maxHeight: 200,
                overflow: 'auto',
                fontSize: 12
              }}>
                {currentLog.params}
              </pre>
            )}
            <p><strong>IP地址：</strong>{currentLog.ipAddress || '-'}</p>
            <p><strong>操作地点：</strong>{currentLog.location || '-'}</p>
            <p><strong>执行时长：</strong>{currentLog.time ? `${currentLog.time}ms` : '-'}</p>
            <p>
              <strong>操作状态：</strong>
              <Tag color={currentLog.status === 1 ? 'success' : 'error'}>
                {currentLog.status === 1 ? '成功' : '失败'}
              </Tag>
            </p>
            {currentLog.errorMsg && (
              <>
                <p><strong>错误消息：</strong></p>
                <pre style={{
                  padding: 12,
                  background: '#fff2f0',
                  borderRadius: 4,
                  maxHeight: 200,
                  overflow: 'auto',
                  fontSize: 12,
                  color: '#ff4d4f'
                }}>
                  {currentLog.errorMsg}
                </pre>
              </>
            )}
            <p><strong>操作时间：</strong>{currentLog.operationTime ? dayjs(currentLog.operationTime).format('YYYY-MM-DD HH:mm:ss') : '-'}</p>
          </div>
        )}
      </Modal>
    </Card>
  )
}

export default OperationLogPage
