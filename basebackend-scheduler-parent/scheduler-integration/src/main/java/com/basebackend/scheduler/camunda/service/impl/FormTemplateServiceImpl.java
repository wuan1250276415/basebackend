package com.basebackend.scheduler.camunda.service.impl;

import com.basebackend.common.dto.PageResult;
import com.basebackend.scheduler.camunda.config.PaginationConstants;
import com.basebackend.scheduler.camunda.dto.FormTemplateCreateRequest;
import com.basebackend.scheduler.camunda.dto.FormTemplateDTO;
import com.basebackend.scheduler.camunda.dto.FormTemplatePageQuery;
import com.basebackend.scheduler.camunda.dto.FormTemplateUpdateRequest;
import com.basebackend.scheduler.camunda.entity.FormTemplateEntity;
import com.basebackend.scheduler.camunda.exception.CamundaServiceException;
import com.basebackend.scheduler.camunda.mapper.FormTemplateMapper;
import com.basebackend.scheduler.camunda.service.FormTemplateService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 表单模板服务实现类
 *
 * <p>
 * 提供表单模板的CRUD操作，支持版本管理和缓存。
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FormTemplateServiceImpl implements FormTemplateService {

    private final FormTemplateMapper formTemplateMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResult<FormTemplateDTO> page(FormTemplatePageQuery query) {
        try {
            // 验证分页参数
            int pageNum = Math.max(1, query.getPageNum());
            int pageSize = Math.min(Math.max(1, query.getPageSize()), PaginationConstants.MAX_PAGE_SIZE);

            log.info("Querying form templates, page={}, size={}", pageNum, pageSize);

            // 构建查询条件
            LambdaQueryWrapper<FormTemplateEntity> wrapper = new LambdaQueryWrapper<>();

            if (StringUtils.hasText(query.getFormCode())) {
                wrapper.like(FormTemplateEntity::getFormCode, query.getFormCode());
            }
            if (StringUtils.hasText(query.getFormName())) {
                wrapper.like(FormTemplateEntity::getFormName, query.getFormName());
            }
            if (StringUtils.hasText(query.getProcessDefinitionKey())) {
                wrapper.eq(FormTemplateEntity::getProcessDefinitionKey, query.getProcessDefinitionKey());
            }
            if (query.getStatus() != null) {
                wrapper.eq(FormTemplateEntity::getStatus, query.getStatus());
            }

            // 分页查询
            Page<FormTemplateEntity> page = new Page<>(pageNum, pageSize);
            IPage<FormTemplateEntity> result = formTemplateMapper.selectPage(page, wrapper);

            // 转换为DTO
            List<FormTemplateDTO> dtoList = result.getRecords().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            return PageResult.of(dtoList, result.getTotal(), (long) pageNum, (long) pageSize);
        } catch (Exception ex) {
            log.error("Failed to query form templates", ex);
            throw new CamundaServiceException("查询表单模板失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "formTemplates", key = "'detail_' + #id", cacheManager = "workflowCacheManager")
    public FormTemplateDTO detail(Long id) {
        try {
            log.info("Getting form template detail, id={}", id);

            FormTemplateEntity entity = formTemplateMapper.selectById(id);

            if (entity == null) {
                throw new CamundaServiceException("表单模板不存在: " + id);
            }

            return convertToDTO(entity);
        } catch (CamundaServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to get form template detail, id={}", id, ex);
            throw new CamundaServiceException("获取表单模板详情失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "formTemplates", allEntries = true, cacheManager = "workflowCacheManager")
    public FormTemplateDTO create(FormTemplateCreateRequest request) {
        try {
            log.info("Creating form template, formCode={}", request.getFormCode());

            // 检查表单编码是否已存在
            LambdaQueryWrapper<FormTemplateEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(FormTemplateEntity::getFormCode, request.getFormCode());
            Long count = formTemplateMapper.selectCount(wrapper);

            if (count > 0) {
                throw new CamundaServiceException("表单编码已存在: " + request.getFormCode());
            }

            // 创建表单模板实体
            FormTemplateEntity entity = new FormTemplateEntity();
            entity.setFormCode(request.getFormCode());
            entity.setFormName(request.getFormName());
            entity.setFormType(request.getFormType());
            entity.setSchema(request.getFormSchema());
            entity.setContent(request.getContent());
            entity.setProcessDefinitionKey(request.getProcessDefinitionKey());
            entity.setVersion(1L);
            entity.setStatus(String.valueOf(1)); // 启用
            entity.setDescription(request.getDescription());
            entity.setCreatedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());

            // 保存到数据库
            formTemplateMapper.insert(entity);

            log.info("Form template created successfully, id={}", entity.getId());

            return convertToDTO(entity);
        } catch (CamundaServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to create form template, formCode={}", request.getFormCode(), ex);
            throw new CamundaServiceException("创建表单模板失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "formTemplates", allEntries = true, cacheManager = "workflowCacheManager")
    public FormTemplateDTO update(Long id, FormTemplateUpdateRequest request) {
        try {
            log.info("Updating form template, id={}", id);

            // 查询现有模板
            FormTemplateEntity entity = formTemplateMapper.selectById(id);
            if (entity == null) {
                throw new CamundaServiceException("表单模板不存在: " + id);
            }

            // 更新字段
            if (StringUtils.hasText(request.getFormName())) {
                entity.setFormName(request.getFormName());
            }
            if (StringUtils.hasText(request.getFormSchema())) {
                entity.setSchema(request.getFormSchema());
                // 更新Schema时增加版本号
                entity.setVersion(entity.getVersion() + 1);
            }
            if (StringUtils.hasText(request.getContent())) {
                entity.setContent(request.getContent());
            }
            if (StringUtils.hasText(request.getDescription())) {
                entity.setDescription(request.getDescription());
            }
            if (request.getStatus() != null) {
                entity.setStatus(request.getStatus());
            }

            entity.setUpdatedAt(LocalDateTime.now());

            // 更新到数据库
            formTemplateMapper.updateById(entity);

            log.info("Form template updated successfully, id={}, version={}", id, entity.getVersion());

            return convertToDTO(entity);
        } catch (CamundaServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to update form template, id={}", id, ex);
            throw new CamundaServiceException("更新表单模板失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "formTemplates", allEntries = true, cacheManager = "workflowCacheManager")
    public void delete(Long id) {
        try {
            log.info("Deleting form template, id={}", id);

            // 检查模板是否存在
            FormTemplateEntity entity = formTemplateMapper.selectById(id);
            if (entity == null) {
                throw new CamundaServiceException("表单模板不存在: " + id);
            }

            // 软删除（设置状态为删除）
            entity.setStatus(String.valueOf(0)); // 禁用
            entity.setUpdatedAt(LocalDateTime.now());
            formTemplateMapper.updateById(entity);

            // 如果需要硬删除，可以使用：
            // formTemplateMapper.deleteById(id);

            log.info("Form template deleted successfully, id={}", id);
        } catch (CamundaServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to delete form template, id={}", id, ex);
            throw new CamundaServiceException("删除表单模板失败: " + ex.getMessage(), ex);
        }
    }

    // ========== 私有辅助方法 ==========

    private FormTemplateDTO convertToDTO(FormTemplateEntity entity) {
        FormTemplateDTO dto = new FormTemplateDTO();
        dto.setId(entity.getId());
        dto.setFormCode(entity.getFormCode());
        dto.setFormName(entity.getFormName());
        dto.setFormType(entity.getFormType());
        dto.setSchema(entity.getFormSchema());
        dto.setContent(entity.getContent());
        dto.setProcessDefinitionKey(entity.getProcessDefinitionKey());
        dto.setVersion(entity.getVersion());
        dto.setStatus(entity.getStatus());
        dto.setDescription(entity.getDescription());
        dto.setCreatedAt(entity.getCreateTime());
        dto.setUpdatedAt(entity.getUpdateTime());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setTenantId(entity.getTenantId());
        return dto;
    }
}
