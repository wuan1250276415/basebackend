import { List, Avatar, Tag } from 'antd';
import { UserOutlined } from '@ant-design/icons';
import type { AssigneeRank } from '@/api/ticketApi';

interface TopAssigneesProps {
  data: AssigneeRank[];
}

const TopAssignees: React.FC<TopAssigneesProps> = ({ data }) => {
  return (
    <List
      size="small"
      dataSource={data}
      style={{ maxHeight: 280, overflow: 'auto' }}
      renderItem={(item, index) => (
        <List.Item extra={<Tag>{item.avgResolutionHours}h 平均</Tag>}>
          <List.Item.Meta
            avatar={
              <Avatar
                size="small"
                icon={<UserOutlined />}
                style={{
                  backgroundColor: index < 3 ? ['#f5222d', '#fa8c16', '#faad14'][index] : '#d9d9d9',
                }}
              >
                {index + 1}
              </Avatar>
            }
            title={item.assigneeName}
            description={`已解决 ${item.resolvedCount} 个`}
          />
        </List.Item>
      )}
    />
  );
};

export default TopAssignees;
