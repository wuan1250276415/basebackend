import React, { useCallback, useEffect } from 'react';
import { Avatar } from 'antd';
import { UserOutlined } from '@ant-design/icons';
import type { Friend } from '@/types';
import { useConversationStore } from '@/stores/useConversationStore';
import { useContactStore } from '@/stores/useContactStore';
import { getFriends } from '@/services/friendApi';
import { createConversation } from '@/services/conversationApi';
import { ConversationType } from '@/types';

/** 好友列表组件 */
const ContactList: React.FC = () => {
  const { friends, setFriends } = useContactStore();
  const { setActive } = useConversationStore();

  useEffect(() => {
    getFriends().then(setFriends).catch(console.error);
  }, [setFriends]);

  /** 点击好友，打开/创建私聊会话 */
  const handleClick = useCallback(async (friend: Friend) => {
    try {
      const resp = await createConversation({
        type: ConversationType.PRIVATE,
        targetId: friend.userId,
      });
      setActive(resp.conversationId);
    } catch (err) {
      console.error('打开会话失败', err);
    }
  }, [setActive]);

  return (
    <div className="contacts-body">
      {friends.length === 0 && (
        <div className="empty-state" style={{ padding: 40 }}>
          <span>暂无好友</span>
        </div>
      )}
      {friends.map((f) => (
        <div key={f.userId} className="contact-item" onClick={() => handleClick(f)}>
          <Avatar
            className="contact-avatar"
            size={38}
            src={f.avatar || undefined}
            icon={!f.avatar ? <UserOutlined /> : undefined}
            shape="square"
          />
          <div className="contact-info">
            <div className="contact-name">{f.remark || f.nickname}</div>
            <div className="contact-status">
              <span className={`online-dot ${f.status}`} style={{ marginRight: 4 }} />
              {f.status}
            </div>
          </div>
        </div>
      ))}
    </div>
  );
};

export default ContactList;
