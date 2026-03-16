/**
 * 聊天消息管理页面
 * 展示消息列表，支持按发送者、内容关键词、日期范围搜索
 * 支持删除消息（确认对话框）和查看消息详情（抽屉）
 */
import { useRef, useState } from 'react';
import { Button, Modal, Tag, Drawer, Descriptions, message } from 'antd';
import {
  DeleteOutlined,
  ExclamationCircleOutlined,
  EyeOutlined,
} from '@ant-design/icons';
import { ProTable } from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { chatApi } from '@/api/chatApi';
import { Permission } from '@/components/Permission';
import type { ChatMessage } from '@/types';

/** 消息类型映射 */
const messageTypeMap: Record<string, { text: string; color: string }> = {
  TEXT: { text: '文本', color: 'blue' },
  IMAGE: { text: '图片', color: 'green' },
  FILE: { text: '文件', color: 'orange' },
  AUDIO: { text: '语音', color: 'purple' },
  VIDEO: { text: '视频', color: 'cyan' },
};

/**
 * 聊天消息管理主页面
 */
const ChatMessagePage: React.FC = () => {
  const actionRef = useRef<ActionType>();
  /** 详情抽屉可见状态 */
  const [detailOpen, setDetailOpen] = useState(false);
  /** 当前查看的消息 */
  const [currentMessage, setCurrentMessage] = useState<ChatMessage | null>(null);

  /** 查看消息详情 */
  const handleViewDetail = (record: ChatMessage) => {
    setCurrentMessage(record);
    setDetailOpen(true);
  };

  /** 删除消息 */
  const handleDelete = (record: ChatMessage) => {
    Modal.confirm({
      title: '确认删除',
      icon: <ExclamationCircleOutlined />,
      content: '确定要删除该消息吗？此操作不可恢复！',
      okText: '确定',
      cancelText: '取消',
      onOk: async () => {
        await chatApi.deleteMessage(record.id);
        message.success('删除成功');
        actionRef.current?.reload();
      },
    });
  };

  /** 获取消息类型标签 */
  const renderMessageType = (type: string) => {
    const config = messageTypeMap[type] || { text: type, color: 'default' };
    return <Tag color={config.color}>{config.text}</Tag>;
  };

  /** 获取状态标签 */
  const renderStatus = (status: number) => {
    const map: Record<number, { text: string; color: string }> = {
      0: { text: '未发送', color: 'default' },
      1: { text: '已发送', color: 'success' },
      2: { text: '已撤回', color: 'warning' },
    };
    const config = map[status] || { text: `未知(${status})`, color: 'default' };
    return <Tag color={config.color}>{config.text}</Tag>;
  };

  /** 表格列定义 */
  const columns: ProColumns<ChatMessage>[] = [
    {
      title: '发送者',
      dataIndex: 'senderName',
      ellipsis: true,
      width: 120,
      search: {
        transform: (value: string) => ({ senderId: value }),
      },
      render: (_, record) => record.senderName || `用户${record.senderId}`,
    },
    {
      title: '会话ID',
      dataIndex: 'conversationId',
      hideInSearch: true,
      width: 100,
    },
    {
      title: '消息类型',
      dataIndex: 'messageType',
      hideInSearch: true,
      width: 100,
      render: (_, record) => renderMessageType(record.messageType),
    },
    {
      title: '内容预览',
      dataIndex: 'content',
      ellipsis: true,
      width: 300,
      search: {
        transform: (value: string) => ({ content: value }),
      },
    },
    {
      title: '发送时间',
      dataIndex: 'sendTime',
      valueType: 'dateTime',
      hideInSearch: true,
      width: 180,
    },
    {
      title: '发送时间',
      dataIndex: 'sendTimeRange',
      valueType: 'dateRange',
      hideInTable: true,
      search: {
        transform: (value: [string, string]) => ({
          beginTime: value[0],
          endTime: value[1],
        }),
      },
    },
    {
      title: '状态',
      dataIndex: 'status',
      hideInSearch: true,
      width: 100,
      render: (_, record) => renderStatus(record.status),
    },
    {
      title: '操作',
      valueType: 'option',
      width: 100,
      render: (_, record) => [
        <Button
          key="view"
          type="link"
          icon={<EyeOutlined />}
          size="small"
          onClick={() => handleViewDetail(record)}
        />,
        <Permission key="delete" code="chat:message:delete">
          <Button
            type="link"
            danger
            icon={<DeleteOutlined />}
            size="small"
            onClick={() => handleDelete(record)}
          />
        </Permission>,
      ],
    },
  ];

  return (
    <>
      <ProTable<ChatMessage>
        headerTitle="消息管理"
        actionRef={actionRef}
        rowKey="id"
        columns={columns}
        request={async (params) => {
          const { current = 1, pageSize = 10, ...query } = params;
          const res = await chatApi.messageList({
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
        onRow={(record) => ({
          onClick: () => handleViewDetail(record),
          style: { cursor: 'pointer' },
        })}
      />

      {/* 消息详情抽屉 */}
      <Drawer
        title="消息详情"
        open={detailOpen}
        onClose={() => setDetailOpen(false)}
        width={600}
      >
        {currentMessage && (
          <Descriptions column={1} bordered size="small">
            <Descriptions.Item label="消息ID">
              {currentMessage.id}
            </Descriptions.Item>
            <Descriptions.Item label="发送者">
              {currentMessage.senderName || `用户${currentMessage.senderId}`}
            </Descriptions.Item>
            <Descriptions.Item label="会话ID">
              {currentMessage.conversationId}
            </Descriptions.Item>
            <Descriptions.Item label="消息类型">
              {renderMessageType(currentMessage.messageType)}
            </Descriptions.Item>
            <Descriptions.Item label="状态">
              {renderStatus(currentMessage.status)}
            </Descriptions.Item>
            <Descriptions.Item label="发送时间">
              {currentMessage.sendTime}
            </Descriptions.Item>
            <Descriptions.Item label="消息内容">
              <pre style={{ margin: 0, whiteSpace: 'pre-wrap', wordBreak: 'break-all' }}>
                {currentMessage.content}
              </pre>
            </Descriptions.Item>
          </Descriptions>
        )}
      </Drawer>
    </>
  );
};

export default ChatMessagePage;
