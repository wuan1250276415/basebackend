package com.basebackend.observability.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.client.RestClient;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 保护镜像启动所依赖的关键配置与运行时类路径，避免 fat jar 启动时再次在早期阶段失败。
 */
class ObservabilityServiceStartupSanityTest {

    @Test
    @DisplayName("RestClient 配置不应因同名 Bean 冲突阻塞启动")
    void restClientConfigurationsShouldNotConflictOnBeanName() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.setAllowBeanDefinitionOverriding(false);

        try (GenericApplicationContext context = new GenericApplicationContext(beanFactory)) {
            AnnotatedBeanDefinitionReader reader = new AnnotatedBeanDefinitionReader(context);
            context.registerBean(ObservabilityProperties.class, ObservabilityProperties::new);
            context.registerBean(RestClient.Builder.class, () -> RestClient.builder());
            reader.register(ObservabilityConfig.class, EnhancedRestTemplateConfig.class);
            context.refresh();

            assertThat(context.getBean(RestClient.class))
                    .isSameAs(context.getBean("restClient", RestClient.class));
        }
    }

    @Test
    @DisplayName("TracingFilter 依赖的 Servlet API 应存在于运行时类路径")
    void tracingFilterShouldLoadWithServletApiOnRuntimeClasspath() {
        assertThatCode(() -> Class.forName("jakarta.servlet.Filter"))
                .doesNotThrowAnyException();

        assertThatCode(() -> Class.forName("com.basebackend.observability.tracing.TracingFilter"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("application.yml 应将自动配置排除项放在 spring.autoconfigure 下")
    @SuppressWarnings("unchecked")
    void applicationYamlShouldKeepSpringAutoconfigureAtTopLevel() throws IOException {
        Map<String, Object> yaml = loadApplicationYaml();

        assertThat(yaml).containsKey("spring");

        Map<String, Object> spring = (Map<String, Object>) yaml.get("spring");
        assertThat(spring).containsKey("autoconfigure");

        Map<String, Object> nacos = (Map<String, Object>) yaml.get("nacos");
        assertThat(nacos).doesNotContainKeys("main", "autoconfigure");

        Map<String, Object> autoconfigure = (Map<String, Object>) spring.get("autoconfigure");
        List<String> excludes = (List<String>) autoconfigure.get("exclude");
        assertThat(excludes).contains("com.basebackend.observability.otel.config.OtelAutoConfiguration");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadApplicationYaml() throws IOException {
        Yaml yaml = new Yaml();
        try (InputStream inputStream = new ClassPathResource("application.yml").getInputStream()) {
            return yaml.load(inputStream);
        }
    }
}
