import React from 'react';
import { Tag } from 'antd';

const PRIORITY_MAP: Record<number, { color: string; label: string }> = {
  1: { color: 'red', label: '紧急' },
  2: { color: 'orange', label: '高' },
  3: { color: 'blue', label: '中' },
  4: { color: 'green', label: '低' },
  5: { color: 'default', label: '最低' },
};

interface PriorityBadgeProps {
  priority: number;
}

const PriorityBadge: React.FC<PriorityBadgeProps> = ({ priority }) => {
  const config = PRIORITY_MAP[priority] ?? { color: 'default', label: `P${priority}` };
  return <Tag color={config.color}>{config.label}</Tag>;
};

export default PriorityBadge;
