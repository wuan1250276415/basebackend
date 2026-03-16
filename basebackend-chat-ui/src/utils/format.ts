import dayjs from 'dayjs';

/**
 * 智能格式化时间：
 * - 今天：HH:mm
 * - 昨天：昨天 HH:mm
 * - 本周：周X HH:mm
 * - 今年：MM-DD HH:mm
 * - 更早：YYYY-MM-DD
 */
export function formatTime(time: string | number | Date): string {
  const t = dayjs(time);
  const now = dayjs();

  if (!t.isValid()) return '';

  if (t.isSame(now, 'day')) {
    return t.format('HH:mm');
  }
  if (t.isSame(now.subtract(1, 'day'), 'day')) {
    return `昨天 ${t.format('HH:mm')}`;
  }
  if (t.isSame(now, 'week')) {
    const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
    return `${weekdays[t.day()] ?? ''} ${t.format('HH:mm')}`;
  }
  if (t.isSame(now, 'year')) {
    return t.format('MM-DD HH:mm');
  }
  return t.format('YYYY-MM-DD');
}

/**
 * 消息预览文本：将消息类型转换为文字摘要
 */
export function messagePreview(type: number, content: string | null): string {
  switch (type) {
    case 1: return content ?? '';
    case 2: return '[图片]';
    case 3: return '[文件]';
    case 4: return '[语音]';
    case 5: return '[视频]';
    case 6: return '[位置]';
    case 7: return '[名片]';
    case 8: return '[表情]';
    case 9: return '消息已撤回';
    case 10: return '[系统消息]';
    case 11: return '[聊天记录]';
    default: return content ?? '';
  }
}

/**
 * 格式化文件大小
 */
export function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  return `${(bytes / (1024 * 1024 * 1024)).toFixed(1)} GB`;
}

/**
 * 生成客户端消息 ID
 */
export function generateClientMsgId(): string {
  return `web-${Date.now()}-${Math.random().toString(36).slice(2, 9)}`;
}
