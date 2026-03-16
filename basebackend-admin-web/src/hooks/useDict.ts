import { useState, useEffect } from 'react';
import { dictApi } from '@/api/dictApi';
import type { DictDataDTO } from '@/types';

/** 模块级缓存，按 dictType 存储已请求的字典数据 */
const dictCache = new Map<string, DictDataDTO[]>();

/** 正在进行中的请求，避免同一 dictType 并发重复请求 */
const pendingRequests = new Map<string, Promise<DictDataDTO[]>>();

/**
 * 字典数据 Hook
 *
 * 根据字典类型标识获取字典数据列表，内置模块级缓存，
 * 同一 dictType 仅首次调用会发起 API 请求，后续直接返回缓存。
 *
 * @param dictType 字典类型标识
 * @returns { data: DictDataDTO[], loading: boolean }
 *
 * @example
 * const { data, loading } = useDict('sys_user_status');
 * // data: [{ dictLabel: '正常', dictValue: '1', ... }, ...]
 */
export function useDict(dictType: string) {
  const [data, setData] = useState<DictDataDTO[]>(() => dictCache.get(dictType) ?? []);
  const [loading, setLoading] = useState<boolean>(!dictCache.has(dictType));

  useEffect(() => {
    // dictType 为空时不请求
    if (!dictType) {
      setData([]);
      setLoading(false);
      return;
    }

    // 缓存命中，直接返回
    if (dictCache.has(dictType)) {
      setData(dictCache.get(dictType)!);
      setLoading(false);
      return;
    }

    let cancelled = false;

    const fetchData = async () => {
      setLoading(true);
      try {
        // 复用正在进行中的请求，避免并发重复调用
        let promise = pendingRequests.get(dictType);
        if (!promise) {
          promise = dictApi.dataByType(dictType);
          pendingRequests.set(dictType, promise);
        }

        const result = await promise;
        dictCache.set(dictType, result);
        pendingRequests.delete(dictType);

        if (!cancelled) {
          setData(result);
        }
      } catch (error) {
        console.error(`获取字典数据失败 [${dictType}]:`, error);
        pendingRequests.delete(dictType);
        if (!cancelled) {
          setData([]);
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    fetchData();

    return () => {
      cancelled = true;
    };
  }, [dictType]);

  return { data, loading };
}

/**
 * 清除指定字典类型的缓存，或清除全部缓存
 * 适用于字典数据变更后需要重新加载的场景
 *
 * @param dictType 可选，指定要清除的字典类型；不传则清除全部
 */
export function clearDictCache(dictType?: string) {
  if (dictType) {
    dictCache.delete(dictType);
  } else {
    dictCache.clear();
  }
}
