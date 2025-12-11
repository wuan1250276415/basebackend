package com.basebackend.scheduler.camunda.service;

import com.basebackend.common.dto.PageResult;
import com.basebackend.scheduler.camunda.dto.FormTemplateCreateRequest;
import com.basebackend.scheduler.camunda.dto.FormTemplateDTO;
import com.basebackend.scheduler.camunda.dto.FormTemplatePageQuery;
import com.basebackend.scheduler.camunda.dto.FormTemplateUpdateRequest;

/**
 * 表单模板业务逻辑接口
 *
 * <p>提供表单模板相关的业务逻辑封装，包括：
 * <ul>
 *   <li>表单模板查询（分页、详情）</li>
 *   <li>表单模板创建</li>
 *   <li>表单模板更新</li>
 *   <li>表单模板删除</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
public interface FormTemplateService {

    /**
     * 分页查询表单模板
     *
     * @param query 分页查询参数
     * @return 分页结果
     */
    PageResult<FormTemplateDTO> page(FormTemplatePageQuery query);

    /**
     * 获取表单模板详情
     *
     * @param id 模板 ID
     * @return 表单模板详情
     */
    FormTemplateDTO detail(Long id);

    /**
     * 创建表单模板
     *
     * @param request 创建请求参数
     * @return 创建结果
     */
    FormTemplateDTO create(FormTemplateCreateRequest request);

    /**
     * 更新表单模板
     *
     * @param id 模板 ID
     * @param request 更新请求参数
     * @return 更新结果
     */
    FormTemplateDTO update(Long id, FormTemplateUpdateRequest request);

    /**
     * 删除表单模板
     *
     * @param id 模板 ID
     */
    void delete(Long id);
}
