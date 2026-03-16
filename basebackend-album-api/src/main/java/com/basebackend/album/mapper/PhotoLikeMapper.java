package com.basebackend.album.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.album.entity.PhotoLike;
import org.apache.ibatis.annotations.Mapper;

/**
 * 照片点赞 Mapper
 *
 * @author BearTeam
 */
@Mapper
public interface PhotoLikeMapper extends BaseMapper<PhotoLike> {
}
