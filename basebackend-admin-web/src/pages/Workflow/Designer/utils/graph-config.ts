import { Graph, Shape } from '@antv/x6'

export const getGraphOptions = (container: HTMLElement): Graph.Options => {
    return {
        container,
        grid: {
            size: 10,
            visible: true,
        },
        // Plugins key not directly in options for v2 usually, but some are allowed if integrated.
        // Safest to just configure basic options and let useGraph handle plugins.
        // clipboard, selecting, etc moved to useGraph or handled via plugins

        // Core features
        // resizing, rotating removed to avoid type errors, can be enabled via plugins or transforming
        // scroller removed
        connecting: {
            anchor: 'center',
            connectionPoint: 'boundary',
            allowBlank: false,
            highlight: true,
            router: 'manhattan',
            connector: {
                name: 'rounded',
                args: {
                    radius: 8,
                },
            },
            createEdge() {
                return new Shape.Edge({
                    attrs: {
                        line: {
                            stroke: '#a0a0a0',
                            strokeWidth: 2,
                            targetMarker: {
                                name: 'block',
                                width: 12,
                                height: 8,
                            },
                        },
                    },
                    zIndex: 0,
                })
            },
            validateConnection({ targetMagnet }) {
                return !!targetMagnet
            },
        },
    }
}
