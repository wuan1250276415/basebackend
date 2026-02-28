import React, { useEffect } from 'react';
import { Modal, Button, Space, Typography, Layout } from 'antd';
import { LeftOutlined, RightOutlined, DownloadOutlined, LikeOutlined, MessageOutlined, DeleteOutlined } from '@ant-design/icons';

const { Sider, Content, Footer } = Layout;
const { Text } = Typography;

interface PhotoViewerProps {
  visible: boolean;
  photo: any;
  onClose: () => void;
  onPrev: () => void;
  onNext: () => void;
}

const PhotoViewer: React.FC<PhotoViewerProps> = ({ visible, photo, onClose, onPrev, onNext }) => {
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (!visible) return;
      if (e.key === 'ArrowLeft') onPrev();
      if (e.key === 'ArrowRight') onNext();
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [visible, onPrev, onNext, onClose]);

  if (!photo) return null;

  return (
    <Modal open={visible} footer={null} onCancel={onClose} width="90vw" style={{ top: 20 }} bodyStyle={{ height: '80vh', padding: 0 }}>
      <Layout style={{ height: '100%', background: '#fff' }}>
        <Content style={{ position: 'relative', display: 'flex', alignItems: 'center', justifyContent: 'center', background: '#000' }}>
          <Button icon={<LeftOutlined />} onClick={onPrev} style={{ position: 'absolute', left: 20 }} ghost />
          <img src={photo.url || 'https://via.placeholder.com/800'} alt={photo.name} style={{ maxHeight: '100%', maxWidth: '100%', objectFit: 'contain' }} />
          <Button icon={<RightOutlined />} onClick={onNext} style={{ position: 'absolute', right: 20 }} ghost />
        </Content>
        <Sider width={300} theme="light" style={{ padding: 16, borderLeft: '1px solid #f0f0f0' }}>
          <Text strong>{photo.name || '未命名照片'}</Text>
          <div style={{ margin: '16px 0' }}>
            <Text type="secondary">拍摄时间: {photo.date || '未知'}</Text>
          </div>
          <Space>
            <Button icon={<DownloadOutlined />} />
            <Button icon={<LikeOutlined />} />
            <Button icon={<MessageOutlined />} />
            <Button icon={<DeleteOutlined />} danger />
          </Space>
          <div style={{ marginTop: 24 }}>
            <Text strong>评论</Text>
            <div style={{ marginTop: 8, color: '#999' }}>暂无评论</div>
          </div>
        </Sider>
      </Layout>
    </Modal>
  );
};
export default PhotoViewer;
