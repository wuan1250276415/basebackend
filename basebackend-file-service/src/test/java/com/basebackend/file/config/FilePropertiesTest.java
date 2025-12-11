package com.basebackend.file.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FileProperties测试类
 * 测试文件配置属性的加载和绑定
 *
 * @author BaseBackend
 */
@DisplayName("FileProperties 文件配置属性测试")
class FilePropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(FilePropertiesTest.TestConfiguration.class));

    @Test
    @DisplayName("FileProperties应该被自动配置")
    void shouldAutoConfigureFileProperties() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(FileProperties.class);
            FileProperties properties = context.getBean(FileProperties.class);
            assertThat(properties).isNotNull();
        });
    }

    @Test
    @DisplayName("文件上传路径配置")
    void shouldConfigureUploadPath() {
        contextRunner.withPropertyValues("file.uploadPath=/custom/upload/path")
            .run(context -> {
                FileProperties properties = context.getBean(FileProperties.class);
                assertThat(properties.getUploadPath()).isEqualTo("/custom/upload/path");
            });
    }

    @Test
    @DisplayName("文件访问前缀配置")
    void shouldConfigureAccessPrefix() {
        contextRunner.withPropertyValues("file.accessPrefix=/api/files")
            .run(context -> {
                FileProperties properties = context.getBean(FileProperties.class);
                assertThat(properties.getAccessPrefix()).isEqualTo("/api/files");
            });
    }

    @Test
    @DisplayName("允许的文件类型配置")
    void shouldConfigureAllowedTypes() {
        contextRunner.withPropertyValues("file.allowedTypes[0]=txt", "file.allowedTypes[1]=pdf")
            .run(context -> {
                FileProperties properties = context.getBean(FileProperties.class);
                assertThat(properties.getAllowedTypes()).contains("txt", "pdf");
            });
    }

    @Test
    @DisplayName("最大文件大小配置")
    void shouldConfigureMaxSize() {
        contextRunner.withPropertyValues("file.maxSize=20971520") // 20MB
            .run(context -> {
                FileProperties properties = context.getBean(FileProperties.class);
                assertThat(properties.getMaxSize()).isEqualTo(20971520L);
            });
    }

    @Test
    @DisplayName("默认配置值")
    void shouldHaveDefaultValues() {
        contextRunner.run(context -> {
            FileProperties properties = context.getBean(FileProperties.class);
            assertThat(properties.getUploadPath()).isEqualTo("./uploads");
            assertThat(properties.getAccessPrefix()).isEqualTo("/files");
            assertThat(properties.getAllowedTypes()).isNotEmpty();
            assertThat(properties.getMaxSize()).isEqualTo(10 * 1024 * 1024); // 10MB
        });
    }

    @Test
    @DisplayName("属性类型转换")
    void shouldConvertPropertyTypes() {
        contextRunner.withPropertyValues(
                "file.uploadPath=/test",
                "file.accessPrefix=/api",
                "file.maxSize=52428800"
            )
            .run(context -> {
                FileProperties properties = context.getBean(FileProperties.class);
                assertThat(properties).isInstanceOf(FileProperties.class);
                assertThat(properties.getUploadPath()).isInstanceOf(String.class);
                assertThat(properties.getMaxSize()).isInstanceOf(Long.class);
            });
    }

    @Configuration
    static class TestConfiguration {
        @Bean
        public FileProperties fileProperties() {
            return new FileProperties();
        }
    }
}
