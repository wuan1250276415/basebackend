import React, { useEffect, useState } from 'react';
import { Avatar, List } from 'antd';
import { TeamOutlined } from '@ant-design/icons';
import type { Group } from '@/types';
import { getGroupInfo } from '@/services/groupApi';

interface GroupListProps {
  /** 用户所在的群 ID 列表（由父级传入或通过 API 加载） */
  groupIds?: number[];
}

/** 群组列表组件 */
const GroupList: React.FC<GroupListProps> = ({ groupIds = [] }) => {
  const [groups, setGroups] = useState<Group[]>([]);

  useEffect(() => {
    if (groupIds.length === 0) return;
    Promise.all(groupIds.map((id) => getGroupInfo(id).catch(() => null)))
      .then((results) => setGroups(results.filter((g): g is Group => g !== null)));
  }, [groupIds]);

  if (groups.length === 0 && groupIds.length === 0) {
    return (
      <div className="contacts-body">
        <div className="empty-state" style={{ padding: 40 }}>
          <span>暂无群组</span>
        </div>
      </div>
    );
  }

  return (
    <div className="contacts-body">
      <List
        dataSource={groups}
        renderItem={(group) => (
          <div className="contact-item" key={group.groupId}>
            <Avatar
              className="contact-avatar"
              size={38}
              src={group.avatar || undefined}
              icon={!group.avatar ? <TeamOutlined /> : undefined}
              shape="square"
            />
            <div className="contact-info">
              <div className="contact-name">{group.name}</div>
              <div className="contact-status">{group.memberCount} 人</div>
            </div>
          </div>
        )}
      />
    </div>
  );
};

export default GroupList;
