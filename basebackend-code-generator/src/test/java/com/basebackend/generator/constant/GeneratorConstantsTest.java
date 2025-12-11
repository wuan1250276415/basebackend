package com.basebackend.generator.constant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 代码生成器常量测试类
 */
@DisplayName("GeneratorConstants 测试")
class GeneratorConstantsTest {

    @Test
    @DisplayName("常量类不能被实例化")
    void shouldNotAllowInstantiation() throws NoSuchMethodException {
        Constructor<GeneratorConstants> constructor = GeneratorConstants.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        InvocationTargetException exception = assertThrows(InvocationTargetException.class, constructor::newInstance);
        assertInstanceOf(UnsupportedOperationException.class, exception.getCause());
    }

    @Test
    @DisplayName("状态常量值正确")
    void statusConstantsAreCorrect() {
        assertEquals(1, GeneratorConstants.STATUS_ENABLED);
        assertEquals(0, GeneratorConstants.STATUS_DISABLED);
    }

    @Test
    @DisplayName("连接池配置常量值合理")
    void poolConfigConstantsAreReasonable() {
        assertTrue(GeneratorConstants.POOL_INITIAL_SIZE > 0);
        assertTrue(GeneratorConstants.POOL_MIN_IDLE >= GeneratorConstants.POOL_INITIAL_SIZE);
        assertTrue(GeneratorConstants.POOL_MAX_ACTIVE >= GeneratorConstants.POOL_MIN_IDLE);
        assertTrue(GeneratorConstants.POOL_MAX_WAIT_MILLIS > 0);
        assertNotNull(GeneratorConstants.POOL_VALIDATION_QUERY);
    }

    @Test
    @DisplayName("占位符常量包含正确的格式")
    void placeholderConstantsHaveCorrectFormat() {
        assertTrue(GeneratorConstants.PLACEHOLDER_PACKAGE_PATH.contains("${"));
        assertTrue(GeneratorConstants.PLACEHOLDER_CLASS_NAME.contains("${"));
        assertTrue(GeneratorConstants.PLACEHOLDER_VARIABLE_NAME.contains("${"));
        assertTrue(GeneratorConstants.PLACEHOLDER_TEMPLATE_CODE.contains("${"));
    }

    @Test
    @DisplayName("默认类型常量不为空")
    void defaultTypeConstantsAreNotEmpty() {
        assertNotNull(GeneratorConstants.DEFAULT_JAVA_TYPE);
        assertNotNull(GeneratorConstants.DEFAULT_TS_TYPE);
        assertFalse(GeneratorConstants.DEFAULT_JAVA_TYPE.isEmpty());
        assertFalse(GeneratorConstants.DEFAULT_TS_TYPE.isEmpty());
    }

    @Test
    @DisplayName("数据源缓存配置合理")
    void datasourceCacheConfigIsReasonable() {
        assertTrue(GeneratorConstants.DATASOURCE_CACHE_EXPIRE_SECONDS > 0);
        assertTrue(GeneratorConstants.DATASOURCE_CACHE_MAX_SIZE > 0);
    }

    @Test
    @DisplayName("引擎类型常量与枚举匹配")
    void engineTypeConstantsMatchEnums() {
        assertEquals("FREEMARKER", GeneratorConstants.ENGINE_TYPE_FREEMARKER);
        assertEquals("VELOCITY", GeneratorConstants.ENGINE_TYPE_VELOCITY);
        assertEquals("THYMELEAF", GeneratorConstants.ENGINE_TYPE_THYMELEAF);
    }

    @Test
    @DisplayName("生成类型常量与枚举匹配")
    void generateTypeConstantsMatchEnums() {
        assertEquals("DOWNLOAD", GeneratorConstants.GENERATE_TYPE_DOWNLOAD);
        assertEquals("PREVIEW", GeneratorConstants.GENERATE_TYPE_PREVIEW);
        assertEquals("INCREMENT", GeneratorConstants.GENERATE_TYPE_INCREMENT);
    }
}
