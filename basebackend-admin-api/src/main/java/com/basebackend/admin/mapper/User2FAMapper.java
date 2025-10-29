package com.basebackend.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.admin.entity.User2FA;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户双因素认证 Mapper 接口
 *
 * @author Claude Code
 * @since 2025-10-30
 */
@Mapper
public interface User2FAMapper extends BaseMapper<User2FA> {
}
