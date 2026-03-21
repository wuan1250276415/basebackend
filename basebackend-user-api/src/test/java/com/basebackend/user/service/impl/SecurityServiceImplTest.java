package com.basebackend.user.service.impl;

import cn.hutool.core.codec.Base32;
import com.basebackend.common.context.UserContext;
import com.basebackend.common.context.UserContextHolder;
import com.basebackend.common.exception.BusinessException;
import com.basebackend.observability.metrics.CustomMetrics;
import com.basebackend.service.client.OperationLogServiceClient;
import com.basebackend.user.entity.User2FA;
import com.basebackend.user.mapper.SysUserMapper;
import com.basebackend.user.mapper.User2FAMapper;
import com.basebackend.user.mapper.UserDeviceMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityServiceImpl 2FA 校验测试")
class SecurityServiceImplTest {

    private static final String TEST_SECRET = "JBSWY3DPEHPK3PXP";

    @Mock
    private UserDeviceMapper deviceMapper;

    @Mock
    private OperationLogServiceClient operationLogServiceClient;

    @Mock
    private User2FAMapper user2FAMapper;

    @Mock
    private SysUserMapper userMapper;

    @Mock
    private CustomMetrics customMetrics;

    @InjectMocks
    private SecurityServiceImpl securityService;

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    @DisplayName("启用2FA时应校验TOTP验证码")
    void shouldEnable2FAWithValidTotpCode() {
        UserContextHolder.set(UserContext.builder().userId(1L).build());

        User2FA user2FA = new User2FA();
        user2FA.setId(1L);
        user2FA.setUserId(1L);
        user2FA.setType("totp");
        user2FA.setSecretKey(TEST_SECRET);
        user2FA.setEnabled(0);

        when(user2FAMapper.selectOne(any())).thenReturn(user2FA);

        securityService.enable2FA("totp", currentTotpCode(TEST_SECRET));

        verify(user2FAMapper).updateById(eq(user2FA));
    }

    @Test
    @DisplayName("禁用2FA时错误验证码应被拒绝")
    void shouldRejectDisable2FAWhenTotpCodeInvalid() {
        UserContextHolder.set(UserContext.builder().userId(1L).build());

        User2FA user2FA = new User2FA();
        user2FA.setId(1L);
        user2FA.setUserId(1L);
        user2FA.setType("totp");
        user2FA.setSecretKey(TEST_SECRET);
        user2FA.setEnabled(1);

        when(user2FAMapper.selectOne(any())).thenReturn(user2FA);

        assertThatThrownBy(() -> securityService.disable2FA("000000"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("验证码错误或已过期");

        verify(user2FAMapper, never()).updateById(any(User2FA.class));
    }

    @Test
    @DisplayName("未接入真实校验链的短信2FA应拒绝启用")
    void shouldRejectSms2FAUntilVerificationIsIntegrated() {
        UserContextHolder.set(UserContext.builder().userId(1L).build());

        User2FA user2FA = new User2FA();
        user2FA.setId(1L);
        user2FA.setUserId(1L);
        user2FA.setType("sms");
        user2FA.setEnabled(0);

        when(user2FAMapper.selectOne(any())).thenReturn(user2FA);

        assertThatThrownBy(() -> securityService.enable2FA("sms", "123456"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("尚未接入可信验证码校验");
    }

    private String currentTotpCode(String secret) {
        try {
            byte[] key = Base32.decode(secret);
            long counter = Instant.now().getEpochSecond() / 30;
            byte[] counterBytes = ByteBuffer.allocate(Long.BYTES).putLong(counter).array();

            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(counterBytes);

            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);
            int otp = binary % 1_000_000;
            return String.format("%06d", otp);
        } catch (Exception e) {
            throw new IllegalStateException("生成测试TOTP失败", e);
        }
    }
}
