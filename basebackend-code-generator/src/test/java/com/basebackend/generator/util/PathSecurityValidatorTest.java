package com.basebackend.generator.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 路径安全验证器测试类
 */
@DisplayName("PathSecurityValidator 测试")
class PathSecurityValidatorTest {

    @Nested
    @DisplayName("路径遍历攻击防护测试")
    class PathTraversalTests {

        @ParameterizedTest
        @DisplayName("检测父目录遍历攻击")
        @ValueSource(strings = {
                "../etc/passwd",
                "..\\windows\\system32",
                "com/example/../../../etc/passwd",
                "foo/bar/../../.."
        })
        void shouldRejectParentDirectoryTraversal(String path) {
            PathSecurityValidator.PathValidationResult result = PathSecurityValidator.validatePath(path);
            assertFalse(result.isValid(), "应拒绝路径遍历攻击: " + path);
        }

        @ParameterizedTest
        @DisplayName("检测系统目录访问")
        @ValueSource(strings = {
                "/etc/passwd",
                "/var/log/syslog",
                "/tmp/malicious",
                "\\windows\\system32\\config"
        })
        void shouldRejectSystemDirectoryAccess(String path) {
            PathSecurityValidator.PathValidationResult result = PathSecurityValidator.validatePath(path);
            assertFalse(result.isValid(), "应拒绝系统目录访问: " + path);
        }

        @ParameterizedTest
        @DisplayName("检测空字符注入")
        @ValueSource(strings = {
                "file.java\u0000.txt",
                "path%00exploit"
        })
        void shouldRejectNullByteInjection(String path) {
            PathSecurityValidator.PathValidationResult result = PathSecurityValidator.validatePath(path);
            assertFalse(result.isValid(), "应拒绝空字符注入: " + path);
        }
    }

    @Nested
    @DisplayName("有效路径测试")
    class ValidPathTests {

        @ParameterizedTest
        @DisplayName("接受有效的Java项目路径")
        @ValueSource(strings = {
                "com/example/entity/User.java",
                "src/main/java/Controller.java",
                "mapper/UserMapper.xml",
                "service/impl/UserServiceImpl.java"
        })
        void shouldAcceptValidPaths(String path) {
            PathSecurityValidator.PathValidationResult result = PathSecurityValidator.validatePath(path);
            assertTrue(result.isValid(), "应接受有效路径: " + path);
            assertNotNull(result.getNormalizedPath());
        }

        @Test
        @DisplayName("返回规范化后的路径")
        void shouldReturnNormalizedPath() {
            String path = "com/example/entity/User.java";
            PathSecurityValidator.PathValidationResult result = PathSecurityValidator.validatePath(path);
            assertTrue(result.isValid());
            assertEquals("com\\example\\entity\\User.java", result.getNormalizedPath().replace("/", "\\"));
        }
    }

    @Nested
    @DisplayName("未替换模板变量测试")
    class UnreplacedTemplateVariableTests {

        @ParameterizedTest
        @DisplayName("检测未替换的模板变量")
        @ValueSource(strings = {
                "${packagePath}/entity/User.java",
                "com/example/${className}.java",
                "${variableName}/${templateCode}/Test.java"
        })
        void shouldRejectUnreplacedTemplateVariables(String path) {
            PathSecurityValidator.PathValidationResult result = PathSecurityValidator.validatePath(path);
            assertFalse(result.isValid(), "应拒绝未替换的模板变量: " + path);
        }
    }

    @Nested
    @DisplayName("边界条件测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("拒绝空路径")
        void shouldRejectNullPath() {
            PathSecurityValidator.PathValidationResult result = PathSecurityValidator.validatePath(null);
            assertFalse(result.isValid());
            assertEquals("路径不能为空", result.getErrorMessage());
        }

        @Test
        @DisplayName("拒绝空白字符串")
        void shouldRejectEmptyPath() {
            PathSecurityValidator.PathValidationResult result = PathSecurityValidator.validatePath("   ");
            assertFalse(result.isValid());
        }

        @Test
        @DisplayName("isSecurePath快速验证")
        void testIsSecurePathMethod() {
            assertTrue(PathSecurityValidator.isSecurePath("com/example/User.java"));
            assertFalse(PathSecurityValidator.isSecurePath("../etc/passwd"));
        }

        @Test
        @DisplayName("requireSecurePath抛出异常")
        void shouldThrowExceptionForInsecurePath() {
            assertThrows(SecurityException.class, () -> PathSecurityValidator.requireSecurePath("../malicious"));
        }

        @Test
        @DisplayName("requireSecurePath返回规范化路径")
        void shouldReturnNormalizedPathFromRequireMethod() {
            String result = PathSecurityValidator.requireSecurePath("com/example/User.java");
            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("文件扩展名白名单测试")
    class FileExtensionTests {

        @ParameterizedTest
        @DisplayName("接受白名单中的扩展名")
        @ValueSource(strings = {
                "User.java",
                "mapper.xml",
                "config.yml",
                "config.yaml",
                "app.properties",
                "index.html",
                "style.css",
                "app.js",
                "app.ts",
                "App.tsx",
                "App.jsx",
                "App.vue",
                "data.json",
                "README.md",
                "init.sql",
                "template.ftl",
                "template.vm"
        })
        void shouldAcceptWhitelistedExtensions(String path) {
            PathSecurityValidator.PathValidationResult result = PathSecurityValidator.validatePath(path);
            assertTrue(result.isValid(), "应接受白名单扩展名: " + path);
        }

        @ParameterizedTest
        @DisplayName("拒绝非白名单扩展名")
        @ValueSource(strings = {
                "script.exe",
                "script.sh",
                "script.bat",
                "data.dll"
        })
        void shouldRejectNonWhitelistedExtensions(String path) {
            PathSecurityValidator.PathValidationResult result = PathSecurityValidator.validatePath(path);
            assertFalse(result.isValid(), "应拒绝非白名单扩展名: " + path);
        }
    }
}
