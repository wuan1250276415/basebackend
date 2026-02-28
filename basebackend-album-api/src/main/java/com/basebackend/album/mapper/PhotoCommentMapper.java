package com.basebackend.album.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.album.entity.PhotoComment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 照片评论 Mapper
 *
 * @author BearTeam
 */
@Mapper
public interface PhotoCommentMapper extends BaseMapper<PhotoComment> {
}
