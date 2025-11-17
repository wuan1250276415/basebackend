package com.basebackend.profile.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.profile.entity.UserPreference;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户偏好设置 Mapper 接口
 *
 * @author BaseBackend Team
 * @since 2025-11-14
 */
@Mapper
public interface UserPreferenceMapper extends BaseMapper<UserPreference> {
}
