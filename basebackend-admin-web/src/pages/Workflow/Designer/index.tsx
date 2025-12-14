import React, { useState } from 'react'
import { Graph } from '@antv/x6'
import { Toolbar } from './components/Toolbar'
import { Stencil } from './components/Stencil'
import { ConfigPanel } from './components/ConfigPanel'
import { GraphCanvas } from './components/GraphCanvas'

const Designer: React.FC = () => {
    const [graph, setGraph] = useState<Graph | null>(null)

    return (
        <div style={{ height: 'calc(100vh - 100px)', display: 'flex', flexDirection: 'column', border: '1px solid #ddd' }}>
            <Toolbar graph={graph} />
            <div style={{ display: 'flex', flex: 1, overflow: 'hidden' }}>
                <Stencil graph={graph} />
                <GraphCanvas onGraphReady={setGraph} />
                <ConfigPanel graph={graph} />
            </div>
        </div>
    )
}

export default Designer
