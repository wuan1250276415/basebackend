package com.basebackend.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.basebackend.application.dto.ApplicationDTO;
import com.basebackend.application.entity.SysApplication;
import com.basebackend.application.mapper.SysApplicationMapper;
import com.basebackend.application.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 应用管理Service实现
 *
 * @author BaseBackend Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final SysApplicationMapper applicationMapper;

    @Override
    public List<ApplicationDTO> listApplications() {
        log.info("查询应用列表");
        LambdaQueryWrapper<SysApplication> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysApplication::getDeleted, 0)
                .orderByAsc(SysApplication::getOrderNum)
                .orderByAsc(SysApplication::getCreateTime);

        List<SysApplication> applications = applicationMapper.selectList(queryWrapper);
        return applications.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ApplicationDTO> listEnabledApplications() {
        log.info("查询所有启用的应用");
        List<SysApplication> applications = applicationMapper.selectEnabledApplications();
        return applications.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ApplicationDTO getApplicationById(Long id) {
        log.info("根据ID查询应用: {}", id);
        SysApplication application = applicationMapper.selectById(id);
        if (application != null && application.getDeleted() == 0) {
            return convertToDTO(application);
        }
        return null;
    }

    @Override
    public ApplicationDTO getApplicationByCode(String appCode) {
        log.info("根据应用编码查询应用: {}", appCode);
        SysApplication application = applicationMapper.selectByAppCode(appCode);
        if (application != null) {
            return convertToDTO(application);
        }
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createApplication(ApplicationDTO dto) {
        log.info("创建应用: {}", dto.getAppName());

        // 检查应用编码是否已存在
        SysApplication existing = applicationMapper.selectByAppCode(dto.getAppCode());
        if (existing != null) {
            throw new RuntimeException("应用编码已存在：" + dto.getAppCode());
        }

        SysApplication application = new SysApplication();
        BeanUtils.copyProperties(dto, application);

        return applicationMapper.insert(application) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateApplication(ApplicationDTO dto) {
        log.info("更新应用: {}", dto.getId());

        if (dto.getId() == null) {
            throw new RuntimeException("应用ID不能为空");
        }

        // 检查应用编码是否被其他应用使用
        SysApplication existing = applicationMapper.selectByAppCode(dto.getAppCode());
        if (existing != null && !existing.getId().equals(dto.getId())) {
            throw new RuntimeException("应用编码已被其他应用使用：" + dto.getAppCode());
        }

        SysApplication application = new SysApplication();
        BeanUtils.copyProperties(dto, application);

        return applicationMapper.updateById(application) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteApplication(Long id) {
        log.info("删除应用: {}", id);

        SysApplication application = applicationMapper.selectById(id);
        if (application == null) {
            throw new RuntimeException("应用不存在");
        }

        // 软删除
        application.setDeleted(1);
        return applicationMapper.updateById(application) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(Long id, Integer status) {
        log.info("修改应用状态: id={}, status={}", id, status);

        SysApplication application = applicationMapper.selectById(id);
        if (application == null) {
            throw new RuntimeException("应用不存在");
        }

        application.setStatus(status);
        return applicationMapper.updateById(application) > 0;
    }

    /**
     * 转换为DTO
     *
     * @param entity 实体
     * @return DTO
     */
    private ApplicationDTO convertToDTO(SysApplication entity) {
        ApplicationDTO dto = new ApplicationDTO();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }
}
