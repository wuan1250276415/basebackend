package com.basebackend.scheduler.camunda.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.basebackend.scheduler.camunda.entity.NodeConfigEntity;
import com.basebackend.scheduler.camunda.mapper.NodeConfigMapper;
import com.basebackend.scheduler.camunda.service.NodeConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 节点配置服务实现
 *
 * @author BaseBackend Team
 * @since 2025-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NodeConfigServiceImpl extends ServiceImpl<NodeConfigMapper, NodeConfigEntity>
        implements NodeConfigService {

    @Override
    public NodeConfigEntity getByNodeKey(Long templateId, String nodeKey) {
        return this.getOne(new LambdaQueryWrapper<NodeConfigEntity>()
                .eq(NodeConfigEntity::getTemplateId, templateId)
                .eq(NodeConfigEntity::getNodeKey, nodeKey));
    }
}
