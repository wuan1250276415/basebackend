package com.basebackend.backup.infrastructure.executor;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * MySQL Binlog位置对象
 * 用于标识binlog文件中的特定位置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BinlogPosition {
    /**
     * Binlog文件名
     */
    private String filename;

    /**
     * 位置偏移量
     */
    private long position;

    /**
     * 创建BinlogPosition对象
     *
     * @param filename 文件名
     * @param position 位置
     * @return BinlogPosition对象
     */
    public static BinlogPosition of(String filename, long position) {
        return BinlogPosition.builder()
            .filename(filename)
            .position(position)
            .build();
    }

    /**
     * 转换为字符串格式 "filename:position"
     */
    @Override
    public String toString() {
        return filename + ":" + position;
    }

    /**
     * 从字符串解析BinlogPosition
     *
     * @param str 字符串格式 "filename:position"
     * @return BinlogPosition对象
     */
    public static BinlogPosition fromString(String str) {
        if (str == null || !str.contains(":")) {
            throw new IllegalArgumentException("无效的binlog位置格式: " + str);
        }
        String[] parts = str.split(":", 2);
        return BinlogPosition.of(parts[0], Long.parseLong(parts[1]));
    }

    /**
     * 检查位置是否有效
     */
    public boolean isValid() {
        return filename != null && !filename.trim().isEmpty() && position > 0;
    }

    /**
     * 比较两个位置（用于排序）
     */
    public int compareTo(BinlogPosition other) {
        if (other == null) return 1;

        int filenameCompare = this.filename.compareTo(other.filename);
        if (filenameCompare != 0) {
            return filenameCompare;
        }
        return Long.compare(this.position, other.position);
    }
}
