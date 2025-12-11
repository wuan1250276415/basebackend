package com.basebackend.generator.cache;

import com.basebackend.generator.core.metadata.TableMetadata;
import com.basebackend.generator.entity.GenTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 代码生成器缓存管理器测试
 */
@DisplayName("GeneratorCacheManager 测试")
class GeneratorCacheManagerTest {

    private GeneratorCacheManager cacheManager;

    @BeforeEach
    void setUp() {
        cacheManager = new GeneratorCacheManager();
    }

    @Nested
    @DisplayName("模板缓存测试")
    class TemplateCacheTests {

        @Test
        @DisplayName("缓存模板列表")
        void shouldCacheTemplates() {
            Long groupId = 1L;
            List<GenTemplate> templates = createTestTemplates();

            cacheManager.putTemplates(groupId, templates);
            List<GenTemplate> cached = cacheManager.getTemplates(groupId);

            assertNotNull(cached);
            assertEquals(templates.size(), cached.size());
        }

        @Test
        @DisplayName("获取不存在的模板缓存返回null")
        void shouldReturnNullForNonExistentTemplateCache() {
            assertNull(cacheManager.getTemplates(999L));
        }

        @Test
        @DisplayName("使模板缓存失效")
        void shouldInvalidateTemplateCache() {
            Long groupId = 1L;
            cacheManager.putTemplates(groupId, createTestTemplates());

            cacheManager.invalidateTemplates(groupId);

            assertNull(cacheManager.getTemplates(groupId));
        }

        @Test
        @DisplayName("使所有模板缓存失效")
        void shouldInvalidateAllTemplates() {
            cacheManager.putTemplates(1L, createTestTemplates());
            cacheManager.putTemplates(2L, createTestTemplates());

            cacheManager.invalidateAllTemplates();

            assertNull(cacheManager.getTemplates(1L));
            assertNull(cacheManager.getTemplates(2L));
        }

        private List<GenTemplate> createTestTemplates() {
            GenTemplate template = new GenTemplate();
            template.setId(1L);
            template.setCode("entity");
            template.setName("Entity Template");
            return List.of(template);
        }
    }

    @Nested
    @DisplayName("元数据缓存测试")
    class MetadataCacheTests {

        @Test
        @DisplayName("缓存表元数据")
        void shouldCacheMetadata() {
            Long datasourceId = 1L;
            String tableName = "sys_user";
            TableMetadata metadata = createTestMetadata(tableName);

            cacheManager.putMetadata(datasourceId, tableName, metadata);
            TableMetadata cached = cacheManager.getMetadata(datasourceId, tableName);

            assertNotNull(cached);
            assertEquals(tableName, cached.getTableName());
        }

        @Test
        @DisplayName("获取不存在的元数据缓存返回null")
        void shouldReturnNullForNonExistentMetadataCache() {
            assertNull(cacheManager.getMetadata(999L, "non_existent_table"));
        }

        @Test
        @DisplayName("使元数据缓存失效")
        void shouldInvalidateMetadataCache() {
            Long datasourceId = 1L;
            String tableName = "sys_user";
            cacheManager.putMetadata(datasourceId, tableName, createTestMetadata(tableName));

            cacheManager.invalidateMetadata(datasourceId, tableName);

            assertNull(cacheManager.getMetadata(datasourceId, tableName));
        }

        @Test
        @DisplayName("使数据源所有元数据缓存失效")
        void shouldInvalidateAllMetadataByDatasource() {
            Long datasourceId = 1L;
            cacheManager.putMetadata(datasourceId, "table1", createTestMetadata("table1"));
            cacheManager.putMetadata(datasourceId, "table2", createTestMetadata("table2"));
            cacheManager.putMetadata(2L, "table3", createTestMetadata("table3"));

            cacheManager.invalidateMetadataByDatasource(datasourceId);

            assertNull(cacheManager.getMetadata(datasourceId, "table1"));
            assertNull(cacheManager.getMetadata(datasourceId, "table2"));
            assertNotNull(cacheManager.getMetadata(2L, "table3")); // 其他数据源不受影响
        }

        @Test
        @DisplayName("使所有元数据缓存失效")
        void shouldInvalidateAllMetadata() {
            cacheManager.putMetadata(1L, "table1", createTestMetadata("table1"));
            cacheManager.putMetadata(2L, "table2", createTestMetadata("table2"));

            cacheManager.invalidateAllMetadata();

            assertNull(cacheManager.getMetadata(1L, "table1"));
            assertNull(cacheManager.getMetadata(2L, "table2"));
        }

        private TableMetadata createTestMetadata(String tableName) {
            return TableMetadata.builder()
                    .tableName(tableName)
                    .tableComment("Test Table")
                    .columns(Collections.emptyList())
                    .hasDateTime(false)
                    .hasBigDecimal(false)
                    .importPackages(Collections.emptyList())
                    .build();
        }
    }

    @Nested
    @DisplayName("渲染缓存测试")
    class RenderCacheTests {

        @Test
        @DisplayName("缓存渲染结果")
        void shouldCacheRenderResult() {
            Long templateId = 1L;
            int dataModelHash = 12345;
            String content = "Generated Code Content";

            cacheManager.putRenderResult(templateId, dataModelHash, content);
            String cached = cacheManager.getRenderResult(templateId, dataModelHash);

            assertEquals(content, cached);
        }

        @Test
        @DisplayName("获取不存在的渲染缓存返回null")
        void shouldReturnNullForNonExistentRenderCache() {
            assertNull(cacheManager.getRenderResult(999L, 12345));
        }

        @Test
        @DisplayName("使模板渲染缓存失效")
        void shouldInvalidateRenderByTemplate() {
            Long templateId = 1L;
            cacheManager.putRenderResult(templateId, 111, "content1");
            cacheManager.putRenderResult(templateId, 222, "content2");
            cacheManager.putRenderResult(2L, 333, "content3");

            cacheManager.invalidateRenderByTemplate(templateId);

            assertNull(cacheManager.getRenderResult(templateId, 111));
            assertNull(cacheManager.getRenderResult(templateId, 222));
            assertNotNull(cacheManager.getRenderResult(2L, 333)); // 其他模板不受影响
        }

        @Test
        @DisplayName("使所有渲染缓存失效")
        void shouldInvalidateAllRender() {
            cacheManager.putRenderResult(1L, 111, "content1");
            cacheManager.putRenderResult(2L, 222, "content2");

            cacheManager.invalidateAllRender();

            assertNull(cacheManager.getRenderResult(1L, 111));
            assertNull(cacheManager.getRenderResult(2L, 222));
        }
    }

    @Nested
    @DisplayName("缓存统计测试")
    class CacheStatsTests {

        @Test
        @DisplayName("获取缓存统计信息")
        void shouldGetCacheStats() {
            // 添加一些缓存数据
            cacheManager.putTemplates(1L, List.of(new GenTemplate()));
            cacheManager.putMetadata(1L, "table1", TableMetadata.builder()
                    .tableName("table1")
                    .columns(Collections.emptyList())
                    .importPackages(Collections.emptyList())
                    .build());
            cacheManager.putRenderResult(1L, 123, "content");

            GeneratorCacheManager.CacheStats stats = cacheManager.getStats();

            assertNotNull(stats);
            assertTrue(stats.templateCacheSize() >= 1);
            assertTrue(stats.metadataCacheSize() >= 1);
            assertTrue(stats.renderCacheSize() >= 1);
        }

        @Test
        @DisplayName("缓存统计toString")
        void statsShouldHaveReadableToString() {
            GeneratorCacheManager.CacheStats stats = cacheManager.getStats();
            String str = stats.toString();

            assertNotNull(str);
            assertTrue(str.contains("template"));
            assertTrue(str.contains("metadata"));
            assertTrue(str.contains("render"));
        }
    }

    @Nested
    @DisplayName("清理全部缓存测试")
    class ClearAllTests {

        @Test
        @DisplayName("清空所有缓存")
        void shouldClearAllCaches() {
            cacheManager.putTemplates(1L, List.of(new GenTemplate()));
            cacheManager.putMetadata(1L, "table1", TableMetadata.builder()
                    .tableName("table1")
                    .columns(Collections.emptyList())
                    .importPackages(Collections.emptyList())
                    .build());
            cacheManager.putRenderResult(1L, 123, "content");

            cacheManager.clearAll();

            assertNull(cacheManager.getTemplates(1L));
            assertNull(cacheManager.getMetadata(1L, "table1"));
            assertNull(cacheManager.getRenderResult(1L, 123));
        }
    }
}
