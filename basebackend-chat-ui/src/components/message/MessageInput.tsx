import React, { useState, useRef, useCallback } from 'react';
import { Popover } from 'antd';
import {
  SmileOutlined,
  PictureOutlined,
  FolderOutlined,
  SendOutlined,
  CloseOutlined,
} from '@ant-design/icons';
import type { QuoteMessage } from '@/types';

/** 常用 Emoji 列表 */
const EMOJI_LIST = [
  '😀','😁','😂','🤣','😊','😍','🥰','😘','😜','🤔',
  '😎','🥳','😢','😭','😡','🤯','🥺','👍','👎','👏',
  '🙏','💪','❤️','🔥','⭐','🎉','💯','✅','❌','⚡',
  '😄','😆','😅','😋','😛','🤗','🤭','🤫','🤥','😶',
  '😑','😬','🙄','😲','🤤','😴','🤮','🤧','😇','🤠',
];

interface MessageInputProps {
  onSend: (content: string) => void;
  onTyping?: () => void;
  onFileUpload?: (file: File) => void;
  /** 当前引用的消息（显示在输入框上方） */
  quoteMessage?: QuoteMessage | null;
  /** 取消引用回调 */
  onCancelQuote?: () => void;
  disabled?: boolean;
}

/** 消息输入框组件（含表情面板、文件上传、引用回复） */
const MessageInput: React.FC<MessageInputProps> = ({
  onSend,
  onTyping,
  onFileUpload,
  quoteMessage,
  onCancelQuote,
  disabled,
}) => {
  const [text, setText] = useState('');
  const [emojiOpen, setEmojiOpen] = useState(false);
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const imageInputRef = useRef<HTMLInputElement>(null);
  const typingTimerRef = useRef<ReturnType<typeof setTimeout>>();

  /** 自动调整高度 */
  const adjustHeight = useCallback(() => {
    const el = textareaRef.current;
    if (!el) return;
    el.style.height = 'auto';
    el.style.height = `${Math.min(el.scrollHeight, 120)}px`;
  }, []);

  /** 输入变化时触发正在输入事件（节流 3 秒） */
  const handleChange = useCallback(
    (e: React.ChangeEvent<HTMLTextAreaElement>) => {
      setText(e.target.value);
      adjustHeight();

      if (onTyping) {
        if (typingTimerRef.current) clearTimeout(typingTimerRef.current);
        typingTimerRef.current = setTimeout(() => {
          onTyping();
        }, 300);
      }
    },
    [adjustHeight, onTyping],
  );

  /** 发送消息 */
  const handleSend = useCallback(() => {
    const content = text.trim();
    if (!content) return;
    onSend(content);
    setText('');
    setEmojiOpen(false);
    // 重置高度
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
    }
  }, [text, onSend]);

  /** Enter 发送，Shift+Enter 换行，Ctrl+Enter 也发送 */
  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        handleSend();
      }
      if (e.key === 'Enter' && e.ctrlKey) {
        e.preventDefault();
        handleSend();
      }
    },
    [handleSend],
  );

  /** 插入 Emoji */
  const insertEmoji = useCallback(
    (emoji: string) => {
      const el = textareaRef.current;
      if (el) {
        const start = el.selectionStart;
        const end = el.selectionEnd;
        const newText = text.slice(0, start) + emoji + text.slice(end);
        setText(newText);
        // 光标移到 emoji 后面
        requestAnimationFrame(() => {
          el.selectionStart = start + emoji.length;
          el.selectionEnd = start + emoji.length;
          el.focus();
        });
      } else {
        setText((prev) => prev + emoji);
      }
    },
    [text],
  );

  /** 文件选择处理 */
  const handleFileSelect = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      const file = e.target.files?.[0];
      if (file && onFileUpload) {
        onFileUpload(file);
      }
      // 清空 value 以允许重复选同一文件
      e.target.value = '';
    },
    [onFileUpload],
  );

  /** 表情面板内容 */
  const emojiPanel = (
    <div className="emoji-panel">
      {EMOJI_LIST.map((emoji) => (
        <span
          key={emoji}
          className="emoji-item"
          onClick={() => insertEmoji(emoji)}
        >
          {emoji}
        </span>
      ))}
    </div>
  );

  return (
    <div className="chat-input-area">
      {/* 引用消息预览 */}
      {quoteMessage && (
        <div className="chat-input-quote">
          <div className="chat-input-quote-content">
            <span className="chat-input-quote-name">{quoteMessage.senderName}:</span>
            {' '}
            {quoteMessage.content ?? '[消息]'}
          </div>
          <CloseOutlined className="chat-input-quote-close" onClick={onCancelQuote} />
        </div>
      )}

      <div className="chat-input-toolbar">
        <Popover
          content={emojiPanel}
          trigger="click"
          open={emojiOpen}
          onOpenChange={setEmojiOpen}
          placement="topLeft"
        >
          <span title="表情"><SmileOutlined /></span>
        </Popover>
        <span title="图片" onClick={() => imageInputRef.current?.click()}>
          <PictureOutlined />
        </span>
        <span title="文件" onClick={() => fileInputRef.current?.click()}>
          <FolderOutlined />
        </span>

        {/* 隐藏的文件输入 */}
        <input
          ref={imageInputRef}
          type="file"
          accept="image/*"
          style={{ display: 'none' }}
          onChange={handleFileSelect}
        />
        <input
          ref={fileInputRef}
          type="file"
          style={{ display: 'none' }}
          onChange={handleFileSelect}
        />
      </div>
      <div className="chat-input-box">
        <textarea
          ref={textareaRef}
          value={text}
          onChange={handleChange}
          onKeyDown={handleKeyDown}
          placeholder="输入消息，Enter 发送，Shift+Enter 换行"
          disabled={disabled}
          rows={1}
        />
        <button
          className="chat-input-send"
          onClick={handleSend}
          disabled={disabled || !text.trim()}
        >
          <SendOutlined /> 发送
        </button>
      </div>
    </div>
  );
};

export default MessageInput;
