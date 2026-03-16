package com.basebackend.album.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.album.entity.Album;
import org.apache.ibatis.annotations.Mapper;

/**
 * 相册 Mapper
 *
 * @author BearTeam
 */
@Mapper
public interface AlbumMapper extends BaseMapper<Album> {
}
