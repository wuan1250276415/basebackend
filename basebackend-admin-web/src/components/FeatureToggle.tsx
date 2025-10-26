import React, { ReactNode } from 'react';
import { Spin } from 'antd';
import { useFeatureToggle } from '@/hooks/useFeatureToggle';
import type { FeatureContext } from '@/types/featureToggle';

interface FeatureToggleProps {
  /** 特性名称 */
  featureName: string;
  /** 特性上下文 */
  context?: FeatureContext;
  /** 特性启用时渲染的内容 */
  children: ReactNode;
  /** 特性未启用时渲染的内容（可选） */
  fallback?: ReactNode;
  /** 是否显示加载状态 */
  showLoading?: boolean;
  /** 默认值 */
  defaultValue?: boolean;
}

/**
 * 特性开关组件
 * 根据特性状态条件渲染内容
 *
 * @example
 * <FeatureToggle featureName="new-feature">
 *   <NewFeatureComponent />
 * </FeatureToggle>
 *
 * @example
 * <FeatureToggle
 *   featureName="premium-feature"
 *   context={{ userId: currentUser.id }}
 *   fallback={<div>该功能仅对VIP用户开放</div>}
 * >
 *   <PremiumFeatureComponent />
 * </FeatureToggle>
 */
export const FeatureToggle: React.FC<FeatureToggleProps> = ({
  featureName,
  context,
  children,
  fallback = null,
  showLoading = false,
  defaultValue = false,
}) => {
  const { enabled, loading } = useFeatureToggle(featureName, context, defaultValue);

  if (loading && showLoading) {
    return <Spin size="small" />;
  }

  return <>{enabled ? children : fallback}</>;
};

export default FeatureToggle;
