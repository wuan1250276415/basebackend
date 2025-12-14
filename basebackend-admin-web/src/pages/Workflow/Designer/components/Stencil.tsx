import React from 'react'
import { Graph } from '@antv/x6'

interface StencilProps {
    graph: Graph | null
}

export const Stencil: React.FC<StencilProps> = () => {
    // Manual Drag Start Handler
    // We use native HTML5 drag and drop, passing data via dataTransfer.
    // The GraphCanvas handles the 'drop' event.

    return (
        <div className="stencil-container" style={{ width: 220, borderRight: '1px solid #ddd', padding: 10, background: '#fafafa', overflowY: 'auto' }}>
            <h3 style={{ marginBottom: 16 }}>Components</h3>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                <div
                    draggable
                    onDragStart={(e) => {
                        e.dataTransfer.setData('node-type', 'bpmn-start')
                        e.dataTransfer.setData('node-label', 'Start')
                    }}
                    style={{ padding: 10, border: '1px solid #ccc', cursor: 'grab', borderRadius: 4, textAlign: 'center' }}
                >
                    Start Event
                </div>

                <div
                    draggable
                    onDragStart={(e) => {
                        e.dataTransfer.setData('node-type', 'bpmn-end')
                        e.dataTransfer.setData('node-label', 'End')
                    }}
                    style={{ padding: 10, border: '1px solid #ccc', cursor: 'grab', borderRadius: 4, textAlign: 'center' }}
                >
                    End Event
                </div>

                <div
                    draggable
                    onDragStart={(e) => {
                        e.dataTransfer.setData('node-type', 'bpmn-user-task')
                        e.dataTransfer.setData('node-label', 'User Task')
                    }}
                    style={{ padding: 10, border: '1px solid #ccc', cursor: 'grab', borderRadius: 4, textAlign: 'center' }}
                >
                    User Task
                </div>

                <div
                    draggable
                    onDragStart={(e) => {
                        e.dataTransfer.setData('node-type', 'bpmn-service-task')
                        e.dataTransfer.setData('node-label', 'Service Task')
                    }}
                    style={{ padding: 10, border: '1px solid #ccc', cursor: 'grab', borderRadius: 4, textAlign: 'center' }}
                >
                    Service Task
                </div>

                <div
                    draggable
                    onDragStart={(e) => {
                        e.dataTransfer.setData('node-type', 'bpmn-gateway')
                        e.dataTransfer.setData('node-label', '')
                    }}
                    style={{ padding: 10, border: '1px solid #ccc', cursor: 'grab', borderRadius: 4, textAlign: 'center' }}
                >
                    Gateway
                </div>

                <div
                    draggable
                    onDragStart={(e) => {
                        e.dataTransfer.setData('node-type', 'bpmn-custom')
                        e.dataTransfer.setData('node-label', 'Biz Node')
                    }}
                    style={{ padding: 10, border: '1px solid #ccc', cursor: 'grab', borderRadius: 4, textAlign: 'center' }}
                >
                    Business Node
                </div>
            </div>
        </div>
    )
}
