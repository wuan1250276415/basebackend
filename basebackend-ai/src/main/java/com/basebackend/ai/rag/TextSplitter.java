package com.basebackend.ai.rag;

import java.util.ArrayList;
import java.util.List;

/**
 * 文本分割器
 * <p>
 * 将长文本按固定大小切分为多个块，支持重叠以保留上下文连贯性。
 */
public class TextSplitter {

    private final int chunkSize;
    private final int chunkOverlap;

    public TextSplitter(int chunkSize, int chunkOverlap) {
        if (chunkSize <= 0) throw new IllegalArgumentException("chunkSize 必须大于 0");
        if (chunkOverlap < 0) throw new IllegalArgumentException("chunkOverlap 不能为负数");
        if (chunkOverlap >= chunkSize) throw new IllegalArgumentException("chunkOverlap 必须小于 chunkSize");
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
    }

    /**
     * 将文本分割为多个块
     *
     * @param text 原始文本
     * @return 文本块列表
     */
    public List<String> split(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;
        int length = text.length();

        while (start < length) {
            int end = Math.min(start + chunkSize, length);

            // 尽量在句号、换行处断开
            if (end < length) {
                int breakPoint = findBreakPoint(text, start, end);
                if (breakPoint > start) {
                    end = breakPoint;
                }
            }

            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            // 到达文本尾部后直接结束，避免 overlap 回退导致死循环
            if (end >= length) {
                break;
            }

            int nextStart = end - chunkOverlap;
            // 保证起点单调递增，避免异常断点或大 overlap 造成回退/卡死
            if (nextStart <= start) {
                nextStart = start + 1;
            }
            start = nextStart;
        }

        return chunks;
    }

    /**
     * 在指定范围内查找合适的断点（句号、换行等）
     */
    private int findBreakPoint(String text, int start, int end) {
        // 从 end 往回找，优先在自然断句处切分
        for (int i = end; i > start + chunkSize / 2; i--) {
            char c = text.charAt(i - 1);
            if (c == '\n' || c == '。' || c == '.' || c == '！' || c == '?' || c == '；') {
                return i;
            }
        }
        return end;
    }
}
