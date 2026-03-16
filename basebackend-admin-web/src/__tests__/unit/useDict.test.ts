import { describe, it, expect, beforeEach, vi } from 'vitest';
import { renderHook, waitFor, act } from '@testing-library/react';

/**
 * useDict Hook 单元测试
 * 验证字典数据获取、缓存机制和加载状态
 */

// mock dictApi
vi.mock('@/api/dictApi', () => ({
  dictApi: {
    dataByType: vi.fn(),
  },
}));

// 动态导入以便在 mock 之后使用
import { dictApi } from '@/api/dictApi';
import { useDict, clearDictCache } from '@/hooks/useDict';

const mockDataByType = vi.mocked(dictApi.dataByType);

/** 构造测试用字典数据 */
function makeDictData(dictType: string, count = 2) {
  return Array.from({ length: count }, (_, i) => ({
    id: i + 1,
    appId: 1,
    dictSort: i,
    dictLabel: `${dictType}_label_${i}`,
    dictValue: String(i),
    dictType,
    cssClass: '',
    listClass: '',
    isDefault: i === 0 ? 1 : 0,
    status: 1,
    remark: '',
  }));
}

describe('useDict', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    // 每个测试前清除缓存
    clearDictCache();
  });

  it('首次调用时发起 API 请求并返回数据', async () => {
    const mockData = makeDictData('sys_status');
    mockDataByType.mockResolvedValueOnce(mockData);

    const { result } = renderHook(() => useDict('sys_status'));

    // 初始状态：loading 为 true
    expect(result.current.loading).toBe(true);

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.data).toEqual(mockData);
    expect(mockDataByType).toHaveBeenCalledWith('sys_status');
    expect(mockDataByType).toHaveBeenCalledTimes(1);
  });

  it('缓存命中时不再发起 API 请求', async () => {
    const mockData = makeDictData('sys_gender');
    mockDataByType.mockResolvedValueOnce(mockData);

    // 第一次调用：触发请求
    const { result: result1 } = renderHook(() => useDict('sys_gender'));
    await waitFor(() => {
      expect(result1.current.loading).toBe(false);
    });
    expect(mockDataByType).toHaveBeenCalledTimes(1);

    // 第二次调用：应命中缓存
    const { result: result2 } = renderHook(() => useDict('sys_gender'));
    // 缓存命中时 loading 直接为 false
    expect(result2.current.loading).toBe(false);
    expect(result2.current.data).toEqual(mockData);
    // 不应再次调用 API
    expect(mockDataByType).toHaveBeenCalledTimes(1);
  });

  it('不同 dictType 分别请求和缓存', async () => {
    const statusData = makeDictData('sys_status');
    const genderData = makeDictData('sys_gender');
    mockDataByType.mockResolvedValueOnce(statusData);
    mockDataByType.mockResolvedValueOnce(genderData);

    const { result: r1 } = renderHook(() => useDict('sys_status'));
    await waitFor(() => expect(r1.current.loading).toBe(false));

    const { result: r2 } = renderHook(() => useDict('sys_gender'));
    await waitFor(() => expect(r2.current.loading).toBe(false));

    expect(r1.current.data).toEqual(statusData);
    expect(r2.current.data).toEqual(genderData);
    expect(mockDataByType).toHaveBeenCalledTimes(2);
  });

  it('dictType 为空字符串时不发起请求', async () => {
    const { result } = renderHook(() => useDict(''));

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.data).toEqual([]);
    expect(mockDataByType).not.toHaveBeenCalled();
  });

  it('API 请求失败时返回空数组', async () => {
    mockDataByType.mockRejectedValueOnce(new Error('网络错误'));

    const { result } = renderHook(() => useDict('sys_fail'));

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.data).toEqual([]);
  });

  it('clearDictCache 清除指定类型后重新请求', async () => {
    const mockData = makeDictData('sys_type');
    mockDataByType.mockResolvedValue(mockData);

    // 第一次请求
    const { result: r1 } = renderHook(() => useDict('sys_type'));
    await waitFor(() => expect(r1.current.loading).toBe(false));
    expect(mockDataByType).toHaveBeenCalledTimes(1);

    // 清除缓存
    act(() => {
      clearDictCache('sys_type');
    });

    // 再次请求应重新调用 API
    const { result: r2 } = renderHook(() => useDict('sys_type'));
    await waitFor(() => expect(r2.current.loading).toBe(false));
    expect(mockDataByType).toHaveBeenCalledTimes(2);
  });

  it('clearDictCache 不传参数时清除全部缓存', async () => {
    const data1 = makeDictData('type_a');
    const data2 = makeDictData('type_b');
    mockDataByType.mockResolvedValueOnce(data1);
    mockDataByType.mockResolvedValueOnce(data2);

    const { result: r1 } = renderHook(() => useDict('type_a'));
    await waitFor(() => expect(r1.current.loading).toBe(false));

    const { result: r2 } = renderHook(() => useDict('type_b'));
    await waitFor(() => expect(r2.current.loading).toBe(false));

    expect(mockDataByType).toHaveBeenCalledTimes(2);

    // 清除全部缓存
    act(() => {
      clearDictCache();
    });

    // 两个类型都应重新请求
    mockDataByType.mockResolvedValueOnce(data1);
    mockDataByType.mockResolvedValueOnce(data2);

    const { result: r3 } = renderHook(() => useDict('type_a'));
    await waitFor(() => expect(r3.current.loading).toBe(false));

    const { result: r4 } = renderHook(() => useDict('type_b'));
    await waitFor(() => expect(r4.current.loading).toBe(false));

    expect(mockDataByType).toHaveBeenCalledTimes(4);
  });
});
