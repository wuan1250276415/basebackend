import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Modal, message } from 'antd';
import { useConversationStore } from '@/stores/useConversationStore';
import { useMessageStore } from '@/stores/useMessageStore';
import { useWebSocketStore } from '@/stores/useWebSocketStore';
import { useAuthStore } from '@/stores/useAuthStore';
import { getConversations, clearUnread } from '@/services/conversationApi';
import { getMessages, revokeMessage, forwardMessages } from '@/services/messageApi';
import { uploadFile } from '@/services/fileApi';
import { chatWs } from '@/services/websocket';
import { MessageType, MessageStatus } from '@/types';
import type { QuoteMessage, Conversation } from '@/types';
import { generateClientMsgId } from '@/utils/format';
import ConversationList from '@/components/conversation/ConversationList';
import MessageList from '@/components/message/MessageList';
import MessageInput from '@/components/message/MessageInput';

/** 聊天主页面（三栏布局中的 会话列表 + 聊天面板） */
const ChatPage: React.FC = () => {
  const user = useAuthStore((s) => s.user);
  const {
    conversations,
    activeConversationId,
    setConversations,
    setLoading: setConvLoading,
    updateConversation,
  } = useConversationStore();
  const {
    messageMap,
    hasMoreMap,
    setMessages,
    prependMessages,
    addMessage,
    setLoading: setMsgLoading,
  } = useMessageStore();
  const typingUsers = useWebSocketStore((s) => s.typingUsers);

  /** 引用回复状态 */
  const [quoteMessage, setQuoteMessage] = useState<QuoteMessage | null>(null);
  /** 转发选择对话框 */
  const [forwardMsgId, setForwardMsgId] = useState<number | null>(null);
  const [forwardModalOpen, setForwardModalOpen] = useState(false);

  const currentUserId = user?.userId ?? 0;
  const activeConv = useMemo(
    () => conversations.find((c) => c.conversationId === activeConversationId),
    [conversations, activeConversationId],
  );
  const messages = activeConversationId != null ? (messageMap[activeConversationId] ?? []) : [];
  const hasMore = activeConversationId != null ? (hasMoreMap[activeConversationId] ?? true) : false;
  const typingUser = activeConversationId != null ? typingUsers[activeConversationId] : undefined;

  /** 切换会话时清除引用状态 */
  useEffect(() => {
    setQuoteMessage(null);
  }, [activeConversationId]);

  /** 加载会话列表 */
  useEffect(() => {
    setConvLoading(true);
    getConversations(1, 100)
      .then((page) => setConversations(page.records))
      .catch(console.error)
      .finally(() => setConvLoading(false));
  }, [setConversations, setConvLoading]);

  /** 切换会话时加载消息 + 清除未读 */
  useEffect(() => {
    if (activeConversationId == null) return;
    // 如果已加载则跳过
    if (messageMap[activeConversationId]) return;

    setMsgLoading(activeConversationId, true);
    getMessages(activeConversationId, { limit: 30 })
      .then((data) => {
        setMessages(activeConversationId, data.messages, data.hasMore);
        // 清除未读
        const conv = conversations.find((c) => c.conversationId === activeConversationId);
        if (conv && conv.unreadCount > 0 && data.messages.length > 0) {
          const lastMsg = data.messages[data.messages.length - 1]!;
          clearUnread(activeConversationId, lastMsg.messageId).catch(console.error);
          updateConversation(activeConversationId, { unreadCount: 0 });
          // 发送已读上报
          chatWs.sendRead(activeConversationId, lastMsg.messageId);
        }
      })
      .catch(console.error)
      .finally(() => setMsgLoading(activeConversationId, false));
  }, [activeConversationId, messageMap, setMessages, setMsgLoading, conversations, updateConversation]);

  /** 加载更多历史消息 */
  const handleLoadMore = useCallback(() => {
    if (activeConversationId == null) return;
    const msgs = messageMap[activeConversationId];
    if (!msgs || msgs.length === 0) return;
    const firstMsg = msgs[0]!;

    setMsgLoading(activeConversationId, true);
    getMessages(activeConversationId, { beforeId: firstMsg.messageId, limit: 30 })
      .then((data) => prependMessages(activeConversationId, data.messages, data.hasMore))
      .catch(console.error)
      .finally(() => setMsgLoading(activeConversationId, false));
  }, [activeConversationId, messageMap, prependMessages, setMsgLoading]);

  /** 发送消息 */
  const handleSend = useCallback(
    (content: string) => {
      if (activeConversationId == null || !user) return;

      const clientMsgId = generateClientMsgId();

      // 乐观插入本地消息
      addMessage(activeConversationId, {
        messageId: 0,
        conversationId: activeConversationId,
        senderId: user.userId,
        senderName: user.nickname,
        senderAvatar: user.avatar,
        type: MessageType.TEXT,
        content,
        extra: null,
        quoteMessage: quoteMessage,
        atUserIds: null,
        clientMsgId,
        sendTime: new Date().toISOString(),
        status: MessageStatus.SENDING,
      });

      // 通过 WebSocket 发送
      chatWs.sendChat({
        clientMsgId,
        conversationId: activeConversationId,
        msgType: MessageType.TEXT,
        content,
        quoteMessageId: quoteMessage?.messageId ?? null,
      });

      // 清除引用
      setQuoteMessage(null);
    },
    [activeConversationId, user, addMessage, quoteMessage],
  );

  /** 撤回消息 */
  const handleRevoke = useCallback(
    async (messageId: number) => {
      if (activeConversationId == null) return;
      try {
        await revokeMessage(messageId);
        useMessageStore.getState().revokeMessage(activeConversationId, messageId);
        message.success('消息已撤回');
      } catch {
        message.error('撤回失败');
      }
    },
    [activeConversationId],
  );

  /** 引用回复 */
  const handleQuote = useCallback((msg: QuoteMessage) => {
    setQuoteMessage(msg);
  }, []);

  /** 转发消息 — 打开选择会话对话框 */
  const handleForward = useCallback((messageId: number) => {
    setForwardMsgId(messageId);
    setForwardModalOpen(true);
  }, []);

  /** 确认转发到指定会话 */
  const handleForwardConfirm = useCallback(
    async (targetConv: Conversation) => {
      if (forwardMsgId == null) return;
      try {
        await forwardMessages({
          messageIds: [forwardMsgId],
          targetConversationIds: [targetConv.conversationId],
          forwardType: 'single',
        });
        message.success(`已转发到 ${targetConv.targetName}`);
      } catch {
        message.error('转发失败');
      }
      setForwardModalOpen(false);
      setForwardMsgId(null);
    },
    [forwardMsgId],
  );

  /** 文件上传 */
  const handleFileUpload = useCallback(
    async (file: File) => {
      if (activeConversationId == null || !user) return;
      try {
        const result = await uploadFile(file);
        const isImage = file.type.startsWith('image/');
        const clientMsgId = generateClientMsgId();
        const msgType = isImage ? MessageType.IMAGE : MessageType.FILE;

        // 乐观插入
        addMessage(activeConversationId, {
          messageId: 0,
          conversationId: activeConversationId,
          senderId: user.userId,
          senderName: user.nickname,
          senderAvatar: user.avatar,
          type: msgType,
          content: isImage ? '[图片]' : result.fileName,
          extra: {
            url: result.url,
            fileName: result.fileName,
            size: result.fileSize,
            format: result.contentType,
          },
          quoteMessage: null,
          atUserIds: null,
          clientMsgId,
          sendTime: new Date().toISOString(),
          status: MessageStatus.SENDING,
        });

        // 通过 WebSocket 发送
        chatWs.sendChat({
          clientMsgId,
          conversationId: activeConversationId,
          msgType: msgType as number,
          content: isImage ? '[图片]' : result.fileName,
          extra: {
            url: result.url,
            fileName: result.fileName,
            size: result.fileSize,
            format: result.contentType,
          },
        });
      } catch {
        message.error('文件上传失败');
      }
    },
    [activeConversationId, user, addMessage],
  );

  /** 正在输入 */
  const handleTyping = useCallback(() => {
    if (activeConversationId != null) {
      chatWs.sendTyping(activeConversationId);
    }
  }, [activeConversationId]);

  return (
    <>
      {/* 会话列表 */}
      <ConversationList conversations={conversations} />

      {/* 聊天面板 */}
      <div className="chat-panel">
        {activeConv ? (
          <>
            <div className="chat-header">
              <span className="chat-header-title">
                {activeConv.targetName}
                {activeConv.memberCount != null && (
                  <span style={{ color: 'var(--color-text-tertiary)', fontSize: 13, marginLeft: 8 }}>
                    ({activeConv.memberCount})
                  </span>
                )}
              </span>
            </div>

            <MessageList
              messages={messages}
              currentUserId={currentUserId}
              hasMore={hasMore}
              onLoadMore={handleLoadMore}
              onRevoke={handleRevoke}
              onQuote={handleQuote}
              onForward={handleForward}
            />

            {typingUser && (
              <div className="typing-indicator" style={{ padding: '0 20px' }}>
                {typingUser.nickname} 正在输入...
              </div>
            )}

            <MessageInput
              onSend={handleSend}
              onTyping={handleTyping}
              onFileUpload={handleFileUpload}
              quoteMessage={quoteMessage}
              onCancelQuote={() => setQuoteMessage(null)}
            />
          </>
        ) : (
          <div className="empty-state">
            <span className="empty-state-icon">&#128172;</span>
            <span>选择一个会话开始聊天</span>
          </div>
        )}
      </div>

      {/* 转发选择会话对话框 */}
      <Modal
        title="转发到"
        open={forwardModalOpen}
        onCancel={() => setForwardModalOpen(false)}
        footer={null}
        width={360}
      >
        <div style={{ maxHeight: 400, overflowY: 'auto' }}>
          {conversations.map((conv) => (
            <div
              key={conv.conversationId}
              className="contact-item"
              onClick={() => void handleForwardConfirm(conv)}
            >
              <div className="contact-info">
                <div className="contact-name">{conv.targetName}</div>
              </div>
            </div>
          ))}
        </div>
      </Modal>
    </>
  );
};

export default ChatPage;
