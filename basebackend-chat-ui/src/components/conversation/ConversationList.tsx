import React from 'react';
import { Avatar, Badge } from 'antd';
import { UserOutlined } from '@ant-design/icons';
import type { Conversation } from '@/types';
import { formatTime, messagePreview } from '@/utils/format';
import { useConversationStore } from '@/stores/useConversationStore';

interface ConversationListProps {
  conversations: Conversation[];
  onSearch?: (keyword: string) => void;
}

/** 会话列表组件（左栏） */
const ConversationList: React.FC<ConversationListProps> = ({ conversations, onSearch }) => {
  const { activeConversationId, setActive } = useConversationStore();

  return (
    <div className="conv-panel">
      <div className="conv-panel-header">
        <input
          type="text"
          placeholder="搜索"
          onChange={(e) => onSearch?.(e.target.value)}
        />
      </div>
      <div className="conv-list">
        {conversations.map((conv) => (
          <div
            key={conv.conversationId}
            className={`conv-item${conv.conversationId === activeConversationId ? ' active' : ''}`}
            onClick={() => setActive(conv.conversationId)}
          >
            <Avatar
              className="conv-item-avatar"
              size={42}
              src={conv.targetAvatar || undefined}
              icon={!conv.targetAvatar ? <UserOutlined /> : undefined}
              shape="square"
            />
            <div className="conv-item-body">
              <div className="conv-item-top">
                <span className="conv-item-name">
                  {conv.isPinned && <span style={{ color: 'var(--color-accent)', marginRight: 4 }}>⊤</span>}
                  {conv.targetName}
                </span>
                <span className="conv-item-time">
                  {conv.lastMessage ? formatTime(conv.lastMessage.sendTime) : ''}
                </span>
              </div>
              <div className="conv-item-bottom">
                <span className="conv-item-preview">
                  {conv.draft
                    ? <span style={{ color: 'var(--color-danger)' }}>[草稿] {conv.draft}</span>
                    : conv.lastMessage
                      ? messagePreview(conv.lastMessage.type, conv.lastMessage.content)
                      : ''
                  }
                </span>
                {conv.unreadCount > 0 && !conv.isMuted && (
                  <Badge count={conv.unreadCount} size="small" />
                )}
                {conv.unreadCount > 0 && conv.isMuted && (
                  <span className="online-dot offline" />
                )}
              </div>
            </div>
          </div>
        ))}
        {conversations.length === 0 && (
          <div className="empty-state" style={{ padding: 40 }}>
            <span>暂无会话</span>
          </div>
        )}
      </div>
    </div>
  );
};

export default ConversationList;
