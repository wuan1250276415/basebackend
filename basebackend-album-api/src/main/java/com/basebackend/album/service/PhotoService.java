package com.basebackend.album.service;

import com.basebackend.album.dto.BatchDeleteDTO;
import com.basebackend.album.dto.MovePhotoDTO;
import com.basebackend.album.dto.UpdatePhotoDTO;
import com.basebackend.album.dto.UploadPhotoDTO;
import com.basebackend.album.vo.PhotoVO;
import com.basebackend.common.dto.PageResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 照片/视频服务接口
 *
 * @author BearTeam
 */
public interface PhotoService {

    /** 上传照片 */
    PhotoVO uploadPhoto(MultipartFile file, UploadPhotoDTO dto, Long userId);

    /** 照片列表（相册内分页） */
    PageResult<PhotoVO> listPhotos(Long albumId, int page, int size, Long userId);

    /** 照片详情 */
    PhotoVO getPhotoDetail(Long photoId, Long userId);

    /** 编辑照片信息 */
    void updatePhoto(Long photoId, UpdatePhotoDTO dto, Long userId);

    /** 删除照片（软删除） */
    void deletePhoto(Long photoId, Long userId);

    /** 批量删除 */
    void batchDelete(BatchDeleteDTO dto, Long userId);

    /** 移动到其他相册 */
    void movePhoto(Long photoId, MovePhotoDTO dto, Long userId);

    /** 点赞 */
    void likePhoto(Long photoId, Long userId);

    /** 取消点赞 */
    void unlikePhoto(Long photoId, Long userId);

    /** 下载原图（返回文件路径） */
    String getDownloadPath(Long photoId, Long userId);

    /** 搜索照片 */
    PageResult<PhotoVO> searchPhotos(String keyword, int page, int size, Long userId);

    /** 回收站列表 */
    PageResult<PhotoVO> listTrash(int page, int size, Long userId);

    /** 恢复照片 */
    void restorePhoto(Long photoId, Long userId);

    /** 彻底删除 */
    void permanentDelete(Long photoId, Long userId);

    /** 清空回收站 */
    void clearTrash(Long userId);
}
