import { useEffect, useState } from 'react'
import { Button, Card, Empty, Space, Spin, Typography, message } from 'antd'
import {
  CloudDownloadOutlined,
  FileTextOutlined,
  ReloadOutlined,
  SnippetsOutlined,
} from '@ant-design/icons'
import SwaggerUI from 'swagger-ui-react'
import 'swagger-ui-react/swagger-ui.css'

const { Title, Paragraph, Text } = Typography

const OPENAPI_JSON_URL = '/api/admin/openapi/spec.json'
const OPENAPI_YAML_URL = '/api/admin/openapi/spec.yaml'
const SDK_ZIP_URL = '/api/admin/openapi/sdk/typescript'

type DownloadTarget = 'json' | 'yaml' | 'sdk'

const resolveOpenApiDocument = (raw: any): Record<string, unknown> | null => {
  if (!raw) {
    return null
  }

  if (raw.openapi) {
    return raw as Record<string, unknown>
  }

  if (raw.data) {
    try {
      if (typeof raw.data === 'string') {
        const parsed = JSON.parse(raw.data)
        if (parsed?.openapi) {
          return parsed
        }
      } else if (typeof raw.data === 'object' && raw.data.openapi) {
        return raw.data as Record<string, unknown>
      }
    } catch (error) {
      console.warn('无法解析嵌套的 OpenAPI 数据', error)
      return null
    }
  }

  if (typeof raw === 'string') {
    try {
      const parsed = JSON.parse(raw)
      return parsed?.openapi ? parsed : null
    } catch (error) {
      console.warn('无法解析字符串 OpenAPI 数据', error)
      return null
    }
  }

  return null
}

const ApiDocs = () => {
  const [spec, setSpec] = useState<Record<string, unknown> | null>(null)
  const [loadingSpec, setLoadingSpec] = useState(false)
  const [downloading, setDownloading] = useState<DownloadTarget | null>(null)

  const fetchSpec = async (silent?: boolean) => {
    setLoadingSpec(true)
    if (!silent) {
      setSpec(null)
    }
    try {
      const token = localStorage.getItem('token')
      const response = await fetch(OPENAPI_JSON_URL, {
        cache: 'no-store',
        headers: {
          Accept: 'application/json',
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
      })

      if (!response.ok) {
        throw new Error(`加载失败: ${response.status}`)
      }

      const data = await response.json()
      const resolved = resolveOpenApiDocument(data)
      if (!resolved) {
        throw new Error('OpenAPI 响应格式无效')
      }
      setSpec(resolved)
      if (!silent) {
        message.success('OpenAPI 规范已刷新')
      }
    } catch (error) {
      console.error('加载 OpenAPI 规范失败', error)
      message.error('加载 OpenAPI 规范失败，请稍后重试')
    } finally {
      setLoadingSpec(false)
    }
  }

  useEffect(() => {
    fetchSpec(true)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const downloadFile = async (url: string, filename: string, target: DownloadTarget) => {
    setDownloading(target)
    try {
      const token = localStorage.getItem('token')
      const response = await fetch(url, {
        headers: token ? { Authorization: `Bearer ${token}` } : undefined,
      })

      if (!response.ok) {
        throw new Error(`下载失败: ${response.status}`)
      }

      const blob = await response.blob()
      const link = document.createElement('a')
      const blobUrl = window.URL.createObjectURL(blob)
      link.href = blobUrl
      link.download = filename
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(blobUrl)
      message.success(`${filename} 下载成功`)
    } catch (error) {
      console.error('下载文件失败', error)
      message.error('下载失败，请稍后重试')
    } finally {
      setDownloading(null)
    }
  }

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Card>
        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          <Title level={4} style={{ margin: 0 }}>
            OpenAPI 文档与 SDK
          </Title>
          <Paragraph type="secondary" style={{ marginBottom: 0 }}>
            <Text>实时展示后台管理服务的 OpenAPI 规范，并提供 SDK 与规范文件下载能力。</Text>
          </Paragraph>
          <Space wrap>
            <Button
              icon={<ReloadOutlined />}
              loading={loadingSpec}
              onClick={() => fetchSpec()}
              type="default"
            >
              刷新文档
            </Button>
            <Button
              icon={<FileTextOutlined />}
              loading={downloading === 'json'}
              onClick={() =>
                downloadFile(OPENAPI_JSON_URL, 'admin-api-openapi.json', 'json')
              }
            >
              下载 JSON 规范
            </Button>
            <Button
              icon={<SnippetsOutlined />}
              loading={downloading === 'yaml'}
              onClick={() =>
                downloadFile(OPENAPI_YAML_URL, 'admin-api-openapi.yaml', 'yaml')
              }
            >
              下载 YAML 规范
            </Button>
            <Button
              type="primary"
              icon={<CloudDownloadOutlined />}
              loading={downloading === 'sdk'}
              onClick={() =>
                downloadFile(SDK_ZIP_URL, 'basebackend-admin-sdk.zip', 'sdk')
              }
            >
              下载 TypeScript SDK
            </Button>
          </Space>
        </Space>
      </Card>

      <Card bodyStyle={{ padding: 0, minHeight: 480 }}>
        {loadingSpec ? (
          <div style={{ padding: 48, textAlign: 'center' }}>
            <Spin />
          </div>
        ) : spec ? (
          <SwaggerUI
            spec={spec}
            docExpansion="none"
            defaultModelsExpandDepth={1}
            supportedSubmitMethods={['get', 'post', 'put', 'delete', 'patch']}
          />
        ) : (
          <div style={{ padding: 48 }}>
            <Empty description="尚未加载到 OpenAPI 规范" />
          </div>
        )}
      </Card>
    </Space>
  )
}

export default ApiDocs
