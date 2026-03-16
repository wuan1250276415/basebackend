package com.basebackend.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("NacosUtils 单元测试")
class NacosUtilsTest {

    @Test
    @DisplayName("encodeToBase64 正常编码")
    void shouldEncodeToBase64() {
        assertThat(NacosUtils.encodeToBase64("nacos-secret")).isEqualTo("bmFjb3Mtc2VjcmV0");
    }

    @Test
    @DisplayName("encodeToBase64 空输入抛出异常")
    void shouldThrowForBlankInput() {
        assertThatThrownBy(() -> NacosUtils.encodeToBase64(" "))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("不能为空");
    }

    @Test
    @DisplayName("main 空参数输出使用提示")
    void shouldPrintUsageWhenArgsEmpty() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try {
            System.setOut(new PrintStream(outputStream, true, StandardCharsets.UTF_8));
            NacosUtils.main(new String[0]);
        } finally {
            System.setOut(originalOut);
        }
        String output = outputStream.toString(StandardCharsets.UTF_8);
        assertThat(output).contains("用法：java com.basebackend.common.util.NacosUtils <明文密钥>");
        assertThat(output).doesNotContain("密钥Base64编码");
    }
}
