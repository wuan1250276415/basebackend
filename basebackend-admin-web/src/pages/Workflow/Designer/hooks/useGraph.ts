import { useEffect, useRef, useState } from 'react'
import { Graph } from '@antv/x6'
import { getGraphOptions } from '../utils/graph-config'
import { registerBpmnShapes } from '../utils/register-shapes'

import { Snapline } from '@antv/x6-plugin-snapline'
import { Keyboard } from '@antv/x6-plugin-keyboard'
import { Selection } from '@antv/x6-plugin-selection'
import { History } from '@antv/x6-plugin-history'
import { Clipboard } from '@antv/x6-plugin-clipboard'
// import { Scroller } from '@antv/x6-plugin-scroller' // Included in core now or separate? Usually separate package but recent versions include simple scroller in options or plugin.
// Checking package.json... dependencies are separate.
// I will just use basic features provided by Graph Options if plugins are loaded, or load them explicitly.
// Actually X6 v2+ plugins are modular.

export const useGraph = (containerRef: React.RefObject<HTMLDivElement>) => {
    const graphRef = useRef<Graph | null>(null)
    const [isReady, setIsReady] = useState(false)

    useEffect(() => {
        if (!containerRef.current || graphRef.current) return

        registerBpmnShapes()

        const graph = new Graph(getGraphOptions(containerRef.current))

        // Register plugins
        graph.use(
            new Snapline({
                enabled: true,
            })
        )
        graph.use(
            new Keyboard({
                enabled: true,
            })
        )
        graph.use(
            new Selection({
                enabled: true,
                multiple: true,
                rubberband: true,
                showNodeSelectionBox: true,
            })
        )
        graph.use(
            new History({
                enabled: true,
            })
        )
        graph.use(
            new Clipboard({
                enabled: true,
            })
        )

        // Key bindings
        graph.bindKey(['meta+c', 'ctrl+c'], () => {
            const cells = graph.getSelectedCells()
            if (cells.length) {
                graph.copy(cells)
            }
            return false
        })
        graph.bindKey(['meta+v', 'ctrl+v'], () => {
            if (!graph.isClipboardEmpty()) {
                const cells = graph.paste({ offset: 32 })
                graph.cleanSelection()
                graph.select(cells)
            }
            return false
        })
        graph.bindKey(['meta+z', 'ctrl+z'], () => {
            if (graph.canUndo()) {
                graph.undo()
            }
            return false
        })

        // Delete
        graph.bindKey(['backspace', 'delete'], () => {
            const cells = graph.getSelectedCells()
            if (cells.length) {
                graph.removeCells(cells)
            }
        })


        graphRef.current = graph
        setIsReady(true)

        return () => {
            graph.dispose()
            graphRef.current = null
        }
    }, [containerRef])

    return { graph: graphRef.current, isReady }
}
