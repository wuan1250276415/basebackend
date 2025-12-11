package com.basebackend.generator.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GenerateRequest DTO验证测试
 */
@DisplayName("GenerateRequest 验证测试")
class GenerateRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private GenerateRequest createValidRequest() {
        GenerateRequest request = new GenerateRequest();
        request.setDatasourceId(1L);
        request.setTableNames(Arrays.asList("sys_user", "sys_role"));
        request.setTemplateGroupId(1L);
        request.setGenerateType("PREVIEW");
        request.setPackageName("com.example.demo");
        request.setModuleName("user");
        request.setAuthor("Test");
        return request;
    }

    @Nested
    @DisplayName("有效请求测试")
    class ValidRequestTests {

        @Test
        @DisplayName("有效请求无验证错误")
        void validRequestShouldPassValidation() {
            GenerateRequest request = createValidRequest();
            Set<ConstraintViolation<GenerateRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty(), "有效请求不应有验证错误");
        }
    }

    @Nested
    @DisplayName("数据源ID验证")
    class DatasourceIdValidation {

        @Test
        @DisplayName("数据源ID不能为空")
        void datasourceIdShouldNotBeNull() {
            GenerateRequest request = createValidRequest();
            request.setDatasourceId(null);

            Set<ConstraintViolation<GenerateRequest>> violations = validator.validate(request);
            assertEquals(1, violations.size());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("数据源ID")));
        }
    }

    @Nested
    @DisplayName("表名列表验证")
    class TableNamesValidation {

        @Test
        @DisplayName("表名列表不能为空")
        void tableNamesShouldNotBeEmpty() {
            GenerateRequest request = createValidRequest();
            request.setTableNames(Collections.emptyList());

            Set<ConstraintViolation<GenerateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("表名列表不能为null")
        void tableNamesShouldNotBeNull() {
            GenerateRequest request = createValidRequest();
            request.setTableNames(null);

            Set<ConstraintViolation<GenerateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }
    }

    @Nested
    @DisplayName("生成类型验证")
    class GenerateTypeValidation {

        @ParameterizedTest
        @DisplayName("接受有效的生成类型")
        @ValueSource(strings = { "DOWNLOAD", "PREVIEW", "INCREMENT" })
        void shouldAcceptValidGenerateTypes(String type) {
            GenerateRequest request = createValidRequest();
            request.setGenerateType(type);

            Set<ConstraintViolation<GenerateRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty());
        }

        @ParameterizedTest
        @DisplayName("拒绝无效的生成类型")
        @ValueSource(strings = { "INVALID", "download", "preview", "OTHER" })
        void shouldRejectInvalidGenerateTypes(String type) {
            GenerateRequest request = createValidRequest();
            request.setGenerateType(type);

            Set<ConstraintViolation<GenerateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }

        @ParameterizedTest
        @DisplayName("生成类型不能为空")
        @NullAndEmptySource
        void generateTypeShouldNotBeNullOrEmpty(String type) {
            GenerateRequest request = createValidRequest();
            request.setGenerateType(type);

            Set<ConstraintViolation<GenerateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }
    }

    @Nested
    @DisplayName("包名验证")
    class PackageNameValidation {

        @ParameterizedTest
        @DisplayName("接受有效的包名")
        @ValueSource(strings = {
                "com.example",
                "com.example.demo",
                "org.springframework.boot",
                "io.github.example"
        })
        void shouldAcceptValidPackageNames(String packageName) {
            GenerateRequest request = createValidRequest();
            request.setPackageName(packageName);

            Set<ConstraintViolation<GenerateRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty(), "应接受有效包名: " + packageName);
        }

        @ParameterizedTest
        @DisplayName("拒绝无效的包名")
        @ValueSource(strings = {
                "Com.Example", // 大写开头
                "123.example", // 数字开头
                "com..example", // 双点
                "com.Example.Demo" // 包含大写
        })
        void shouldRejectInvalidPackageNames(String packageName) {
            GenerateRequest request = createValidRequest();
            request.setPackageName(packageName);

            Set<ConstraintViolation<GenerateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty(), "应拒绝无效包名: " + packageName);
        }
    }

    @Nested
    @DisplayName("模块名验证")
    class ModuleNameValidation {

        @ParameterizedTest
        @DisplayName("接受有效的模块名")
        @ValueSource(strings = { "user", "User", "userService", "user-service", "user_service" })
        void shouldAcceptValidModuleNames(String moduleName) {
            GenerateRequest request = createValidRequest();
            request.setModuleName(moduleName);

            Set<ConstraintViolation<GenerateRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty(), "应接受有效模块名: " + moduleName);
        }

        @ParameterizedTest
        @DisplayName("拒绝无效的模块名")
        @ValueSource(strings = { "123module", "-invalid", "_invalid" })
        void shouldRejectInvalidModuleNames(String moduleName) {
            GenerateRequest request = createValidRequest();
            request.setModuleName(moduleName);

            Set<ConstraintViolation<GenerateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty(), "应拒绝无效模块名: " + moduleName);
        }
    }
}
