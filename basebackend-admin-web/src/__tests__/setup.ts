// 测试环境全局配置
import '@testing-library/jest-dom';

/**
 * Node.js 22+ 内置的 localStorage 缺少完整 Storage API（如 clear/removeItem）。
 * 这里用一个符合 Storage 接口的 Map 实现替换，确保 jsdom 测试正常运行。
 */
const store = new Map<string, string>();

const localStorageMock: Storage = {
  getItem(key: string): string | null {
    return store.get(key) ?? null;
  },
  setItem(key: string, value: string): void {
    store.set(key, String(value));
  },
  removeItem(key: string): void {
    store.delete(key);
  },
  clear(): void {
    store.clear();
  },
  get length(): number {
    return store.size;
  },
  key(index: number): string | null {
    const keys = Array.from(store.keys());
    return keys[index] ?? null;
  },
};

Object.defineProperty(globalThis, 'localStorage', {
  value: localStorageMock,
  writable: true,
  configurable: true,
});
