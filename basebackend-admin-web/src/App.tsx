import { useEffect } from 'react'
import { BrowserRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from 'react-query'
import { ConfigProvider, theme as antdTheme } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import enUS from 'antd/locale/en_US'
import { useTranslation } from 'react-i18next'
import AppRouter from './router'
import { useThemeStore } from './stores/theme'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
    },
  },
})

function App() {
  const { i18n } = useTranslation()
  const { themeConfig, actualMode, updateThemeConfig } = useThemeStore()

  // 组件挂载时更新主题配置
  useEffect(() => {
    updateThemeConfig()
  }, [updateThemeConfig])

  // 根据语言选择 Ant Design 的语言包
  const locale = i18n.language === 'en-US' ? enUS : zhCN

  return (
    <ConfigProvider
      locale={locale}
      theme={{
        ...themeConfig,
        algorithm: actualMode === 'dark' ? antdTheme.darkAlgorithm : antdTheme.defaultAlgorithm,
      }}
    >
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <AppRouter />
        </BrowserRouter>
      </QueryClientProvider>
    </ConfigProvider>
  )
}

export default App
