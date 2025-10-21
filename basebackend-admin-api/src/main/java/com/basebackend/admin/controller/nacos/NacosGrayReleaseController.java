package com.basebackend.admin.controller.nacos;

import com.basebackend.admin.dto.nacos.GrayReleaseDTO;
import com.basebackend.admin.service.nacos.NacosGrayReleaseManagementService;
import com.basebackend.common.model.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Nacos灰度发布Controller
 */
@RestController
@RequestMapping("/api/nacos/gray-release")
@RequiredArgsConstructor
public class NacosGrayReleaseController {

    private final NacosGrayReleaseManagementService grayReleaseManagementService;

    /**
     * 创建灰度发布
     */
    @PostMapping
    public Result<Long> create(@Valid @RequestBody GrayReleaseDTO grayDTO) {
        return Result.success(grayReleaseManagementService.createGrayRelease(grayDTO));
    }

    /**
     * 灰度全量发布
     */
    @PostMapping("/promote/{grayId}")
    public Result<Void> promote(@PathVariable Long grayId) {
        grayReleaseManagementService.promoteGrayRelease(grayId);
        return Result.success();
    }

    /**
     * 回滚灰度发布
     */
    @PostMapping("/rollback/{grayId}")
    public Result<Void> rollback(@PathVariable Long grayId) {
        grayReleaseManagementService.rollbackGrayRelease(grayId);
        return Result.success();
    }
}
