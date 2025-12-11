package com.basebackend.generator.controller;

import com.basebackend.common.model.Result;
import com.basebackend.generator.constant.GeneratorConstants;
import com.basebackend.generator.dto.GenerateRequest;
import com.basebackend.generator.dto.GenerateResult;
import com.basebackend.generator.service.GeneratorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 代码生成控制器
 * 
 * 优化内容：
 * 1. 添加@Valid注解进行参数验证
 * 2. 使用常量替代魔法值
 */
@Slf4j
@RestController
@RequestMapping("/api/generator")
@RequiredArgsConstructor
@Tag(name = "代码生成器")
public class GeneratorController {

    private final GeneratorService generatorService;

    /**
     * 生成代码
     *
     * @param request 生成请求（已添加参数验证）
     * @return 下载模式返回ZIP文件，预览模式返回JSON
     */
    @PostMapping("/generate")
    @Operation(summary = "生成代码")
    public ResponseEntity<?> generate(@Valid @RequestBody GenerateRequest request) {
        log.info("开始生成代码: {}", request.getTableNames());

        GenerateResult result = generatorService.generate(request);

        // 下载模式返回ZIP文件
        if (GeneratorConstants.GENERATE_TYPE_DOWNLOAD.equals(request.getGenerateType())) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=generated-code.zip")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(result.getZipData());
        }

        // 预览模式返回JSON
        return ResponseEntity.ok(Result.success("生成成功", result));
    }

    /**
     * 预览代码
     *
     * @param request 生成请求（已添加参数验证）
     * @return 生成的代码预览
     */
    @PostMapping("/preview")
    @Operation(summary = "预览代码")
    public Result<GenerateResult> preview(@Valid @RequestBody GenerateRequest request) {
        request.setGenerateType(GeneratorConstants.GENERATE_TYPE_PREVIEW);
        GenerateResult result = generatorService.generate(request);
        return Result.success("预览成功", result);
    }
}
