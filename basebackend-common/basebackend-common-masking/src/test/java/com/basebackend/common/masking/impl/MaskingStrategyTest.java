package com.basebackend.common.masking.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 数据脱敏策略单元测试
 */
class MaskingStrategyTest {

    // ========== PhoneMaskingStrategy ==========

    @Nested
    @DisplayName("手机号脱敏")
    class PhoneTest {
        private final PhoneMaskingStrategy strategy = new PhoneMaskingStrategy();

        @Test
        @DisplayName("标准11位手机号: 138****0000")
        void shouldMaskStandardPhone() {
            assertThat(strategy.mask("13800000000", '*')).isEqualTo("138****0000");
        }

        @Test
        @DisplayName("自定义脱敏字符")
        void shouldUseCustomMaskChar() {
            assertThat(strategy.mask("13800000000", '#')).isEqualTo("138####0000");
        }

        @Test
        @DisplayName("null 返回 null")
        void shouldReturnNullForNull() {
            assertThat(strategy.mask(null, '*')).isNull();
        }

        @Test
        @DisplayName("短号码原样返回")
        void shouldReturnShortValueAsIs() {
            assertThat(strategy.mask("123456", '*')).isEqualTo("123456");
        }
    }

    // ========== EmailMaskingStrategy ==========

    @Nested
    @DisplayName("邮箱脱敏")
    class EmailTest {
        private final EmailMaskingStrategy strategy = new EmailMaskingStrategy();

        @Test
        @DisplayName("标准邮箱: t***@example.com")
        void shouldMaskEmail() {
            assertThat(strategy.mask("test@example.com", '*')).isEqualTo("t***@example.com");
        }

        @Test
        @DisplayName("单字符用户名原样返回")
        void shouldReturnSingleCharUserAsIs() {
            assertThat(strategy.mask("a@b.com", '*')).isEqualTo("a@b.com");
        }

        @Test
        @DisplayName("无 @ 符号原样返回")
        void shouldReturnNoAtSignAsIs() {
            assertThat(strategy.mask("noemail", '*')).isEqualTo("noemail");
        }

        @Test
        @DisplayName("null 返回 null")
        void shouldReturnNullForNull() {
            assertThat(strategy.mask(null, '*')).isNull();
        }
    }

    // ========== IdCardMaskingStrategy ==========

    @Nested
    @DisplayName("身份证脱敏")
    class IdCardTest {
        private final IdCardMaskingStrategy strategy = new IdCardMaskingStrategy();

        @Test
        @DisplayName("18位身份证: 360***********1234")
        void shouldMask18DigitIdCard() {
            String result = strategy.mask("360123199001011234", '*');
            assertThat(result).startsWith("360");
            assertThat(result).endsWith("1234");
            assertThat(result).hasSize(18);
            assertThat(result.substring(3, 14)).isEqualTo("***********");
        }

        @Test
        @DisplayName("15位身份证也能脱敏")
        void shouldMask15DigitIdCard() {
            String result = strategy.mask("360123900101123", '*');
            assertThat(result).startsWith("360");
            assertThat(result).endsWith("1123");
            assertThat(result).hasSize(15);
            // 前3 + 中间8个* + 后4
            assertThat(result.substring(3, 11)).isEqualTo("********");
        }

        @Test
        @DisplayName("短字符串原样返回")
        void shouldReturnShortValueAsIs() {
            assertThat(strategy.mask("123456", '*')).isEqualTo("123456");
        }

        @Test
        @DisplayName("null 返回 null")
        void shouldReturnNullForNull() {
            assertThat(strategy.mask(null, '*')).isNull();
        }
    }

    // ========== BankCardMaskingStrategy ==========

    @Nested
    @DisplayName("银行卡脱敏")
    class BankCardTest {
        private final BankCardMaskingStrategy strategy = new BankCardMaskingStrategy();

        @Test
        @DisplayName("16位卡号: ************1234")
        void shouldMask16DigitCard() {
            String result = strategy.mask("6222021234561234", '*');
            assertThat(result).endsWith("1234");
            assertThat(result).hasSize(16);
            assertThat(result.substring(0, 12)).isEqualTo("************");
        }

        @Test
        @DisplayName("19位卡号也能脱敏")
        void shouldMask19DigitCard() {
            String result = strategy.mask("6222021234567891234", '*');
            assertThat(result).endsWith("1234");
            assertThat(result).hasSize(19);
        }

        @Test
        @DisplayName("4位及以下原样返回")
        void shouldReturnShortValueAsIs() {
            assertThat(strategy.mask("1234", '*')).isEqualTo("1234");
        }

        @Test
        @DisplayName("null 返回 null")
        void shouldReturnNullForNull() {
            assertThat(strategy.mask(null, '*')).isNull();
        }
    }

    // ========== AddressMaskingStrategy ==========

    @Nested
    @DisplayName("地址脱敏")
    class AddressTest {
        private final AddressMaskingStrategy strategy = new AddressMaskingStrategy();

        @Test
        @DisplayName("标准地址: 江西省南昌市****")
        void shouldMaskAddress() {
            String result = strategy.mask("江西省南昌市青山湖区XXX路", '*');
            assertThat(result).startsWith("江西省南昌市");
            assertThat(result).doesNotContain("青山湖");
        }

        @Test
        @DisplayName("6字及以下原样返回")
        void shouldReturnShortValueAsIs() {
            assertThat(strategy.mask("北京市", '*')).isEqualTo("北京市");
            assertThat(strategy.mask("江西省南昌市", '*')).isEqualTo("江西省南昌市");
        }

        @Test
        @DisplayName("null 返回 null")
        void shouldReturnNullForNull() {
            assertThat(strategy.mask(null, '*')).isNull();
        }
    }

    // ========== MaskingStrategyRegistry ==========

    @Nested
    @DisplayName("MaskingStrategyRegistry")
    class RegistryTest {

        @Test
        @DisplayName("注册和获取策略")
        void shouldRegisterAndGet() {
            var registry = new com.basebackend.common.masking.MaskingStrategyRegistry();
            var phoneStrategy = new PhoneMaskingStrategy();
            registry.register(com.basebackend.common.masking.MaskType.PHONE, phoneStrategy);

            assertThat(registry.get(com.basebackend.common.masking.MaskType.PHONE)).isSameAs(phoneStrategy);
            assertThat(registry.get(com.basebackend.common.masking.MaskType.EMAIL)).isNull();
        }
    }
}
