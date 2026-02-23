package com.basebackend.common.masking;

import com.basebackend.common.masking.impl.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MaskingStrategyTest {

    @Test
    void phoneMasking() {
        PhoneMaskingStrategy strategy = new PhoneMaskingStrategy();
        assertEquals("138****1234", strategy.mask("13812341234", '*'));
        assertNull(strategy.mask(null, '*'));
        assertEquals("123456", strategy.mask("123456", '*'));
    }

    @Test
    void emailMasking() {
        EmailMaskingStrategy strategy = new EmailMaskingStrategy();
        assertEquals("t***@example.com", strategy.mask("test@example.com", '*'));
        assertNull(strategy.mask(null, '*'));
        assertEquals("noemail", strategy.mask("noemail", '*'));
    }

    @Test
    void idCardMasking() {
        IdCardMaskingStrategy strategy = new IdCardMaskingStrategy();
        assertEquals("110***********1234", strategy.mask("110123199901011234", '*'));
        assertNull(strategy.mask(null, '*'));
    }

    @Test
    void bankCardMasking() {
        BankCardMaskingStrategy strategy = new BankCardMaskingStrategy();
        assertEquals("***************1234", strategy.mask("6222021234567891234", '*'));
        assertNull(strategy.mask(null, '*'));
        assertEquals("1234", strategy.mask("1234", '*'));
    }

    @Test
    void addressMasking() {
        AddressMaskingStrategy strategy = new AddressMaskingStrategy();
        assertEquals("北京市海淀区*******", strategy.mask("北京市海淀区中关村大街1号", '*'));
        assertNull(strategy.mask(null, '*'));
        assertEquals("短地址abc", strategy.mask("短地址abc", '*'));
    }
}
