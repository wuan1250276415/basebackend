import React, { useCallback, useRef, useState } from 'react';
import { Avatar, Dropdown, Image, type MenuProps } from 'antd';
import { UserOutlined } from '@ant-design/icons';
import { Virtuoso, type VirtuosoHandle } from 'react-virtuoso';
import type { Message, QuoteMessage } from '@/types';
import { MessageStatus, MessageType } from '@/types';
import { formatTime, formatFileSize } from '@/utils/format';
import dayjs from 'dayjs';

interface MessageListProps {
  messages: Message[];
  currentUserId: number;
  hasMore: boolean;
  onLoadMore: () => void;
  /** 撤回消息回调（2分钟内自己的消息） */
  onRevoke?: (messageId: number) => void;
  /** 引用回复回调 */
  onQuote?: (msg: QuoteMessage) => void;
  /** 转发回调 */
  onForward?: (messageId: number) => void;
}

/** 判断两条消息之间是否需要时间分割线（间隔超过 5 分钟） */
function needTimeDivider(prev: Message | undefined, curr: Message): boolean {
  if (!prev) return true;
  const diff = dayjs(curr.sendTime).diff(dayjs(prev.sendTime), 'minute');
  return Math.abs(diff) >= 5;
}

/** 消息列表组件（虚拟滚动 + 右键菜单 + 时间分割线） */
const MessageList: React.FC<MessageListProps> = ({
  messages,
  currentUserId,
  hasMore,
  onLoadMore,
  onRevoke,
  onQuote,
  onForward,
}) => {
  const virtuosoRef = useRef<VirtuosoHandle>(null);
  const [_isAtBottom, setIsAtBottom] = useState(true);

  /** 加载更多历史消息 */
  const handleStartReached = useCallback(() => {
    if (hasMore) {
      onLoadMore();
    }
  }, [hasMore, onLoadMore]);

  /** 构建右键菜单 */
  const buildContextMenu = useCallback(
    (message: Message): MenuProps['items'] => {
      const items: MenuProps['items'] = [];
      const isSelf = message.senderId === currentUserId;

      // 引用回复
      if (message.status !== MessageStatus.REVOKED && onQuote) {
        items.push({
          key: 'quote',
          label: '引用回复',
          onClick: () =>
            onQuote({
              messageId: message.messageId,
              senderId: message.senderId,
              senderName: message.senderName,
              type: message.type,
              content: message.content,
            }),
        });
      }

      // 转发
      if (message.status !== MessageStatus.REVOKED && onForward) {
        items.push({
          key: 'forward',
          label: '转发',
          onClick: () => onForward(message.messageId),
        });
      }

      // 撤回（自己发送的，2分钟内）
      if (isSelf && message.status === MessageStatus.SENT && onRevoke) {
        const elapsed = dayjs().diff(dayjs(message.sendTime), 'second');
        if (elapsed <= 120) {
          items.push({
            key: 'revoke',
            label: '撤回',
            danger: true,
            onClick: () => onRevoke(message.messageId),
          });
        }
      }

      // 复制文本
      if (message.type === MessageType.TEXT && message.content) {
        items.push({
          key: 'copy',
          label: '复制',
          onClick: () => {
            void navigator.clipboard.writeText(message.content ?? '');
          },
        });
      }

      return items.length > 0 ? items : [{ key: 'none', label: '无可用操作', disabled: true }];
    },
    [currentUserId, onRevoke, onQuote, onForward],
  );

  /** 渲染单条消息（含时间分割线） */
  const renderMessage = useCallback(
    (index: number, message: Message) => {
      const isSelf = message.senderId === currentUserId;
      const prevMsg = index > 0 ? messages[index - 1] : undefined;
      const showDivider = needTimeDivider(prevMsg, message);

      // 撤回消息
      if (message.status === MessageStatus.REVOKED) {
        return (
          <div key={message.messageId || message.clientMsgId}>
            {showDivider && (
              <div className="message-time-divider">{formatTime(message.sendTime)}</div>
            )}
            <div className="message-row">
              <div style={{ width: '100%', textAlign: 'center' }}>
                <span className="message-bubble revoked">
                  {isSelf ? '你' : message.senderName}撤回了一条消息
                </span>
              </div>
            </div>
          </div>
        );
      }

      return (
        <div key={message.messageId || message.clientMsgId}>
          {showDivider && (
            <div className="message-time-divider">{formatTime(message.sendTime)}</div>
          )}
          <Dropdown menu={{ items: buildContextMenu(message) }} trigger={['contextMenu']}>
            <div className={`message-row${isSelf ? ' self' : ''}`}>
              <Avatar
                className="message-avatar"
                size={36}
                src={message.senderAvatar || undefined}
                icon={!message.senderAvatar ? <UserOutlined /> : undefined}
                shape="square"
              />
              <div className="message-body">
                {!isSelf && <span className="message-sender">{message.senderName}</span>}
                {/* 引用消息显示 */}
                {message.quoteMessage && (
                  <div className="message-quote">
                    <span className="message-quote-name">{message.quoteMessage.senderName}:</span>
                    {' '}
                    {message.quoteMessage.content ?? '[消息]'}
                  </div>
                )}
                <div className="message-bubble">
                  <MessageContent message={message} />
                </div>
                <span className="message-time">
                  {formatTime(message.sendTime)}
                  {message.status === MessageStatus.SENDING && ' 发送中...'}
                  {message.status === MessageStatus.FAILED && ' 发送失败'}
                </span>
              </div>
            </div>
          </Dropdown>
        </div>
      );
    },
    [currentUserId, messages, buildContextMenu],
  );

  if (messages.length === 0) {
    return (
      <div className="chat-messages">
        <div className="empty-state">
          <span className="empty-state-icon">&#128172;</span>
          <span>暂无消息</span>
        </div>
      </div>
    );
  }

  return (
    <div className="chat-messages" style={{ padding: 0 }}>
      <Virtuoso
        ref={virtuosoRef}
        data={messages}
        style={{ height: '100%' }}
        initialTopMostItemIndex={messages.length - 1}
        followOutput="smooth"
        startReached={handleStartReached}
        atBottomStateChange={setIsAtBottom}
        alignToBottom
        itemContent={renderMessage}
        overscan={200}
      />
    </div>
  );
};

/** 消息内容渲染：根据消息类型分发 */
const MessageContent: React.FC<{ message: Message }> = ({ message }) => {
  switch (message.type) {
    case MessageType.TEXT:
      return <span>{message.content}</span>;

    case MessageType.IMAGE:
      return (
        <Image
          src={message.extra?.url ?? message.extra?.thumbnailUrl ?? ''}
          alt="图片"
          style={{ maxWidth: 240, maxHeight: 240, borderRadius: 4 }}
          preview={{ mask: '预览' }}
          fallback="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="
        />
      );

    case MessageType.FILE:
      return (
        <a
          href={message.extra?.url ?? '#'}
          download={message.extra?.fileName ?? '文件'}
          target="_blank"
          rel="noreferrer"
          style={{ display: 'flex', alignItems: 'center', gap: 8, textDecoration: 'none', color: 'inherit' }}
        >
          <span style={{ fontSize: 24 }}>&#128206;</span>
          <div>
            <div style={{ fontSize: 13 }}>{message.extra?.fileName ?? '文件'}</div>
            <div style={{ fontSize: 11, color: 'var(--color-text-tertiary)' }}>
              {message.extra?.size ? formatFileSize(message.extra.size) : ''}
              {' '}
              <span style={{ color: 'var(--color-accent)' }}>下载</span>
            </div>
          </div>
        </a>
      );

    case MessageType.AUDIO:
      return <span>&#127908; 语音消息{message.extra?.duration ? ` ${message.extra.duration}"` : ''}</span>;

    case MessageType.VIDEO:
      return <span>&#127916; 视频消息</span>;

    case MessageType.LOCATION:
      return <span>&#128205; {message.content ?? '位置消息'}</span>;

    case MessageType.CONTACT_CARD:
      return <span>&#128100; {message.content ?? '名片'}</span>;

    case MessageType.EMOJI:
      return message.extra?.url
        ? <img src={message.extra.url} alt="表情" style={{ width: 120, height: 120 }} />
        : <span style={{ fontSize: 32 }}>{message.content}</span>;

    case MessageType.SYSTEM:
      return <span style={{ color: 'var(--color-text-tertiary)', fontSize: 12 }}>{message.content}</span>;

    case MessageType.MERGED_FORWARD:
      return (
        <div style={{ fontSize: 13, color: 'var(--color-text-secondary)' }}>
          <div style={{ fontWeight: 500, marginBottom: 4 }}>{message.extra?.title ?? '聊天记录'}</div>
          <div>[合并转发消息]</div>
        </div>
      );

    default:
      return <span>{message.content ?? '[未知消息类型]'}</span>;
  }
};

export default MessageList;
