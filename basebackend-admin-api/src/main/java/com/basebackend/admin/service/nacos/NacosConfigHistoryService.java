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
        Page<SysNacosConfigHistory> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());

        LambdaQueryWrapper<SysNacosConfigHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(queryDTO.getConfigId() != null, SysNacosConfigHistory::getConfigId, queryDTO.getConfigId())
                .like(queryDTO.getDataId() != null, SysNacosConfigHistory::getDataId, queryDTO.getDataId())
                .eq(queryDTO.getOperationType() != null, SysNacosConfigHistory::getOperationType, queryDTO.getOperationType())
                .eq(queryDTO.getOperator() != null, SysNacosConfigHistory::getOperator, queryDTO.getOperator())
                .ge(queryDTO.getStartTime() != null, SysNacosConfigHistory::getCreateTime, queryDTO.getStartTime())
                .le(queryDTO.getEndTime() != null, SysNacosConfigHistory::getCreateTime, queryDTO.getEndTime())
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
