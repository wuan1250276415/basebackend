import React, { useState } from 'react';
import { Tabs } from 'antd';
import ContactList from '@/components/contact/ContactList';
import GroupList from '@/components/group/GroupList';

/** 通讯录页面 */
const ContactsPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState('friends');

  return (
    <div className="contacts-page">
      <div className="contacts-header">通讯录</div>
      <div style={{ padding: '0 16px' }}>
        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          items={[
            { key: 'friends', label: '好友', children: <ContactList /> },
            { key: 'groups', label: '群组', children: <GroupList /> },
          ]}
        />
      </div>
    </div>
  );
};

export default ContactsPage;
