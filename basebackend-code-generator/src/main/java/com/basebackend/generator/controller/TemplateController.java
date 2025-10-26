package com.basebackend.generator.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.common.model.Result;
import com.basebackend.generator.entity.GenTemplate;
import com.basebackend.generator.entity.GenTemplateGroup;
import com.basebackend.generator.mapper.GenTemplateGroupMapper;
import com.basebackend.generator.mapper.GenTemplateMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模板管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/generator/template")
@RequiredArgsConstructor
@Tag(name = "模板管理")
public class TemplateController {

    private final GenTemplateMapper templateMapper;
    private final GenTemplateGroupMapper templateGroupMapper;

    @GetMapping("/group")
    @Operation(summary = "查询模板分组列表")
    public Result<List<GenTemplateGroup>> listGroups() {
        return Result.success(templateGroupMapper.selectList(null));
    }

    @GetMapping("/group/{groupId}/templates")
    @Operation(summary = "查询分组下的模板列表")
    public Result<List<GenTemplate>> listTemplatesByGroup(@PathVariable Long groupId) {
        List<GenTemplate> templates = templateMapper.selectList(
                new LambdaQueryWrapper<GenTemplate>()
                        .eq(GenTemplate::getGroupId, groupId)
                        .orderByAsc(GenTemplate::getSortOrder)
        );
        return Result.success(templates);
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询模板")
    public Result<GenTemplate> getById(@PathVariable Long id) {
        return Result.success(templateMapper.selectById(id));
    }

    @PostMapping
    @Operation(summary = "创建模板")
    public Result<String> create(@RequestBody GenTemplate template) {
        templateMapper.insert(template);
        return Result.success("创建成功");
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新模板")
    public Result<String> update(@PathVariable Long id, @RequestBody GenTemplate template) {
        template.setId(id);
        templateMapper.updateById(template);
        return Result.success("更新成功");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除模板")
    public Result<String> delete(@PathVariable Long id) {
        templateMapper.deleteById(id);
        return Result.success("删除成功");
    }
}
