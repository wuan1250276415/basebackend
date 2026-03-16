/**
 * localStorage 封装工具函数单元测试
 */
import { describe, it, expect, beforeEach } from 'vitest';
import { getStorage, setStorage, removeStorage } from '@/utils/storage';

describe('storage 工具函数', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  describe('setStorage', () => {
    it('应将对象序列化为 JSON 存入 localStorage', () => {
      const data = { name: '张三', age: 25 };
      setStorage('user', data);
      expect(localStorage.getItem('user')).toBe(JSON.stringify(data));
    });

    it('应正确存储字符串值', () => {
      setStorage('token', 'abc123');
      expect(localStorage.getItem('token')).toBe('"abc123"');
    });

    it('应正确存储数字值', () => {
      setStorage('count', 42);
      expect(localStorage.getItem('count')).toBe('42');
    });

    it('应正确存储布尔值', () => {
      setStorage('flag', true);
      expect(localStorage.getItem('flag')).toBe('true');
    });

    it('应正确存储 null', () => {
      setStorage('empty', null);
      expect(localStorage.getItem('empty')).toBe('null');
    });
  });

  describe('getStorage', () => {
    it('应从 localStorage 读取并解析 JSON 对象', () => {
      const data = { name: '李四', roles: ['admin'] };
      localStorage.setItem('user', JSON.stringify(data));
      expect(getStorage<typeof data>('user')).toEqual(data);
    });

    it('键不存在时应返回 null', () => {
      expect(getStorage('nonexistent')).toBeNull();
    });

    it('JSON 解析失败时应返回 null', () => {
      localStorage.setItem('bad', '{invalid json}');
      expect(getStorage('bad')).toBeNull();
    });

    it('应正确读取数字', () => {
      localStorage.setItem('num', '99');
      expect(getStorage<number>('num')).toBe(99);
    });
  });

  describe('removeStorage', () => {
    it('应从 localStorage 移除指定键', () => {
      localStorage.setItem('key', '"value"');
      removeStorage('key');
      expect(localStorage.getItem('key')).toBeNull();
    });

    it('移除不存在的键不应报错', () => {
      expect(() => removeStorage('nonexistent')).not.toThrow();
    });
  });

  describe('setStorage + getStorage 往返一致性', () => {
    it('存取对象应保持一致', () => {
      const data = { id: 1, items: [1, 2, 3], nested: { a: true } };
      setStorage('data', data);
      expect(getStorage('data')).toEqual(data);
    });

    it('存取数组应保持一致', () => {
      const arr = [1, 'two', { three: 3 }];
      setStorage('arr', arr);
      expect(getStorage('arr')).toEqual(arr);
    });
  });
});
