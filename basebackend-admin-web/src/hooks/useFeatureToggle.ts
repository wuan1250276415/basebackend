import { useState, useEffect, useCallback } from 'react';
import { featureToggleApi } from '@/api/featureToggleApi';
import type { FeatureContext, VariantResponse } from '@/types/featureToggle';

/**
 * 检查单个特性是否启用
 * @param featureName 特性名称
 * @param context 特性上下文（用户信息）
 * @param defaultValue 默认值
 */
export function useFeatureToggle(
  featureName: string,
  context?: FeatureContext,
  defaultValue = false
) {
  const [enabled, setEnabled] = useState<boolean>(defaultValue);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<Error | null>(null);

  const checkFeature = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await featureToggleApi.checkFeature(featureName, context);
      setEnabled(response.enabled);
    } catch (err) {
      console.error(`Failed to check feature '${featureName}':`, err);
      setError(err as Error);
      setEnabled(defaultValue);
    } finally {
      setLoading(false);
    }
  }, [featureName, context?.userId, context?.username, context?.email, defaultValue]);

  useEffect(() => {
    checkFeature();
  }, [checkFeature]);

  return { enabled, loading, error, refresh: checkFeature };
}

/**
 * 批量检查多个特性
 * @param featureNames 特性名称列表
 * @param context 特性上下文
 */
export function useFeatureToggles(
  featureNames: string[],
  context?: FeatureContext
) {
  const [features, setFeatures] = useState<Record<string, boolean>>({});
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<Error | null>(null);

  const checkFeatures = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const result = await featureToggleApi.checkFeaturesBatch({
        featureNames,
        userId: context?.userId,
        username: context?.username,
        email: context?.email,
      });
      setFeatures(result);
    } catch (err) {
      console.error('Failed to check features:', err);
      setError(err as Error);
    } finally {
      setLoading(false);
    }
  }, [featureNames.join(','), context?.userId, context?.username, context?.email]);

  useEffect(() => {
    if (featureNames.length > 0) {
      checkFeatures();
    }
  }, [checkFeatures]);

  const isEnabled = useCallback(
    (featureName: string) => features[featureName] ?? false,
    [features]
  );

  return { features, isEnabled, loading, error, refresh: checkFeatures };
}

/**
 * 获取所有特性开关状态
 * @param context 特性上下文
 */
export function useAllFeatures(context?: FeatureContext) {
  const [features, setFeatures] = useState<Record<string, boolean>>({});
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<Error | null>(null);

  const fetchFeatures = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const result = await featureToggleApi.getAllFeatures(context);
      setFeatures(result);
    } catch (err) {
      console.error('Failed to fetch all features:', err);
      setError(err as Error);
    } finally {
      setLoading(false);
    }
  }, [context?.userId, context?.username, context?.email]);

  useEffect(() => {
    fetchFeatures();
  }, [fetchFeatures]);

  const isEnabled = useCallback(
    (featureName: string) => features[featureName] ?? false,
    [features]
  );

  return { features, isEnabled, loading, error, refresh: fetchFeatures };
}

/**
 * 获取变体信息（用于AB测试）
 * @param featureName 特性名称
 * @param context 特性上下文
 */
export function useVariant(featureName: string, context?: FeatureContext) {
  const [variant, setVariant] = useState<VariantResponse | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<Error | null>(null);

  const fetchVariant = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const result = await featureToggleApi.getVariant(featureName, context);
      setVariant(result);
    } catch (err) {
      console.error(`Failed to get variant for '${featureName}':`, err);
      setError(err as Error);
    } finally {
      setLoading(false);
    }
  }, [featureName, context?.userId, context?.username, context?.email]);

  useEffect(() => {
    fetchVariant();
  }, [fetchVariant]);

  return { variant, loading, error, refresh: fetchVariant };
}

/**
 * 获取特性开关服务状态
 */
export function useFeatureToggleStatus() {
  const [status, setStatus] = useState<{
    available: boolean;
    provider: string;
    message: string;
  } | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<Error | null>(null);

  const fetchStatus = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const result = await featureToggleApi.getStatus();
      setStatus(result);
    } catch (err) {
      console.error('Failed to fetch feature toggle status:', err);
      setError(err as Error);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchStatus();
  }, [fetchStatus]);

  return { status, loading, error, refresh: fetchStatus };
}
