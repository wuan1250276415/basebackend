package com.basebackend.generator.controller;

import com.basebackend.common.model.Result;
import com.basebackend.generator.dto.GenerateRequest;
import com.basebackend.generator.dto.GenerateResult;
import com.basebackend.generator.entity.GenerateType;
import com.basebackend.generator.service.GeneratorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 代码生成控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/generator")
@RequiredArgsConstructor
@Tag(name = "代码生成器")
public class GeneratorController {

    private final GeneratorService generatorService;

    @PostMapping("/generate")
    @Operation(summary = "生成代码")
    public ResponseEntity<?> generate(@RequestBody GenerateRequest request) {
        log.info("开始生成代码: {}", request.getTableNames());

        GenerateResult result = generatorService.generate(request);

        // 下载模式返回ZIP文件
        if (GenerateType.DOWNLOAD.name().equals(request.getGenerateType())) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=generated-code.zip")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(result.getZipData());
        }

        // 预览模式返回JSON
        return ResponseEntity.ok(Result.success("生成成功", result));
    }

    @PostMapping("/preview")
    @Operation(summary = "预览代码")
    public Result<GenerateResult> preview(@RequestBody GenerateRequest request) {
        request.setGenerateType(GenerateType.PREVIEW.name());
        GenerateResult result = generatorService.generate(request);
        return Result.success("预览成功", result);
    }
}
