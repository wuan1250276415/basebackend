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
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GenerateRequest DTO验证测试
 */
@DisplayName("GenerateRequest 验证测试")
class GenerateRequestTest {

    private static Validator validator;

    // 默认有效参数常量
    private static final Long VALID_DATASOURCE_ID = 1L;
    private static final List<String> VALID_TABLE_NAMES = Arrays.asList("sys_user", "sys_role");
    private static final Long VALID_TEMPLATE_GROUP_ID = 1L;
    private static final String VALID_GENERATE_TYPE = "PREVIEW";
    private static final String VALID_PACKAGE_NAME = "com.example.demo";
    private static final String VALID_MODULE_NAME = "user";
    private static final String VALID_AUTHOR = "Test";

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    /**
     * 创建有效的 GenerateRequest 实例
     */
    private GenerateRequest createValidRequest() {
        return new GenerateRequest(
                VALID_DATASOURCE_ID, VALID_TABLE_NAMES, VALID_TEMPLATE_GROUP_ID,
                VALID_GENERATE_TYPE, VALID_PACKAGE_NAME, VALID_MODULE_NAME,
                VALID_AUTHOR, null, null);
    }

    /**
     * 使用自定义字段创建 GenerateRequest（record 不可变，需通过构造器）
     */
    private GenerateRequest withDatasourceId(Long datasourceId) {
        return new GenerateRequest(datasourceId, VALID_TABLE_NAMES, VALID_TEMPLATE_GROUP_ID,
                VALID_GENERATE_TYPE, VALID_PACKAGE_NAME, VALID_MODULE_NAME,
                VALID_AUTHOR, null, null);
    }

    private GenerateRequest withTableNames(List<String> tableNames) {
        return new GenerateRequest(VALID_DATASOURCE_ID, tableNames, VALID_TEMPLATE_GROUP_ID,
                VALID_GENERATE_TYPE, VALID_PACKAGE_NAME, VALID_MODULE_NAME,
                VALID_AUTHOR, null, null);
    }

    private GenerateRequest withGenerateType(String generateType) {
        return new GenerateRequest(VALID_DATASOURCE_ID, VALID_TABLE_NAMES, VALID_TEMPLATE_GROUP_ID,
                generateType, VALID_PACKAGE_NAME, VALID_MODULE_NAME,
                VALID_AUTHOR, null, null);
    }

    private GenerateRequest withPackageName(String packageName) {
        return new GenerateRequest(VALID_DATASOURCE_ID, VALID_TABLE_NAMES, VALID_TEMPLATE_GROUP_ID,
                VALID_GENERATE_TYPE, packageName, VALID_MODULE_NAME,
                VALID_AUTHOR, null, null);
    }

    private GenerateRequest withModuleName(String moduleName) {
        return new GenerateRequest(VALID_DATASOURCE_ID, VALID_TABLE_NAMES, VALID_TEMPLATE_GROUP_ID,
                VALID_GENERATE_TYPE, VALID_PACKAGE_NAME, moduleName,
                VALID_AUTHOR, null, null);
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
            GenerateRequest request = withDatasourceId(null);

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
            GenerateRequest request = withTableNames(Collections.emptyList());

            Set<ConstraintViolation<GenerateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("表名列表不能为null")
        void tableNamesShouldNotBeNull() {
            GenerateRequest request = withTableNames(null);

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
            GenerateRequest request = withGenerateType(type);

            Set<ConstraintViolation<GenerateRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty());
        }

        @ParameterizedTest
        @DisplayName("拒绝无效的生成类型")
        @ValueSource(strings = { "INVALID", "download", "preview", "OTHER" })
        void shouldRejectInvalidGenerateTypes(String type) {
            GenerateRequest request = withGenerateType(type);

            Set<ConstraintViolation<GenerateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }

        @ParameterizedTest
        @DisplayName("生成类型不能为空")
        @NullAndEmptySource
        void generateTypeShouldNotBeNullOrEmpty(String type) {
            GenerateRequest request = withGenerateType(type);

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
            GenerateRequest request = withPackageName(packageName);

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
            GenerateRequest request = withPackageName(packageName);

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
            GenerateRequest request = withModuleName(moduleName);

            Set<ConstraintViolation<GenerateRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty(), "应接受有效模块名: " + moduleName);
        }

        @ParameterizedTest
        @DisplayName("拒绝无效的模块名")
        @ValueSource(strings = { "123module", "-invalid", "_invalid" })
        void shouldRejectInvalidModuleNames(String moduleName) {
            GenerateRequest request = withModuleName(moduleName);

            Set<ConstraintViolation<GenerateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty(), "应拒绝无效模块名: " + moduleName);
        }
    }
}
