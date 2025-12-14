import React, { useEffect, useRef, useState } from 'react'
import { Button, Space, message, Input, Modal, Upload } from 'antd'
import BpmnModeler from 'bpmn-js/lib/Modeler'
import 'bpmn-js/dist/assets/diagram-js.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn-embedded.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn-codes.css'

import {
    FolderOpenOutlined,
    PlusOutlined,
    ZoomInOutlined,
    ZoomOutOutlined,
    ExpandOutlined,
    CloudUploadOutlined,
} from '@ant-design/icons'
import { deployProcessDefinition } from '@/api/workflow/processDefinition'

const BpmnDesigner: React.FC = () => {
    const containerRef = useRef<HTMLDivElement>(null)
    const modelerRef = useRef<BpmnModeler | null>(null)

    // Deployment Modal
    const [deployModalVisible, setDeployModalVisible] = useState(false)
    const [processName, setProcessName] = useState('')

    useEffect(() => {
        if (!containerRef.current) return

        const modeler = new BpmnModeler({
            container: containerRef.current,
            keyboard: {
                bindTo: window
            }
        })

        modelerRef.current = modeler

        createDiagram()

        return () => {
            modeler.destroy()
        }
    }, [])

    const createDiagram = () => {
        const initialXml = `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" xmlns:camunda="http://camunda.org/schema/1.0/bpmn" id="Definitions_1" targetNamespace="http://bpmn.io/schema/bpmn">
  <bpmn:process id="Process_1" isExecutable="true" camunda:historyTimeToLive="180">
    <bpmn:startEvent id="StartEvent_1" />
  </bpmn:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="Process_1">
      <bpmndi:BPMNShape id="_BPMNShape_StartEvent_2" bpmnElement="StartEvent_1">
        <dc:Bounds x="173" y="102" width="36" height="36" />
      </bpmndi:BPMNShape>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>`


        // Use any cast to avoid strict type checking on bpmn-js internal types if not available
        modelerRef.current?.importXML(initialXml).then((result: any) => {
            const { warnings } = result
            if (warnings && warnings.length) {
                console.warn('Import warnings', warnings)
            }
            const canvas: any = modelerRef.current?.get('canvas')
            canvas?.zoom('fit-viewport')
        }).catch((err: any) => {
            console.error('Initial Diagram Loading Error', err)
        })
    }

    const handleImport = (file: File) => {
        const reader = new FileReader()
        reader.onload = (e) => {
            const xml = e.target?.result as string
            modelerRef.current?.importXML(xml).catch((err: any) => {
                message.error('Import Failed: ' + err.message)
            })
        }
        reader.readAsText(file)
        return false // Prevent upload
    }

    const handleZoomIn = () => {
        const editorActions: any = modelerRef.current?.get('editorActions')
        editorActions?.trigger('stepZoom', { value: 1 })
    }

    const handleZoomOut = () => {
        const editorActions: any = modelerRef.current?.get('editorActions')
        editorActions?.trigger('stepZoom', { value: -1 })
    }

    const handleFit = () => {
        const canvas: any = modelerRef.current?.get('canvas')
        canvas?.zoom('fit-viewport')
    }

    const handleDeployClick = async () => {
        setDeployModalVisible(true)
    }

    const handleDeploySubmit = async () => {
        if (!processName) {
            message.warning('请输入流程名称')
            return
        }

        try {
            // Update Process Name in BPMN XML before saving
            // Directly update the business object to avoid visual element lookup issues
            const definitions = modelerRef.current?.getDefinitions()
            const process = definitions?.rootElements?.find((e: any) => e.$type === 'bpmn:Process')

            if (process) {
                process.name = processName
            }

            const result: any = await modelerRef.current?.saveXML({ format: true })
            const xml = result?.xml

            if (!xml) return

            // Create a File object from XML string
            const blob = new Blob([xml], { type: 'text/xml' })
            const file = new File([blob], `${processName}.bpmn`, { type: 'text/xml' })

            const response = await deployProcessDefinition({
                file: file,
                deploymentName: processName,
                name: processName
            })

            if (response.code === 200) {
                message.success('部署成功')
                setDeployModalVisible(false)
            } else {
                message.error(response.message || '部署失败')
            }
        } catch (err) {
            console.error(err)
            message.error('部署失败')
        }
    }

    return (
        <div style={{ height: 'calc(100vh - 100px)', display: 'flex', flexDirection: 'column' }}>
            <div style={{ padding: '8px 16px', background: '#f5f5f5', borderBottom: '1px solid #ddd', display: 'flex', justifyContent: 'space-between' }}>
                <Space>
                    <Button icon={<PlusOutlined />} onClick={createDiagram}>New</Button>
                    <Upload beforeUpload={handleImport} showUploadList={false} accept=".bpmn,.xml">
                        <Button icon={<FolderOpenOutlined />}>Open</Button>
                    </Upload>
                    <Button type="primary" icon={<CloudUploadOutlined />} onClick={handleDeployClick}>Deploy</Button>
                </Space>
                <Space>
                    <Button icon={<ZoomInOutlined />} onClick={handleZoomIn} />
                    <Button icon={<ZoomOutOutlined />} onClick={handleZoomOut} />
                    <Button icon={<ExpandOutlined />} onClick={handleFit} />
                </Space>
            </div>

            <div className="bpmn-container" ref={containerRef} style={{ flex: 1, position: 'relative' }} />

            <Modal
                title="部署流程"
                open={deployModalVisible}
                onOk={handleDeploySubmit}
                onCancel={() => setDeployModalVisible(false)}
            >
                <Input
                    placeholder="请输入流程部署名称"
                    value={processName}
                    onChange={e => setProcessName(e.target.value)}
                />
            </Modal>
        </div>
    )
}

export default BpmnDesigner
