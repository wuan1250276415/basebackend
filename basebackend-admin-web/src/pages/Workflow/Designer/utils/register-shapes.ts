import { Graph } from '@antv/x6'

// 注册 BPMN 基础节点
export const registerBpmnShapes = () => {
    // Start Event
    Graph.registerNode(
        'bpmn-start',
        {
            inherit: 'circle',
            width: 40,
            height: 40,
            attrs: {
                body: {
                    strokeWidth: 1.5,
                    stroke: '#1890ff',
                    fill: '#fff',
                },
                label: {
                    text: 'Start',
                    refY: 50
                }
            },
        },
        true
    )

    // End Event
    Graph.registerNode(
        'bpmn-end',
        {
            inherit: 'circle',
            width: 40,
            height: 40,
            attrs: {
                body: {
                    strokeWidth: 3,
                    stroke: '#ff4d4f',
                    fill: '#fff',
                },
                label: {
                    text: 'End',
                    refY: 50
                }
            },
        },
        true
    )

    // User Task
    Graph.registerNode(
        'bpmn-user-task',
        {
            inherit: 'rect',
            width: 100,
            height: 80,
            attrs: {
                body: {
                    strokeWidth: 1.5,
                    stroke: '#1890ff',
                    fill: '#e6f7ff',
                    rx: 10,
                    ry: 10,
                },
                label: {
                    fill: '#333',
                    fontSize: 12,
                },
                icon: {
                    // Note: In real app, might use image or path for User Icon
                    // For simplicity, we use text or small SVG path if we had one.
                    // Using a simple unicode char as placeholder or just label.
                }
            },
        },
        true
    )

    // Service Task
    Graph.registerNode(
        'bpmn-service-task',
        {
            inherit: 'rect',
            width: 100,
            height: 80,
            attrs: {
                body: {
                    strokeWidth: 1.5,
                    stroke: '#52c41a',
                    fill: '#f6ffed',
                    rx: 10,
                    ry: 10,
                },
                label: {
                    fill: '#333',
                    fontSize: 12,
                },
            },
        },
        true
    )

    // Exclusive Gateway
    Graph.registerNode(
        'bpmn-gateway',
        {
            inherit: 'polygon',
            width: 40,
            height: 40,
            attrs: {
                body: {
                    strokeWidth: 1.5,
                    stroke: '#faad14',
                    fill: '#fffbe6',
                    refPoints: '0,10 10,0 20,10 10,20',
                },
                label: {
                    text: '',
                    refY: 50
                }
            },
        },
        true
    )

    // Custom Business Node
    Graph.registerNode(
        'bpmn-custom',
        {
            inherit: 'rect',
            width: 120,
            height: 60,
            attrs: {
                body: {
                    strokeWidth: 2,
                    stroke: '#722ed1',
                    fill: '#f9f0ff',
                    rx: 4,
                    ry: 4,
                },
                label: {
                    fill: '#333',
                    fontSize: 12,
                },
            },
        },
        true
    )
}
