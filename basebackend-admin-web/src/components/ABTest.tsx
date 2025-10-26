import React, { ReactNode, useMemo } from 'react';
import { Spin } from 'antd';
import { useVariant } from '@/hooks/useFeatureToggle';
import type { FeatureContext } from '@/types/featureToggle';

interface ABTestProps {
  /** 实验/特性名称 */
  featureName: string;
  /** 特性上下文 */
  context?: FeatureContext;
  /** 变体渲染映射 */
  variants: Record<string, ReactNode>;
  /** 默认变体（当没有匹配时） */
  defaultVariant?: ReactNode;
  /** 是否显示加载状态 */
  showLoading?: boolean;
}

/**
 * AB测试组件
 * 根据变体信息渲染不同的内容
 *
 * @example
 * <ABTest
 *   featureName="checkout-experiment"
 *   context={{ userId: currentUser.id }}
 *   variants={{
 *     'control': <OldCheckoutFlow />,
 *     'variant-a': <NewCheckoutFlowA />,
 *     'variant-b': <NewCheckoutFlowB />,
 *   }}
 *   defaultVariant={<OldCheckoutFlow />}
 * />
 */
export const ABTest: React.FC<ABTestProps> = ({
  featureName,
  context,
  variants,
  defaultVariant = null,
  showLoading = false,
}) => {
  const { variant, loading } = useVariant(featureName, context);

  const content = useMemo(() => {
    if (!variant || !variant.enabled) {
      return defaultVariant;
    }

    const variantContent = variants[variant.variantName];
    return variantContent ?? defaultVariant;
  }, [variant, variants, defaultVariant]);

  if (loading && showLoading) {
    return <Spin size="small" />;
  }

  return <>{content}</>;
};

export default ABTest;
