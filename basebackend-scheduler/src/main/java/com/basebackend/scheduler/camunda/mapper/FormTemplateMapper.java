package com.basebackend.scheduler.camunda.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.scheduler.camunda.entity.FormTemplateEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 工作流表单模板Mapper
 */
@Mapper
public interface FormTemplateMapper extends BaseMapper<FormTemplateEntity> {

    /**
     * 根据表单Key查询模板
     *
     * @param formKey 表单Key
     * @return 表单模板
     */
    FormTemplateEntity selectByFormKey(@Param("formKey") String formKey);

    /**
     * 根据流程定义Key查询模板列表
     *
     * @param processDefinitionKey 流程定义Key
     * @return 表单模板列表
     */
    List<FormTemplateEntity> selectByProcessDefinitionKey(@Param("processDefinitionKey") String processDefinitionKey);

    /**
     * 查询所有启用的模板
     *
     * @return 表单模板列表
     */
    List<FormTemplateEntity> selectEnabledTemplates();
}
