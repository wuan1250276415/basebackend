import React, { useEffect, useRef } from 'react';
import { Row, Col, Checkbox, Empty, Button, Space } from 'antd';
import { DownloadOutlined, DeleteOutlined, LikeOutlined, EyeOutlined } from '@ant-design/icons';

interface PhotoGridProps {
  photos: any[];
  multiSelect?: boolean;
  selectedIds?: string[];
  onSelectionChange?: (ids: string[]) => void;
  onPhotoClick?: (photo: any) => void;
}

const PhotoGrid: React.FC<PhotoGridProps> = ({ photos, multiSelect, selectedIds = [], onSelectionChange, onPhotoClick }) => {
  const toggleSelection = (id: string) => {
    if (selectedIds.includes(id)) {
      onSelectionChange?.(selectedIds.filter(i => i !== id));
    } else {
      onSelectionChange?.([...selectedIds, id]);
    }
  };

  if (!photos || photos.length === 0) {
    return <Empty description="暂无照片" />;
  }

  return (
    <Row gutter={[16, 16]}>
      {photos.map(photo => (
        <Col xs={12} sm={8} md={6} lg={6} key={photo.id}>
          <div 
            className="photo-card card-radius hover-shadow"
            style={{ position: 'relative', paddingTop: '100%', background: '#eee', overflow: 'hidden', cursor: 'pointer' }}
            onClick={() => onPhotoClick?.(photo)}
          >
            <img src={photo.url || 'https://via.placeholder.com/300'} alt={photo.name} style={{ position: 'absolute', top: 0, left: 0, width: '100%', height: '100%', objectFit: 'cover' }} loading="lazy" />
            
            <div className="photo-overlay" style={{ position: 'absolute', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(0,0,0,0.4)', opacity: 0, transition: 'opacity 0.3s', display: 'flex', flexDirection: 'column', justifyContent: 'space-between', padding: 8 }}>
              <div onClick={e => e.stopPropagation()}>
                {multiSelect && (
                  <Checkbox 
                    checked={selectedIds.includes(photo.id)} 
                    onChange={() => toggleSelection(photo.id)} 
                    style={{ background: 'rgba(255,255,255,0.8)', padding: '2px 4px', borderRadius: 4 }}
                  />
                )}
              </div>
              <Space onClick={e => e.stopPropagation()}>
                <Button size="small" icon={<EyeOutlined />} onClick={() => onPhotoClick?.(photo)} />
                <Button size="small" icon={<DownloadOutlined />} />
                <Button size="small" icon={<LikeOutlined />} />
                <Button size="small" danger icon={<DeleteOutlined />} />
              </Space>
            </div>
            <style>{`.photo-card:hover .photo-overlay { opacity: 1 !important; }`}</style>
          </div>
        </Col>
      ))}
    </Row>
  );
};

export default PhotoGrid;
