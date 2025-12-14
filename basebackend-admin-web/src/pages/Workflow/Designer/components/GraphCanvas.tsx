import React, { useRef, useEffect } from 'react'
import { useGraph } from '../hooks/useGraph'
import { Graph } from '@antv/x6'

interface GraphCanvasProps {
    onGraphReady: (graph: Graph) => void
}

export const GraphCanvas: React.FC<GraphCanvasProps> = ({ onGraphReady }) => {
    const containerRef = useRef<HTMLDivElement>(null)
    const { graph, isReady } = useGraph(containerRef)

    useEffect(() => {
        if (isReady && graph) {
            onGraphReady(graph)
        }
    }, [isReady, graph, onGraphReady])

    // Handle Drop on Canvas
    const handleDrop = (e: React.DragEvent) => {
        e.preventDefault()
        if (!graph) return

        const type = e.dataTransfer.getData('node-type')
        const label = e.dataTransfer.getData('node-label')

        if (type) {
            const point = graph.clientToLocal(e.clientX, e.clientY)
            graph.addNode({
                shape: type,
                x: point.x - 40, // center offset assumption
                y: point.y - 20,
                label: label
            })
        }
    }

    const handleDragOver = (e: React.DragEvent) => {
        e.preventDefault() // Allow Drop
    }

    return (
        <div
            className="graph-canvas-container"
            style={{ flex: 1, height: '100%', position: 'relative', overflow: 'hidden' }}
            onDrop={handleDrop}
            onDragOver={handleDragOver}
        >
            <div
                ref={containerRef}
                style={{ width: '100%', height: '100%' }}
            />
        </div>
    )
}
