package com.basebackend.scheduler.camunda.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.scheduler.camunda.dto.FormTemplateDTO;
import com.basebackend.scheduler.camunda.entity.FormTemplateEntity;
import com.basebackend.scheduler.camunda.mapper.FormTemplateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 工作流表单模板服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FormTemplateService {

    private final FormTemplateMapper formTemplateMapper;

    /**
     * 查询表单模板列表（分页）
     */
    public Page<FormTemplateDTO> listFormTemplates(Integer page, Integer size, String name, String processDefinitionKey, Integer status) {
        // 构建查询条件
        LambdaQueryWrapper<FormTemplateEntity> queryWrapper = new LambdaQueryWrapper<>();

        if (name != null && !name.isEmpty()) {
            queryWrapper.like(FormTemplateEntity::getName, name);
        }
        if (processDefinitionKey != null && !processDefinitionKey.isEmpty()) {
            queryWrapper.eq(FormTemplateEntity::getProcessDefinitionKey, processDefinitionKey);
        }
        if (status != null) {
            queryWrapper.eq(FormTemplateEntity::getStatus, status);
        }

        queryWrapper.orderByDesc(FormTemplateEntity::getCreateTime);

        // 分页查询
        int pageNum = (page != null && page > 0) ? page : 1;
        int pageSize = (size != null && size > 0) ? size : 10;
        Page<FormTemplateEntity> entityPage = new Page<>(pageNum, pageSize);
        Page<FormTemplateEntity> resultPage = formTemplateMapper.selectPage(entityPage, queryWrapper);

        // 转换为DTO
        Page<FormTemplateDTO> dtoPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        List<FormTemplateDTO> dtoList = resultPage.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        dtoPage.setRecords(dtoList);

        return dtoPage;
    }

    /**
     * 根据ID查询表单模板
     */
    public FormTemplateDTO getFormTemplateById(Long id) {
        FormTemplateEntity entity = formTemplateMapper.selectById(id);
        if (entity == null) {
            throw new RuntimeException("表单模板不存在: id=" + id);
        }
        return convertToDTO(entity);
    }

    /**
     * 根据表单Key查询表单模板
     */
    public FormTemplateDTO getFormTemplateByFormKey(String formKey) {
        FormTemplateEntity entity = formTemplateMapper.selectByFormKey(formKey);
        if (entity == null) {
            throw new RuntimeException("表单模板不存在: formKey=" + formKey);
        }
        return convertToDTO(entity);
    }

    /**
     * 根据流程定义Key查询表单模板列表
     */
    public List<FormTemplateDTO> getFormTemplatesByProcessDefinitionKey(String processDefinitionKey) {
        List<FormTemplateEntity> entities = formTemplateMapper.selectByProcessDefinitionKey(processDefinitionKey);
        return entities.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 查询所有启用的表单模板
     */
    public List<FormTemplateDTO> getEnabledFormTemplates() {
        List<FormTemplateEntity> entities = formTemplateMapper.selectEnabledTemplates();
        return entities.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 创建表单模板
     */
    @Transactional(rollbackFor = Exception.class)
    public FormTemplateDTO createFormTemplate(FormTemplateDTO dto) {
        // 检查formKey是否已存在
        FormTemplateEntity existingEntity = formTemplateMapper.selectByFormKey(dto.getFormKey());
        if (existingEntity != null) {
            throw new RuntimeException("表单Key已存在: " + dto.getFormKey());
        }

        // 转换并保存
        FormTemplateEntity entity = convertToEntity(dto);
        if (entity.getStatus() == null) {
            entity.setStatus(1); // 默认启用
        }
        if (entity.getVersion() == null) {
            entity.setVersion(1); // 默认版本1
        }

        formTemplateMapper.insert(entity);
        log.info("创建表单模板成功: formKey={}, id={}", entity.getFormKey(), entity.getId());

        return convertToDTO(entity);
    }

    /**
     * 更新表单模板
     */
    @Transactional(rollbackFor = Exception.class)
    public FormTemplateDTO updateFormTemplate(Long id, FormTemplateDTO dto) {
        // 检查模板是否存在
        FormTemplateEntity existingEntity = formTemplateMapper.selectById(id);
        if (existingEntity == null) {
            throw new RuntimeException("表单模板不存在: id=" + id);
        }

        // 如果修改了formKey，检查新的formKey是否已被使用
        if (dto.getFormKey() != null && !dto.getFormKey().equals(existingEntity.getFormKey())) {
            FormTemplateEntity duplicateEntity = formTemplateMapper.selectByFormKey(dto.getFormKey());
            if (duplicateEntity != null) {
                throw new RuntimeException("表单Key已存在: " + dto.getFormKey());
            }
        }

        // 更新字段
        if (dto.getName() != null) {
            existingEntity.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            existingEntity.setDescription(dto.getDescription());
        }
        if (dto.getFormKey() != null) {
            existingEntity.setFormKey(dto.getFormKey());
        }
        if (dto.getProcessDefinitionKey() != null) {
            existingEntity.setProcessDefinitionKey(dto.getProcessDefinitionKey());
        }
        if (dto.getSchemaJson() != null) {
            existingEntity.setSchemaJson(dto.getSchemaJson());
            // 更新版本号
            existingEntity.setVersion(existingEntity.getVersion() + 1);
        }
        if (dto.getStatus() != null) {
            existingEntity.setStatus(dto.getStatus());
        }

        formTemplateMapper.updateById(existingEntity);
        log.info("更新表单模板成功: id={}, formKey={}", id, existingEntity.getFormKey());

        return convertToDTO(existingEntity);
    }

    /**
     * 删除表单模板（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteFormTemplate(Long id) {
        FormTemplateEntity entity = formTemplateMapper.selectById(id);
        if (entity == null) {
            throw new RuntimeException("表单模板不存在: id=" + id);
        }

        formTemplateMapper.deleteById(id);
        log.info("删除表单模板成功: id={}, formKey={}", id, entity.getFormKey());
    }

    /**
     * 启用/禁用表单模板
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateFormTemplateStatus(Long id, Integer status) {
        FormTemplateEntity entity = formTemplateMapper.selectById(id);
        if (entity == null) {
            throw new RuntimeException("表单模板不存在: id=" + id);
        }

        entity.setStatus(status);
        formTemplateMapper.updateById(entity);
        log.info("更新表单模板状态成功: id={}, status={}", id, status);
    }

    /**
     * 转换为DTO
     */
    private FormTemplateDTO convertToDTO(FormTemplateEntity entity) {
        FormTemplateDTO dto = new FormTemplateDTO();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    /**
     * 转换为Entity
     */
    private FormTemplateEntity convertToEntity(FormTemplateDTO dto) {
        FormTemplateEntity entity = new FormTemplateEntity();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}
