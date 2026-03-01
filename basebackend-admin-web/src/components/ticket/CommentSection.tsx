import React, { useState } from 'react';
import { List, Avatar, Input, Button, Space, Tag, Popconfirm, Switch, message } from 'antd';
import { UserOutlined, DeleteOutlined, SendOutlined, LockOutlined } from '@ant-design/icons';
import type { CommentItem } from '@/api/ticketApi';
import { ticketApi } from '@/api/ticketApi';

interface CommentSectionProps {
  ticketId: number;
  comments: CommentItem[];
  onRefresh: () => void;
}

const CommentSection: React.FC<CommentSectionProps> = ({ ticketId, comments, onRefresh }) => {
  const [content, setContent] = useState('');
  const [isInternal, setIsInternal] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async () => {
    if (!content.trim()) {
      message.warning('请输入评论内容');
      return;
    }
    setSubmitting(true);
    try {
      await ticketApi.addComment(ticketId, {
        content: content.trim(),
        type: 'COMMENT',
        isInternal: isInternal ? 1 : 0,
      });
      setContent('');
      setIsInternal(false);
      onRefresh();
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (commentId: number) => {
    await ticketApi.deleteComment(ticketId, commentId);
    message.success('评论已删除');
    onRefresh();
  };

  return (
    <div>
      <List
        itemLayout="horizontal"
        dataSource={comments}
        locale={{ emptyText: '暂无评论' }}
        renderItem={(item) => (
          <List.Item
            actions={[
              item.type !== 'SYSTEM' && (
                <Popconfirm key="del" title="确认删除该评论？" onConfirm={() => handleDelete(item.id)}>
                  <Button type="link" size="small" icon={<DeleteOutlined />} danger />
                </Popconfirm>
              ),
            ].filter(Boolean)}
          >
            <List.Item.Meta
              avatar={
                <Avatar
                  icon={<UserOutlined />}
                  size="small"
                  style={item.type === 'SYSTEM' ? { backgroundColor: '#d9d9d9' } : undefined}
                />
              }
              title={
                <Space size={4}>
                  <span style={{ fontWeight: 500 }}>{item.creatorName}</span>
                  {item.type === 'SYSTEM' && <Tag color="default">系统</Tag>}
                  {item.type === 'APPROVAL' && <Tag color="blue">审批</Tag>}
                  {item.isInternal === 1 && (
                    <Tag color="orange" icon={<LockOutlined />}>内部备注</Tag>
                  )}
                  <span style={{ color: '#999', fontSize: 12, fontWeight: 'normal' }}>{item.createTime}</span>
                </Space>
              }
              description={
                <div style={{ whiteSpace: 'pre-wrap', color: '#333' }}>{item.content}</div>
              }
            />
          </List.Item>
        )}
      />

      <div style={{ marginTop: 16, border: '1px solid #d9d9d9', borderRadius: 6, padding: 12 }}>
        <Input.TextArea
          rows={3}
          value={content}
          onChange={(e) => setContent(e.target.value)}
          placeholder={isInternal ? '输入内部备注（仅内部可见）...' : '输入评论内容...'}
          onPressEnter={(e) => { if (e.ctrlKey) handleSubmit(); }}
          maxLength={2000}
          showCount
          style={{ border: 'none', boxShadow: 'none', padding: 0 }}
        />
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 8 }}>
          <Space>
            <Switch
              size="small"
              checked={isInternal}
              onChange={setIsInternal}
            />
            <span style={{ fontSize: 13, color: isInternal ? '#fa8c16' : '#999' }}>
              {isInternal ? '内部备注（仅内部可见）' : '公开评论'}
            </span>
          </Space>
          <Space>
            <span style={{ fontSize: 12, color: '#999' }}>Ctrl+Enter 发送</span>
            <Button
              type="primary"
              icon={<SendOutlined />}
              loading={submitting}
              onClick={handleSubmit}
              disabled={!content.trim()}
            >
              发送
            </Button>
          </Space>
        </div>
      </div>
    </div>
  );
};

export default CommentSection;
