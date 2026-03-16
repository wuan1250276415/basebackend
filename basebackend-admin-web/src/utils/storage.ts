/**
 * localStorage 类型安全封装
 * 提供 JSON 序列化/反序列化的存取操作，解析失败时安全返回 null
 */

/**
 * 从 localStorage 读取并解析 JSON 数据
 * @param key 存储键名
 * @returns 解析后的数据，不存在或解析失败返回 null
 */
export function getStorage<T>(key: string): T | null {
  try {
    const raw = localStorage.getItem(key);
    if (raw === null) {
      return null;
    }
    return JSON.parse(raw) as T;
  } catch {
    // JSON 解析失败，安全返回 null
    return null;
  }
}

/**
 * 将数据序列化为 JSON 并存入 localStorage
 * @param key 存储键名
 * @param value 要存储的数据
 */
export function setStorage<T>(key: string, value: T): void {
  localStorage.setItem(key, JSON.stringify(value));
}

/**
 * 从 localStorage 移除指定键
 * @param key 存储键名
 */
export function removeStorage(key: string): void {
  localStorage.removeItem(key);
}
