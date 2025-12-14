import React, { useEffect, useState } from 'react'
import { Form, Input, Select, Divider } from 'antd'
import { Graph, Cell } from '@antv/x6'

interface ConfigPanelProps {
    graph: Graph | null
}

export const ConfigPanel: React.FC<ConfigPanelProps> = ({ graph }) => {
    const [selectedCell, setSelectedCell] = useState<Cell | null>(null)
    const [form] = Form.useForm()

    useEffect(() => {
        if (!graph) return

        const handleSelectionChanged = ({ selected }: { selected: Cell[] }) => {
            if (selected.length === 1) {
                setSelectedCell(selected[0])
                const data = selected[0].getData() || {}
                // Also get label from attrs if simple
                const label = selected[0].getAttrByPath('label/text')

                form.setFieldsValue({
                    label: label || '',
                    id: selected[0].id,
                    ...data
                })
            } else {
                setSelectedCell(null)
            }
        }

        graph.on('selection:changed', handleSelectionChanged)

        // Check if cell is clicked but selection might not trigger if already selected?
        // Selection plugin handles it.

        return () => {
            graph.off('selection:changed', handleSelectionChanged)
        }
    }, [graph, form])

    const handleValuesChange = (changedValues: any, allValues: any) => {
        if (!selectedCell) return

        // Update Label
        if ('label' in changedValues) {
            selectedCell.setAttrByPath('label/text', changedValues.label)
        }

        // Update Data
        selectedCell.setData(allValues)
    }

    if (!selectedCell) {
        return (
            <div style={{ width: 300, borderLeft: '1px solid #ddd', padding: 20 }}>
                <h3>Properties</h3>
                <div style={{ color: '#999' }}>Select a node to edit properties.</div>
            </div>
        )
    }

    const isEdge = selectedCell.isEdge()

    return (
        <div style={{ width: 300, borderLeft: '1px solid #ddd', padding: 20 }}>
            <h3>{isEdge ? 'Sequence Flow' : 'Node Properties'}</h3>
            <Form
                form={form}
                layout="vertical"
                onValuesChange={handleValuesChange}
            >
                <Form.Item label="ID" name="id">
                    <Input disabled />
                </Form.Item>

                <Form.Item label="Label" name="label">
                    <Input />
                </Form.Item>

                {!isEdge && (
                    <>
                        <Divider />
                        <Form.Item label="Description" name="description">
                            <Input.TextArea rows={2} />
                        </Form.Item>
                        {/* Custom fields based on node type */}
                        {/* Eg. Assignee for User Task */}
                        <Form.Item label="Assignee" name="assignee">
                            <Input placeholder="User ID or Expr" />
                        </Form.Item>
                    </>
                )}
            </Form>
        </div>
    )
}
