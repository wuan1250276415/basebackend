package com.basebackend.database.statistics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.database.statistics.entity.SqlStatistics;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * SQL统计Mapper
 */
@Mapper
public interface SqlStatisticsMapper extends BaseMapper<SqlStatistics> {

    /**
     * 根据SQL MD5查询统计信息
     */
    @Select("SELECT * FROM sys_sql_statistics WHERE sql_md5 = #{sqlMd5} AND deleted = 0 LIMIT 1")
    SqlStatistics selectByMd5(@Param("sqlMd5") String sqlMd5);
}
