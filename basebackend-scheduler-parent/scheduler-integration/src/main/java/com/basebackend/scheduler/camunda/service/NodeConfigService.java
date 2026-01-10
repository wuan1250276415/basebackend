package com.basebackend.scheduler.camunda.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.basebackend.scheduler.camunda.entity.NodeConfigEntity;

/**
 * 节点配置服务接口
 *
 * @author BaseBackend Team
 * @since 2025-01-01
 */
public interface NodeConfigService extends IService<NodeConfigEntity> {

    /**
     * 根据模板 ID 和节点 Key 获取配置
     *
     * @param templateId 模板 ID
     * @param nodeKey    节点 Key
     * @return 节点配置
     */
    NodeConfigEntity getByNodeKey(Long templateId, String nodeKey);
}
