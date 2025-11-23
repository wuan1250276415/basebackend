package com.basebackend.backup.infrastructure.executor;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * PostgreSQL WAL文件列表
 */
@Data
@NoArgsConstructor
public class WalFileList {

    private List<WalFileInfo> files = new ArrayList<>();

    /**
     * 添加文件
     */
    public void addFile(WalFileInfo file) {
        if (file != null) {
            files.add(file);
        }
    }

    /**
     * 获取文件数量
     */
    public int getFileCount() {
        return files.size();
    }

    /**
     * 获取所有文件
     */
    public List<WalFileInfo> getAllFiles() {
        return new ArrayList<>(files);
    }

    /**
     * 获取有效文件数量
     */
    public int getValidFileCount() {
        return (int) files.stream()
            .filter(WalFileInfo::isValid)
            .count();
    }
}
