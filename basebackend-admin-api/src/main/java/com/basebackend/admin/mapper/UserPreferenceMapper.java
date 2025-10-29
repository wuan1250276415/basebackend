package com.basebackend.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.admin.entity.UserPreference;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户偏好设置 Mapper
 *
 * @author Claude Code
 * @since 2025-10-29
 */
@Mapper
public interface UserPreferenceMapper extends BaseMapper<UserPreference> {
}
