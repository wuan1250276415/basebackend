package com.basebackend.album.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.album.entity.Photo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 照片/视频 Mapper
 *
 * @author BearTeam
 */
@Mapper
public interface PhotoMapper extends BaseMapper<Photo> {
}
