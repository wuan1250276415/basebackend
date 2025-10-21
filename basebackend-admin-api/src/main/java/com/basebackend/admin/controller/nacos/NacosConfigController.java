package com.basebackend.admin.controller.nacos;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.basebackend.admin.dto.nacos.*;
import com.basebackend.admin.entity.nacos.SysNacosConfig;
import com.basebackend.admin.service.nacos.NacosConfigManagementService;
import com.basebackend.common.model.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Nacos配置管理Controller
 */
@RestController
@RequestMapping("/api/nacos/config")
@RequiredArgsConstructor
public class NacosConfigController {

    private final NacosConfigManagementService configManagementService;

    /**
     * 分页查询配置
     */
    @PostMapping("/page")
    public Result<IPage<SysNacosConfig>> queryPage(@RequestBody NacosConfigQueryDTO queryDTO) {
        return Result.success(configManagementService.queryConfigPage(queryDTO));
    }

    /**
     * 获取配置详情
     */
    @GetMapping("/{id}")
    public Result<SysNacosConfig> getDetail(@PathVariable Long id) {
        return Result.success(configManagementService.getConfigDetail(id));
    }

    /**
     * 创建配置
     */
    @PostMapping
    public Result<Long> create(@Valid @RequestBody NacosConfigDTO configDTO) {
        return Result.success(configManagementService.createConfig(configDTO));
    }

    /**
     * 更新配置
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody NacosConfigDTO configDTO) {
        configManagementService.updateConfig(id, configDTO);
        return Result.success();
    }

    /**
     * 删除配置
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        configManagementService.deleteConfig(id);
        return Result.success();
    }

    /**
     * 发布配置
     */
    @PostMapping("/publish")
    public Result<Void> publish(@Valid @RequestBody ConfigPublishDTO publishDTO) {
        configManagementService.publishConfig(publishDTO);
        return Result.success();
    }

    /**
     * 回滚配置
     */
    @PostMapping("/rollback")
    public Result<Void> rollback(@Valid @RequestBody ConfigRollbackDTO rollbackDTO) {
        configManagementService.rollbackConfig(rollbackDTO);
        return Result.success();
    }
}
