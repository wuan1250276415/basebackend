package com.basebackend.admin.service.nacos;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.admin.dto.nacos.ConfigHistoryQueryDTO;
import com.basebackend.admin.entity.nacos.SysNacosConfigHistory;
import com.basebackend.admin.mapper.nacos.SysNacosConfigHistoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Nacos配置历史Service
 */
@Service
@RequiredArgsConstructor
public class NacosConfigHistoryService {

    private final SysNacosConfigHistoryMapper configHistoryMapper;

    /**
     * 分页查询配置历史
     */
    public IPage<SysNacosConfigHistory> queryHistoryPage(ConfigHistoryQueryDTO queryDTO) {
        Page<SysNacosConfigHistory> page = new Page<>(queryDTO.pageNum(), queryDTO.pageSize());

        LambdaQueryWrapper<SysNacosConfigHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(queryDTO.configId() != null, SysNacosConfigHistory::getConfigId, queryDTO.configId())
                .like(queryDTO.dataId() != null, SysNacosConfigHistory::getDataId, queryDTO.dataId())
                .eq(queryDTO.operationType() != null, SysNacosConfigHistory::getOperationType, queryDTO.operationType())
                .eq(queryDTO.operator() != null, SysNacosConfigHistory::getOperator, queryDTO.operator())
                .ge(queryDTO.startTime() != null, SysNacosConfigHistory::getCreateTime, queryDTO.startTime())
                .le(queryDTO.endTime() != null, SysNacosConfigHistory::getCreateTime, queryDTO.endTime())
                .orderByDesc(SysNacosConfigHistory::getCreateTime);

        return configHistoryMapper.selectPage(page, wrapper);
    }

    /**
     * 获取配置的所有历史版本
     */
    public List<SysNacosConfigHistory> getConfigHistory(Long configId) {
        LambdaQueryWrapper<SysNacosConfigHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNacosConfigHistory::getConfigId, configId)
                .orderByDesc(SysNacosConfigHistory::getVersion);

        return configHistoryMapper.selectList(wrapper);
    }

    /**
     * 获取历史详情
     */
    public SysNacosConfigHistory getHistoryDetail(Long historyId) {
        return configHistoryMapper.selectById(historyId);
    }
}
