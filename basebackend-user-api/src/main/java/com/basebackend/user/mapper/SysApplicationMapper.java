package com.basebackend.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.user.entity.SysApplication;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 应用信息Mapper
 */
@Mapper
public interface SysApplicationMapper extends BaseMapper<SysApplication> {

    /**
     * 查询所有启用的应用
     *
     * @return 应用列表
     */
    List<SysApplication> selectEnabledApplications();

    /**
     * 根据应用编码查询应用
     *
     * @param appCode 应用编码
     * @return 应用信息
     */
    SysApplication selectByAppCode(String appCode);
}
