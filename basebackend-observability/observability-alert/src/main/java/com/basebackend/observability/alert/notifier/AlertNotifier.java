package com.basebackend.observability.alert.notifier;

import com.basebackend.observability.alert.AlertEvent;

/**
 * 告警通知器接口
 * 定义告警通知的标准方法
 */
public interface AlertNotifier {

    /**
     * 发送告警通知
     *
     * @param event 告警事件
     * @return 是否发送成功
     */
    boolean sendAlert(AlertEvent event);

    /**
     * 获取通知器类型
     *
     * @return 通知器类型（email, dingtalk, wechat等）
     */
    String getNotifierType();

    /**
     * 检查通知器是否可用
     *
     * @return 是否可用
     */
    boolean isAvailable();
}
