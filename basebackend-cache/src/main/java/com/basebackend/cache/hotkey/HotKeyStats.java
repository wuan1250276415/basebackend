package com.basebackend.cache.hotkey;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 热点 Key 统计信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HotKeyStats implements Comparable<HotKeyStats> {

    private String key;
    private long accessCount;
    private boolean hot;

    @Override
    public int compareTo(HotKeyStats other) {
        // 降序排列（访问量大的排前面）
        int cmp = Long.compare(other.accessCount, this.accessCount);
        if (cmp != 0) {
            return cmp;
        }
        return this.key.compareTo(other.key);
    }
}
