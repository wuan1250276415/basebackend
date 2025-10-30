import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import LanguageDetector from 'i18next-browser-languagedetector';

// 导入语言包
import zhCN from './locales/zh-CN';
import enUS from './locales/en-US';

// 初始化 i18next
i18n
  // 检测用户语言
  .use(LanguageDetector)
  // 将 i18next 实例传递给 react-i18next
  .use(initReactI18next)
  // 初始化配置
  .init({
    resources: {
      'zh-CN': {
        translation: zhCN,
      },
      'en-US': {
        translation: enUS,
      },
    },
    // 默认语言
    fallbackLng: 'zh-CN',
    // 支持的语言
    supportedLngs: ['zh-CN', 'en-US'],
    // 调试模式
    debug: false,
    // 检测选项
    detection: {
      // 检测顺序
      order: ['localStorage', 'navigator'],
      // 缓存用户语言
      caches: ['localStorage'],
      // localStorage 中的 key
      lookupLocalStorage: 'i18nextLng',
    },
    // 插值配置
    interpolation: {
      escapeValue: false, // React 已经做了转义处理
    },
    // 命名空间
    ns: ['translation'],
    defaultNS: 'translation',
  });

export default i18n;
