package com.basebackend.admin.controller.nacos;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.basebackend.admin.dto.nacos.ConfigHistoryQueryDTO;
import com.basebackend.admin.entity.nacos.SysNacosConfigHistory;
import com.basebackend.admin.service.nacos.NacosConfigHistoryService;
import com.basebackend.common.model.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Nacos配置历史Controller
 */
@RestController
@RequestMapping("/api/nacos/config-history")
@RequiredArgsConstructor
@Validated
public class NacosConfigHistoryController {

    private final NacosConfigHistoryService configHistoryService;

    /**
     * 分页查询配置历史
     */
    @PostMapping("/page")
    public Result<IPage<SysNacosConfigHistory>> queryPage(@RequestBody ConfigHistoryQueryDTO queryDTO) {
        return Result.success(configHistoryService.queryHistoryPage(queryDTO));
    }

    /**
     * 获取配置的所有历史版本
     */
    @GetMapping("/list/{configId}")
    public Result<List<SysNacosConfigHistory>> getConfigHistory(@PathVariable Long configId) {
        return Result.success(configHistoryService.getConfigHistory(configId));
    }

    /**
     * 获取历史详情
     */
    @GetMapping("/{historyId}")
    public Result<SysNacosConfigHistory> getDetail(@PathVariable Long historyId) {
        return Result.success(configHistoryService.getHistoryDetail(historyId));
    }
}
