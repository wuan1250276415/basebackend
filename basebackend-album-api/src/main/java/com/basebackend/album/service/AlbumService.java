package com.basebackend.album.service;

import com.basebackend.album.dto.CreateAlbumDTO;
import com.basebackend.album.dto.SetCoverDTO;
import com.basebackend.album.dto.UpdateAlbumDTO;
import com.basebackend.album.vo.AlbumVO;
import com.basebackend.common.dto.PageResult;

import java.util.List;

/**
 * 相册服务接口
 *
 * @author BearTeam
 */
public interface AlbumService {

    /** 创建相册 */
    AlbumVO createAlbum(CreateAlbumDTO dto, Long userId);

    /** 相册列表（个人 + 所属家庭的家庭相册） */
    PageResult<AlbumVO> listAlbums(Long userId, int page, int size);

    /** 相册详情 */
    AlbumVO getAlbumDetail(Long albumId, Long userId);

    /** 编辑相册 */
    void updateAlbum(Long albumId, UpdateAlbumDTO dto, Long userId);

    /** 删除相册（级联软删除照片） */
    void deleteAlbum(Long albumId, Long userId);

    /** 设置封面 */
    void setCover(Long albumId, SetCoverDTO dto, Long userId);

    /** 家庭相册列表 */
    List<AlbumVO> listFamilyAlbums(Long familyId, Long userId);
}
