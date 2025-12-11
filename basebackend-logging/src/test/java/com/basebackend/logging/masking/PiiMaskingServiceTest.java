package com.basebackend.logging.masking;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doNothing;

/**
 * PiiMaskingService 单元测试
 * P0优化：增加测试覆盖率
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PiiMaskingService PII脱敏服务测试")
class PiiMaskingServiceTest {

    @Mock
    private MaskingMetrics metrics;

    private MaskingProperties properties;
    private ObjectMapper mapper;
    private PiiMaskingService maskingService;

    @BeforeEach
    void setUp() {
        properties = new MaskingProperties();
        properties.setEnabled(true);
        mapper = new ObjectMapper();
        doNothing().when(metrics).record(anyLong(), anyBoolean());
        maskingService = new PiiMaskingService(properties, metrics, mapper);
    }

    @Nested
    @DisplayName("字符串脱敏测试")
    class StringMaskingTests {

        @Test
        @DisplayName("手机号脱敏 - 应保留前3后2位")
        void shouldMaskPhoneNumber() {
            String input = "我的手机号是13812345678";
            String result = maskingService.mask(input);
            assertThat(result).contains("138");
            assertThat(result).contains("78");
            assertThat(result).doesNotContain("12345");
        }

        @Test
        @DisplayName("身份证脱敏 - 应保留前2后2位")
        void shouldMaskIdCard() {
            String input = "身份证号：110101199001011234";
            String result = maskingService.mask(input);
            assertThat(result).contains("11");
            assertThat(result).contains("34");
        }

        @Test
        @DisplayName("银行卡脱敏 - 应保留前4后4位")
        void shouldMaskBankCard() {
            String input = "银行卡：6222021234567890123";
            String result = maskingService.mask(input);
            assertThat(result).contains("6222");
        }

        @Test
        @DisplayName("邮箱脱敏")
        void shouldMaskEmail() {
            String input = "邮箱：test@example.com";
            String result = maskingService.mask(input);
            assertThat(result).isNotEqualTo(input);
        }

        @Test
        @DisplayName("null输入 - 应返回null")
        void shouldReturnNullForNullInput() {
            String result = maskingService.mask((String) null);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("空字符串 - 应返回空字符串")
        void shouldReturnEmptyForEmptyInput() {
            String result = maskingService.mask("");
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("无敏感信息 - 应返回原字符串")
        void shouldReturnOriginalForNoSensitiveData() {
            String input = "这是一段普通文本";
            String result = maskingService.mask(input);
            assertThat(result).isEqualTo(input);
        }
    }

    @Nested
    @DisplayName("禁用脱敏测试")
    class DisabledMaskingTests {

        @Test
        @DisplayName("禁用时应返回原始数据")
        void shouldReturnOriginalWhenDisabled() {
            properties.setEnabled(false);
            maskingService = new PiiMaskingService(properties, metrics, mapper);
            
            String input = "手机号：13812345678";
            String result = maskingService.mask(input);
            
            assertThat(result).isEqualTo(input);
        }
    }

    @Nested
    @DisplayName("对象脱敏测试")
    class ObjectMaskingTests {

        @Test
        @DisplayName("null对象 - 应返回null")
        void shouldReturnNullForNullObject() {
            Object result = maskingService.mask((Object) null);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("字符串对象 - 应调用字符串脱敏")
        void shouldMaskStringObject() {
            Object input = "手机号：13812345678";
            Object result = maskingService.mask(input);
            assertThat(result).isInstanceOf(String.class);
            assertThat((String) result).doesNotContain("12345");
        }
    }

    @Nested
    @DisplayName("脱敏策略测试")
    class MaskingStrategyTests {

        @Test
        @DisplayName("HASH策略 - 应返回SHA256哈希")
        void shouldApplyHashStrategy() {
            List<MaskingRule> rules = new ArrayList<>();
            MaskingRule rule = new MaskingRule();
            rule.setName("hash-test");
            rule.setRegex("\\bSECRET\\b");
            rule.setStrategy(MaskingStrategy.HASH);
            rule.setEnabled(true);
            rules.add(rule);
            
            properties.setRules(rules);
            maskingService = new PiiMaskingService(properties, metrics, mapper);
            
            String input = "密钥是SECRET";
            String result = maskingService.mask(input);
            
            assertThat(result).doesNotContain("SECRET");
            assertThat(result.length()).isGreaterThan(input.length());
        }

        @Test
        @DisplayName("REMOVE策略 - 应移除匹配内容")
        void shouldApplyRemoveStrategy() {
            List<MaskingRule> rules = new ArrayList<>();
            MaskingRule rule = new MaskingRule();
            rule.setName("remove-test");
            rule.setRegex("\\bREMOVE_ME\\b");
            rule.setStrategy(MaskingStrategy.REMOVE);
            rule.setEnabled(true);
            rules.add(rule);
            
            properties.setRules(rules);
            maskingService = new PiiMaskingService(properties, metrics, mapper);
            
            String input = "请移除REMOVE_ME这个词";
            String result = maskingService.mask(input);
            
            assertThat(result).doesNotContain("REMOVE_ME");
        }

        @Test
        @DisplayName("CUSTOM策略 - 应使用自定义替换")
        void shouldApplyCustomStrategy() {
            List<MaskingRule> rules = new ArrayList<>();
            MaskingRule rule = new MaskingRule();
            rule.setName("custom-test");
            rule.setRegex("\\bCUSTOM\\b");
            rule.setStrategy(MaskingStrategy.CUSTOM);
            rule.setReplacement("[REDACTED]");
            rule.setEnabled(true);
            rules.add(rule);
            
            properties.setRules(rules);
            maskingService = new PiiMaskingService(properties, metrics, mapper);
            
            String input = "替换CUSTOM为自定义值";
            String result = maskingService.mask(input);
            
            assertThat(result).contains("[REDACTED]");
            assertThat(result).doesNotContain("CUSTOM");
        }

        @Test
        @DisplayName("禁用的规则 - 应被跳过")
        void shouldSkipDisabledRules() {
            List<MaskingRule> rules = new ArrayList<>();
            MaskingRule rule = new MaskingRule();
            rule.setName("disabled-test");
            rule.setRegex("\\bSKIP\\b");
            rule.setStrategy(MaskingStrategy.REMOVE);
            rule.setEnabled(false);
            rules.add(rule);
            
            properties.setRules(rules);
            maskingService = new PiiMaskingService(properties, metrics, mapper);
            
            String input = "不要跳过SKIP这个词";
            String result = maskingService.mask(input);
            
            assertThat(result).contains("SKIP");
        }
    }

    @Nested
    @DisplayName("规则热更新测试")
    class RuleReloadTests {

        @Test
        @DisplayName("重新加载规则 - 应使用新规则")
        void shouldReloadRules() {
            String input = "新规则NEW_RULE测试";
            
            // 初始规则不匹配
            String result1 = maskingService.mask(input);
            assertThat(result1).contains("NEW_RULE");
            
            // 添加新规则
            List<MaskingRule> newRules = new ArrayList<>();
            MaskingRule rule = new MaskingRule();
            rule.setName("new-rule");
            rule.setRegex("\\bNEW_RULE\\b");
            rule.setStrategy(MaskingStrategy.REMOVE);
            rule.setEnabled(true);
            newRules.add(rule);
            
            maskingService.reloadRules(newRules);
            
            // 新规则应生效
            String result2 = maskingService.mask(input);
            assertThat(result2).doesNotContain("NEW_RULE");
        }

        @Test
        @DisplayName("空规则列表 - 应正常处理")
        void shouldHandleEmptyRules() {
            maskingService.reloadRules(new ArrayList<>());
            
            String input = "手机号：13812345678";
            String result = maskingService.mask(input);
            
            // 无规则时返回原始数据
            assertThat(result).isEqualTo(input);
        }

        @Test
        @DisplayName("null规则列表 - 应正常处理")
        void shouldHandleNullRules() {
            maskingService.reloadRules(null);
            
            String input = "手机号：13812345678";
            String result = maskingService.mask(input);
            
            assertThat(result).isEqualTo(input);
        }
    }

    @Nested
    @DisplayName("并发安全测试")
    class ConcurrencyTests {

        @Test
        @DisplayName("多线程脱敏 - 应线程安全")
        void shouldBeThreadSafe() throws InterruptedException {
            int threadCount = 10;
            Thread[] threads = new Thread[threadCount];
            boolean[] results = new boolean[threadCount];
            
            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                threads[i] = new Thread(() -> {
                    try {
                        String input = "手机号：1381234567" + index;
                        String result = maskingService.mask(input);
                        results[index] = result != null && !result.equals(input);
                    } catch (Exception e) {
                        results[index] = false;
                    }
                });
            }
            
            for (Thread thread : threads) {
                thread.start();
            }
            
            for (Thread thread : threads) {
                thread.join();
            }
            
            for (boolean result : results) {
                assertThat(result).isTrue();
            }
        }
    }
}
