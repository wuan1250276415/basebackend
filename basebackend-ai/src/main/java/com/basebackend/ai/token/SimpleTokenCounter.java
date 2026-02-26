package com.basebackend.ai.token;

/**
 * 简单 Token 计数器
 * <p>
 * 基于字符数估算 Token 数量：
 * - 英文约 4 字符 = 1 Token
 * - 中文约 1-2 字符 = 1 Token
 * - 混合文本取加权平均
 */
public class SimpleTokenCounter implements TokenCounter {

    @Override
    public int countTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }

        int chineseChars = 0;
        int otherChars = 0;

        for (char c : text.toCharArray()) {
            if (isChinese(c)) {
                chineseChars++;
            } else {
                otherChars++;
            }
        }

        // 中文：约 1.5 字符/token，英文：约 4 字符/token
        return (int) Math.ceil(chineseChars / 1.5 + otherChars / 4.0);
    }

    private boolean isChinese(char c) {
        return Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN;
    }
}
