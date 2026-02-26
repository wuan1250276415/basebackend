package com.basebackend.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.file.entity.FileMetadata;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 文件元数据Mapper
 */
@Mapper
public interface FileMetadataMapper extends BaseMapper<FileMetadata> {

    /**
     * 按存储类型聚合文件数量和大小
     */
    @Select("""
            SELECT COALESCE(storage_type, 'LOCAL') AS storage_type,
            COUNT(*) AS file_count,
            COALESCE(SUM(file_size), 0) AS total_size
            FROM file_metadata WHERE is_deleted = 0
            GROUP BY storage_type
            """)
    List<Map<String, Object>> selectStorageStatistics();

    /**
     * 按文件扩展名聚合文件数量和大小
     */
    @Select("""
            SELECT COALESCE(LOWER(file_extension), 'unknown') AS file_type,
            COUNT(*) AS file_count,
            COALESCE(SUM(file_size), 0) AS total_size
            FROM file_metadata WHERE is_deleted = 0
            GROUP BY LOWER(file_extension)
            ORDER BY total_size DESC
            """)
    List<Map<String, Object>> selectFileTypeDistribution();

    /**
     * 查询文件总数和总大小
     */
    @Select("""
            SELECT COUNT(*) AS total_files,
            COALESCE(SUM(file_size), 0) AS total_size
            FROM file_metadata WHERE is_deleted = 0
            """)
    Map<String, Object> selectFileSummary();
}
