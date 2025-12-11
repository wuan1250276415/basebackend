package com.basebackend.generator.core.metadata;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 抽象元数据读取器基类测试
 */
@DisplayName("AbstractDatabaseMetadataReader 测试")
class AbstractDatabaseMetadataReaderTest {

    // 创建一个测试用的具体实现
    private final TestMetadataReader reader = new TestMetadataReader();

    @Nested
    @DisplayName("系统字段判断测试")
    class SystemFieldTests {

        @Test
        @DisplayName("标准系统字段应被识别")
        void shouldRecognizeStandardSystemFields() {
            assertTrue(reader.isSystemFieldPublic("id"));
            assertTrue(reader.isSystemFieldPublic("create_time"));
            assertTrue(reader.isSystemFieldPublic("update_time"));
            assertTrue(reader.isSystemFieldPublic("create_by"));
            assertTrue(reader.isSystemFieldPublic("update_by"));
            assertTrue(reader.isSystemFieldPublic("deleted"));
        }

        @Test
        @DisplayName("扩展系统字段应被识别")
        void shouldRecognizeExtendedSystemFields() {
            assertTrue(reader.isSystemFieldPublic("created_at"));
            assertTrue(reader.isSystemFieldPublic("updated_at"));
            assertTrue(reader.isSystemFieldPublic("created_by"));
            assertTrue(reader.isSystemFieldPublic("updated_by"));
        }

        @Test
        @DisplayName("普通字段不应被识别为系统字段")
        void shouldNotRecognizeNormalFieldsAsSystem() {
            assertFalse(reader.isSystemFieldPublic("username"));
            assertFalse(reader.isSystemFieldPublic("email"));
            assertFalse(reader.isSystemFieldPublic("status"));
        }

        @Test
        @DisplayName("系统字段识别不区分大小写")
        void shouldBeCaseInsensitive() {
            assertTrue(reader.isSystemFieldPublic("ID"));
            assertTrue(reader.isSystemFieldPublic("Create_Time"));
            assertTrue(reader.isSystemFieldPublic("DELETED"));
        }
    }

    @Nested
    @DisplayName("可查询字段判断测试")
    class QueryableFieldTests {

        @Test
        @DisplayName("普通字段应该可查询")
        void normalFieldsShouldBeQueryable() {
            assertTrue(reader.isQueryablePublic("username", "varchar"));
            assertTrue(reader.isQueryablePublic("status", "int"));
            assertTrue(reader.isQueryablePublic("email", "varchar"));
        }

        @Test
        @DisplayName("系统字段不可查询")
        void systemFieldsShouldNotBeQueryable() {
            assertFalse(reader.isQueryablePublic("id", "bigint"));
            assertFalse(reader.isQueryablePublic("create_time", "datetime"));
            assertFalse(reader.isQueryablePublic("deleted", "tinyint"));
        }

        @Test
        @DisplayName("大文本类型不可查询")
        void largeTextTypesShouldNotBeQueryable() {
            assertFalse(reader.isQueryablePublic("content", "text"));
            assertFalse(reader.isQueryablePublic("content", "longtext"));
            assertFalse(reader.isQueryablePublic("content", "mediumtext"));
            assertFalse(reader.isQueryablePublic("content", "clob"));
            assertFalse(reader.isQueryablePublic("content", "blob"));
            assertFalse(reader.isQueryablePublic("content", "bytea"));
        }
    }

    @Nested
    @DisplayName("日期时间类型判断测试")
    class DateTimeTypeTests {

        @Test
        @DisplayName("日期时间类型应被识别")
        void shouldRecognizeDateTimeTypes() {
            assertTrue(reader.isDateTimeTypePublic("LocalDateTime"));
            assertTrue(reader.isDateTimeTypePublic("LocalDate"));
            assertTrue(reader.isDateTimeTypePublic("LocalTime"));
            assertTrue(reader.isDateTimeTypePublic("Date"));
            assertTrue(reader.isDateTimeTypePublic("Timestamp"));
        }

        @Test
        @DisplayName("非日期时间类型不应被识别")
        void shouldNotRecognizeNonDateTimeTypes() {
            assertFalse(reader.isDateTimeTypePublic("String"));
            assertFalse(reader.isDateTimeTypePublic("Integer"));
            assertFalse(reader.isDateTimeTypePublic("BigDecimal"));
            assertFalse(reader.isDateTimeTypePublic(null));
        }
    }

    @Nested
    @DisplayName("常量测试")
    class ConstantsTests {

        @Test
        @DisplayName("系统字段集合包含所有必要字段")
        void systemFieldsShouldContainAllNecessaryFields() {
            Set<String> expectedFields = Set.of(
                    "id", "create_time", "update_time", "create_by", "update_by",
                    "deleted", "created_at", "updated_at", "created_by", "updated_by");

            for (String field : expectedFields) {
                assertTrue(reader.isSystemFieldPublic(field),
                        "系统字段集合应包含: " + field);
            }
        }

        @Test
        @DisplayName("不可查询类型集合包含所有大文本类型")
        void nonQueryableTypesShouldContainAllLargeTextTypes() {
            Set<String> expectedTypes = Set.of(
                    "text", "longtext", "mediumtext", "clob", "blob", "bytea");

            for (String type : expectedTypes) {
                assertFalse(reader.isQueryablePublic("normalField", type),
                        "不可查询类型集合应包含: " + type);
            }
        }
    }

    /**
     * 测试用的具体实现类
     */
    private static class TestMetadataReader extends AbstractDatabaseMetadataReader {

        // 暴露protected方法用于测试
        public boolean isSystemFieldPublic(String columnName) {
            return isSystemField(columnName);
        }

        public boolean isQueryablePublic(String columnName, String dataType) {
            return isQueryable(columnName, dataType);
        }

        public boolean isDateTimeTypePublic(String javaType) {
            return isDateTimeType(javaType);
        }

        @Override
        public java.util.List<String> getTableNames(javax.sql.DataSource dataSource, String schema) {
            return java.util.Collections.emptyList();
        }

        @Override
        public java.util.List<ColumnMetadata> getColumns(javax.sql.DataSource dataSource, String tableName) {
            return java.util.Collections.emptyList();
        }

        @Override
        protected String getTableComment(javax.sql.DataSource dataSource, String tableName) {
            return "";
        }

        @Override
        protected String getDatabaseTypeName() {
            return "Test";
        }
    }
}
