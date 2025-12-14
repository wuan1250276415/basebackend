import React from 'react'
import { Button, Space, Tooltip, message } from 'antd'
import {
    SaveOutlined,
    UndoOutlined,
    RedoOutlined,
    ZoomInOutlined,
    ZoomOutOutlined,
    ExpandOutlined,
} from '@ant-design/icons'
import { Graph } from '@antv/x6'

interface ToolbarProps {
    graph: Graph | null
}

export const Toolbar: React.FC<ToolbarProps> = ({ graph }) => {
    const handleSave = () => {
        if (!graph) return
        const data = graph.toJSON()
        console.log('Saved Data:', JSON.stringify(data, null, 2))
        message.success('流程图数据已打印到控制台 (Mock Save)')
        // In real app, call API to save
    }

    const handleUndo = () => {
        if (graph?.canUndo()) graph.undo()
    }

    const handleRedo = () => {
        if (graph?.canRedo()) graph.redo()
    }

    const handleZoomIn = () => {
        graph?.zoom(0.1)
    }

    const handleZoomOut = () => {
        graph?.zoom(-0.1)
    }

    const handleFit = () => {
        graph?.zoomToFit({ padding: 20 })
    }

    return (
        <div style={{ padding: '8px 16px', borderBottom: '1px solid #ddd', background: '#f5f5f5', display: 'flex', justifyContent: 'space-between' }}>
            <Space>
                <Tooltip title="Save">
                    <Button icon={<SaveOutlined />} onClick={handleSave}>Save</Button>
                </Tooltip>
            </Space>

            <Space>
                <Tooltip title="Undo">
                    <Button icon={<UndoOutlined />} onClick={handleUndo} />
                </Tooltip>
                <Tooltip title="Redo">
                    <Button icon={<RedoOutlined />} onClick={handleRedo} />
                </Tooltip>
                <Tooltip title="Zoom In">
                    <Button icon={<ZoomInOutlined />} onClick={handleZoomIn} />
                </Tooltip>
                <Tooltip title="Zoom Out">
                    <Button icon={<ZoomOutOutlined />} onClick={handleZoomOut} />
                </Tooltip>
                <Tooltip title="Fit Content">
                    <Button icon={<ExpandOutlined />} onClick={handleFit} />
                </Tooltip>
            </Space>
        </div>
    )
}
