/**
 * 聊天群组管理页面
 * 展示群组列表，支持查看成员（弹窗）和解散群组（确认对话框）
 * 使用 ProTable 实现服务端分页和搜索过滤
 */
import { useRef, useState } from 'react';
import { Button, Modal, Tag, Table, Avatar, Space, message } from 'antd';
import {
  DeleteOutlined,
  ExclamationCircleOutlined,
  TeamOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { ProTable } from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { chatApi } from '@/api/chatApi';
import { Permission } from '@/components/Permission';
import type { ChatGroup, ChatGroupMember } from '@/types';

/** 群组成员角色映射 */
const memberRoleMap: Record<number, { text: string; color: string }> = {
  0: { text: '群主', color: 'red' },
  1: { text: '管理员', color: 'orange' },
  2: { text: '成员', color: 'blue' },
};

/** 群组状态映射 */
const groupStatusMap: Record<number, { text: string; color: string }> = {
  0: { text: '已解散', color: 'default' },
  1: { text: '正常', color: 'success' },
};

/**
 * 聊天群组管理主页面
 */
const ChatGroupPage: React.FC = () => {
  const actionRef = useRef<ActionType>();
  /** 成员弹窗可见状态 */
  const [memberModalOpen, setMemberModalOpen] = useState(false);
  /** 当前查看的群组 */
  const [currentGroup, setCurrentGroup] = useState<ChatGroup | null>(null);
  /** 群组成员列表 */
  const [members, setMembers] = useState<ChatGroupMember[]>([]);
  /** 成员加载状态 */
  const [memberLoading, setMemberLoading] = useState(false);

  /** 查看群组成员 */
  const handleViewMembers = async (record: ChatGroup) => {
    setCurrentGroup(record);
    setMemberLoading(true);
    setMemberModalOpen(true);
    try {
      const data = await chatApi.groupMembers(record.id);
      setMembers(data ?? []);
    } finally {
      setMemberLoading(false);
    }
  };

  /** 解散群组 */
  const handleDissolve = (record: ChatGroup) => {
    Modal.confirm({
      title: '确认解散',
      icon: <ExclamationCircleOutlined />,
      content: `确定要解散群组「${record.groupName}」吗？此操作不可恢复！`,
      okText: '确定',
      okButtonProps: { danger: true },
      cancelText: '取消',
      onOk: async () => {
        await chatApi.dissolveGroup(record.id);
        message.success('群组已解散');
        actionRef.current?.reload();
      },
    });
  };

  /** 表格列定义 */
  const columns: ProColumns<ChatGroup>[] = [
    {
      title: '群组名称',
      dataIndex: 'groupName',
      ellipsis: true,
    },
    {
      title: '群主',
      dataIndex: 'ownerName',
      hideInSearch: true,
      ellipsis: true,
      render: (_, record) => record.ownerName || `用户${record.ownerId}`,
    },
    {
      title: '成员数量',
      dataIndex: 'memberCount',
      hideInSearch: true,
      width: 120,
      render: (_, record) => `${record.memberCount} / ${record.maxMembers}`,
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      valueType: 'dateTime',
      hideInSearch: true,
      width: 180,
    },
    {
      title: '状态',
      dataIndex: 'status',
      valueType: 'select',
      valueEnum: {
        0: { text: '已解散', status: 'Default' },
        1: { text: '正常', status: 'Success' },
      },
      width: 100,
      render: (_, record) => {
        const config = groupStatusMap[record.status] || { text: `未知(${record.status})`, color: 'default' };
        return <Tag color={config.color}>{config.text}</Tag>;
      },
    },
    {
      title: '操作',
      valueType: 'option',
      width: 120,
      render: (_, record) => (
        <Space>
          <Button
            type="link"
            icon={<TeamOutlined />}
            size="small"
            onClick={() => handleViewMembers(record)}
          />
          <Permission code="chat:group:dissolve">
            <Button
              type="link"
              danger
              icon={<DeleteOutlined />}
              size="small"
              onClick={() => handleDissolve(record)}
            />
          </Permission>
        </Space>
      ),
    },
  ];

  /** 成员表格列定义 */
  const memberColumns = [
    {
      title: '头像',
      dataIndex: 'avatar',
      width: 60,
      render: (_: unknown, record: ChatGroupMember) => (
        <Avatar src={record.avatar} icon={<UserOutlined />} size="small" />
      ),
    },
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
      title: '角色',
      dataIndex: 'role',
      width: 100,
      render: (_: unknown, record: ChatGroupMember) => {
        const config = memberRoleMap[record.role] || { text: `未知(${record.role})`, color: 'default' };
        return <Tag color={config.color}>{config.text}</Tag>;
      },
    },
    {
      title: '加入时间',
      dataIndex: 'joinTime',
      width: 180,
    },
  ];

  return (
    <>
      <ProTable<ChatGroup>
        headerTitle="群组管理"
        actionRef={actionRef}
        rowKey="id"
        columns={columns}
        request={async (params) => {
          const { current = 1, pageSize = 10, ...query } = params;
          const res = await chatApi.groupList({
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
      />

      {/* 查看成员弹窗 */}
      <Modal
        title={`群组成员 - ${currentGroup?.groupName ?? ''}`}
        open={memberModalOpen}
        onCancel={() => setMemberModalOpen(false)}
        footer={null}
        width={700}
        destroyOnClose
      >
        <Table<ChatGroupMember>
          rowKey="userId"
          columns={memberColumns}
          dataSource={members}
          loading={memberLoading}
          pagination={false}
          size="small"
        />
      </Modal>
    </>
  );
};

export default ChatGroupPage;
