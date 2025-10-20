package com.basebackend.admin.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * OpenAPI 文档及 SDK 导出控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/openapi")
@Tag(name = "OpenAPI 文档", description = "提供 OpenAPI 规范导出与 SDK 生成能力")
public class OpenApiController {

    private static final MediaType MEDIA_TYPE_YAML = MediaType.valueOf("application/yaml");
    private static final MediaType MEDIA_TYPE_ZIP = MediaType.valueOf("application/zip");

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${documentation.openapi.base-url:}")
    private String openApiBaseUrl;

    @Value("${server.port:0}")
    private int serverPort;

    public OpenApiController(RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = objectMapper;
    }

    @GetMapping(value = "/spec.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "获取 OpenAPI 规范(JSON)", description = "返回当前服务的 OpenAPI 规范，JSON 格式")
    public ResponseEntity<JsonNode> getOpenApiJson(HttpServletRequest request) {
        try {
            String specJson = fetchOpenApiJson(request);
            JsonNode jsonNode = objectMapper.readTree(specJson);
            if (!jsonNode.hasNonNull("openapi")) {
                throw new IllegalStateException("OpenAPI spec missing version field");
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonNode);
        } catch (Exception exception) {
            log.error("获取 OpenAPI JSON 规范失败", exception);
            JsonNode errorNode = objectMapper.createObjectNode()
                    .put("error", "Unable to load OpenAPI specification");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorNode);
        }
    }

    @GetMapping(value = "/spec.yaml", produces = "application/yaml")
    @Operation(summary = "获取 OpenAPI 规范(YAML)", description = "返回当前服务的 OpenAPI 规范，YAML 格式")
    public ResponseEntity<String> getOpenApiYaml(HttpServletRequest request) {
        try {
            String specJson = fetchOpenApiJson(request);
            JsonNode jsonNode = objectMapper.readTree(specJson);
            String yaml = Yaml.pretty().writeValueAsString(jsonNode);
            return ResponseEntity.ok()
                    .contentType(MEDIA_TYPE_YAML)
                    .body(yaml);
        } catch (Exception exception) {
            log.error("获取 OpenAPI YAML 规范失败", exception);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("message: Unable to load OpenAPI specification\n");
        }
    }

    @GetMapping("/sdk/typescript")
    @Operation(summary = "生成 TypeScript SDK", description = "实时生成 TypeScript Fetch SDK 并以 Zip 包形式返回")
    public ResponseEntity<byte[]> downloadTypeScriptSdk(HttpServletRequest request) {
        Path workingDir = null;
        Path outputDir = null;
        try {
            String specJson = fetchOpenApiJson(request);
            workingDir = Files.createTempDirectory("openapi-spec-");
            outputDir = Files.createTempDirectory("openapi-sdk-ts-");

            Path specFile = workingDir.resolve("openapi.json");
            Files.writeString(specFile, specJson, StandardCharsets.UTF_8);

            CodegenConfigurator configurator = new CodegenConfigurator();
            configurator.setGeneratorName("typescript-fetch");
            configurator.setInputSpec(specFile.toAbsolutePath().toString());
            configurator.setOutputDir(outputDir.toAbsolutePath().toString());
            configurator.addAdditionalProperty("supportsES6", true);
            configurator.addAdditionalProperty("npmName", "basebackend-admin-sdk");
            configurator.addAdditionalProperty("npmVersion", "1.0.0");
            configurator.addAdditionalProperty("useSingleRequestParameter", true);
            configurator.addAdditionalProperty("withSeparateModelsAndApi", true);
            configurator.addAdditionalProperty("stringEnums", true);

            ClientOptInput clientOptInput = configurator.toClientOptInput();
            DefaultGenerator generator = new DefaultGenerator();
            generator.opts(clientOptInput).generate();

            byte[] archiveBytes = createZipArchive(outputDir);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"basebackend-admin-sdk.zip\"")
                    .contentType(MEDIA_TYPE_ZIP)
                    .contentLength(archiveBytes.length)
                    .body(archiveBytes);
        } catch (Exception exception) {
            log.error("生成 TypeScript SDK 失败", exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (Objects.nonNull(workingDir)) {
                FileSystemUtils.deleteRecursively(workingDir.toFile());
            }
            if (Objects.nonNull(outputDir)) {
                FileSystemUtils.deleteRecursively(outputDir.toFile());
            }
        }
    }

    private String fetchOpenApiJson(HttpServletRequest request) throws IOException {
        String apiDocsUrl = resolveApiDocsUrl(request);
        ResponseEntity<String> response = restTemplate.getForEntity(apiDocsUrl, String.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IOException("Failed to load OpenAPI spec from " + apiDocsUrl + ", status: " + response.getStatusCode());
        }
        return response.getBody();
    }

    private String resolveApiDocsUrl(HttpServletRequest request) {
        if (StringUtils.hasText(openApiBaseUrl)) {
            return UriComponentsBuilder.fromUriString(openApiBaseUrl)
                    .path("/v3/api-docs")
                    .build()
                    .toUriString();
        }

        if (serverPort > 0) {
            return UriComponentsBuilder.newInstance()
                    .scheme("http")
                    .host("127.0.0.1")
                    .port(serverPort)
                    .path("/v3/api-docs")
                    .build()
                    .toUriString();
        }

        ServletUriComponentsBuilder builder = ServletUriComponentsBuilder.fromRequest(request);
        return builder.replacePath(null)
                .replaceQuery(null)
                .path("/v3/api-docs")
                .build()
                .toUriString();
    }

    private byte[] createZipArchive(Path sourceDirectory) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zipOutputStream = new ZipOutputStream(baos)) {

            Files.walkFileTree(sourceDirectory, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (!sourceDirectory.equals(dir)) {
                        String entryName = sourceDirectory.relativize(dir).toString().replace("\\", "/") + "/";
                        zipOutputStream.putNextEntry(new ZipEntry(entryName));
                        zipOutputStream.closeEntry();
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String entryName = sourceDirectory.relativize(file).toString().replace("\\", "/");
                    zipOutputStream.putNextEntry(new ZipEntry(entryName));
                    Files.copy(file, zipOutputStream);
                    zipOutputStream.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
            });

            zipOutputStream.finish();
            return baos.toByteArray();
        }
    }
}
