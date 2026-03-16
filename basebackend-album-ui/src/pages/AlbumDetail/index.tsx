import React, { useState } from 'react';
import { Typography, Space, Button, Modal, Input } from 'antd';
import { UploadOutlined, EditOutlined, ShareAltOutlined, DeleteOutlined } from '@ant-design/icons';
import PhotoGrid from '../../components/PhotoGrid';
import UploadZone from '../../components/UploadZone';

const { Title, Text } = Typography;

const AlbumDetail: React.FC = () => {
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [isShareModalOpen, setIsShareModalOpen] = useState(false);
  const photos = Array.from({ length: 10 }).map((_, i) => ({ id: `p${i}`, name: `Photo ${i}` }));

  return (
    <div style={{ padding: 24 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <div>
          <Title level={3} style={{ margin: 0 }}>我的旅行相册</Title>
          <Text type="secondary">记录美好的回忆</Text>
        </div>
        <Space>
          <Button icon={<UploadOutlined />} type="primary">上传</Button>
          <Button icon={<ShareAltOutlined />} onClick={() => setIsShareModalOpen(true)}>分享</Button>
          <Button icon={<EditOutlined />}>编辑</Button>
          <Button icon={<DeleteOutlined />} danger>删除相册</Button>
        </Space>
      </div>

      {selectedIds.length > 0 && (
        <div style={{ marginBottom: 16, padding: 16, background: '#e6f7ff', borderRadius: 8 }}>
          <Space>
            <span>已选择 {selectedIds.length} 项</span>
            <Button danger size="small">批量删除</Button>
            <Button size="small">移动到...</Button>
          </Space>
        </div>
      )}

      <PhotoGrid photos={photos} multiSelect selectedIds={selectedIds} onSelectionChange={setSelectedIds} />

      <Modal title="分享相册" open={isShareModalOpen} onOk={() => setIsShareModalOpen(false)} onCancel={() => setIsShareModalOpen(false)}>
        <Space direction="vertical" style={{ width: '100%' }}>
          <Input value="https://album.fake/share/xyz" readOnly addonAfter={<Button type="text" style={{padding:0}}>复制</Button>} />
          <Input placeholder="设置密码（选填）" />
        </Space>
      </Modal>
    </div>
  );
};
export default AlbumDetail;
