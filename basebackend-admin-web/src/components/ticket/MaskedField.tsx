import { Tooltip } from 'antd';
import { LockOutlined } from '@ant-design/icons';

interface MaskedFieldProps {
  value: string | undefined | null;
  label?: string;
}

/**
 * 脱敏字段展示组件
 * 当值包含脱敏占位符（*）时，显示锁定图标和提示
 */
const MaskedField: React.FC<MaskedFieldProps> = ({ value, label }) => {
  if (!value) return <span style={{ color: '#999' }}>-</span>;

  const isMasked = value.includes('*');

  if (!isMasked) return <span>{value}</span>;

  return (
    <Tooltip title={`${label || '该字段'}已脱敏处理`}>
      <span style={{ color: '#8c8c8c' }}>
        {value} <LockOutlined style={{ fontSize: 12, marginLeft: 4 }} />
      </span>
    </Tooltip>
  );
};

export default MaskedField;
