package com.basebackend.album.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.album.dto.CreateAlbumDTO;
import com.basebackend.album.dto.SetCoverDTO;
import com.basebackend.album.dto.UpdateAlbumDTO;
import com.basebackend.album.entity.Album;
import com.basebackend.album.entity.FamilyGroup;
import com.basebackend.album.entity.FamilyMember;
import com.basebackend.album.entity.Photo;
import com.basebackend.album.enums.FamilyRole;
import com.basebackend.album.mapper.AlbumMapper;
import com.basebackend.album.mapper.FamilyGroupMapper;
import com.basebackend.album.mapper.FamilyMemberMapper;
import com.basebackend.album.mapper.PhotoMapper;
import com.basebackend.album.service.AlbumService;
import com.basebackend.album.vo.AlbumVO;
import com.basebackend.common.dto.PageResult;
import com.basebackend.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 相册服务实现
 *
 * @author BearTeam
 */
@Service
@RequiredArgsConstructor
public class AlbumServiceImpl implements AlbumService {

    private final AlbumMapper albumMapper;
    private final PhotoMapper photoMapper;
    private final FamilyGroupMapper familyGroupMapper;
    private final FamilyMemberMapper familyMemberMapper;

    /**
     * 将相册实体转为 VO
     */
    private AlbumVO toVO(Album album) {
        String familyName = null;
        if (album.getFamilyId() != null) {
            FamilyGroup family = familyGroupMapper.selectById(album.getFamilyId());
            if (family != null) {
                familyName = family.getName();
            }
        }
        // 封面URL：如果有封面照片ID，查询缩略图路径
        String coverUrl = null;
        if (album.getCoverPhotoId() != null) {
            Photo coverPhoto = photoMapper.selectById(album.getCoverPhotoId());
            if (coverPhoto != null) {
                coverUrl = coverPhoto.getThumbnailPath() != null
                        ? coverPhoto.getThumbnailPath() : coverPhoto.getFilePath();
            }
        }
        return new AlbumVO(
                album.getId(), album.getName(), album.getDescription(),
                coverUrl, album.getFamilyId(), familyName,
                album.getOwnerId(), null,
                album.getType(), album.getVisibility(),
                album.getPhotoCount(),
                album.getCreateTime(), album.getUpdateTime()
        );
    }

    /**
     * 获取用户所属的所有家庭ID
     */
    private List<Long> getUserFamilyIds(Long userId) {
        List<FamilyMember> members = familyMemberMapper.selectList(
                new LambdaQueryWrapper<FamilyMember>()
                        .eq(FamilyMember::getUserId, userId));
        return members.stream().map(FamilyMember::getFamilyId).toList();
    }

    @Override
    @Transactional
    public AlbumVO createAlbum(CreateAlbumDTO dto, Long userId) {
        // 如果是家庭相册，校验用户是否是家庭成员
        if (dto.familyId() != null) {
            FamilyMember member = familyMemberMapper.selectOne(
                    new LambdaQueryWrapper<FamilyMember>()
                            .eq(FamilyMember::getFamilyId, dto.familyId())
                            .eq(FamilyMember::getUserId, userId));
            if (member == null) {
                throw BusinessException.forbidden("您不是该家庭成员，无法创建家庭相册");
            }
        }

        Album album = new Album();
        album.setName(dto.name());
        album.setDescription(dto.description());
        album.setFamilyId(dto.familyId());
        album.setOwnerId(userId);
        album.setType(dto.type() != null ? dto.type() : 0);
        album.setVisibility(dto.visibility() != null ? dto.visibility() : 0);
        album.setPhotoCount(0);
        album.setSortOrder(0);
        albumMapper.insert(album);

        return toVO(album);
    }

    @Override
    public PageResult<AlbumVO> listAlbums(Long userId, int page, int size) {
        // 查询个人相册 + 所属家庭的相册
        List<Long> familyIds = getUserFamilyIds(userId);

        LambdaQueryWrapper<Album> wrapper = new LambdaQueryWrapper<Album>()
                .and(w -> {
                    w.eq(Album::getOwnerId, userId).isNull(Album::getFamilyId);
                    if (!familyIds.isEmpty()) {
                        w.or().in(Album::getFamilyId, familyIds);
                    }
                })
                .orderByDesc(Album::getUpdateTime);

        Page<Album> albumPage = albumMapper.selectPage(new Page<>(page, size), wrapper);
        List<AlbumVO> vos = albumPage.getRecords().stream().map(this::toVO).toList();

        return PageResult.of(vos, albumPage.getTotal(), (long) page, (long) size);
    }

    @Override
    public AlbumVO getAlbumDetail(Long albumId, Long userId) {
        Album album = albumMapper.selectById(albumId);
        if (album == null) {
            throw BusinessException.notFound("相册不存在");
        }
        return toVO(album);
    }

    @Override
    @Transactional
    public void updateAlbum(Long albumId, UpdateAlbumDTO dto, Long userId) {
        Album album = albumMapper.selectById(albumId);
        if (album == null) {
            throw BusinessException.notFound("相册不存在");
        }
        // 权限校验：创建者或家庭管理员
        if (!album.getOwnerId().equals(userId)) {
            if (album.getFamilyId() != null) {
                FamilyMember member = familyMemberMapper.selectOne(
                        new LambdaQueryWrapper<FamilyMember>()
                                .eq(FamilyMember::getFamilyId, album.getFamilyId())
                                .eq(FamilyMember::getUserId, userId));
                if (member == null || member.getRole() < FamilyRole.ADMIN.getCode()) {
                    throw BusinessException.forbidden("无权编辑此相册");
                }
            } else {
                throw BusinessException.forbidden("无权编辑此相册");
            }
        }
        if (dto.name() != null) {
            album.setName(dto.name());
        }
        if (dto.description() != null) {
            album.setDescription(dto.description());
        }
        if (dto.type() != null) {
            album.setType(dto.type());
        }
        if (dto.visibility() != null) {
            album.setVisibility(dto.visibility());
        }
        albumMapper.updateById(album);
    }

    @Override
    @Transactional
    public void deleteAlbum(Long albumId, Long userId) {
        Album album = albumMapper.selectById(albumId);
        if (album == null) {
            throw BusinessException.notFound("相册不存在");
        }
        if (!album.getOwnerId().equals(userId)) {
            throw BusinessException.forbidden("只有相册创建者才能删除");
        }
        // 级联软删除照片
        photoMapper.update(null,
                new LambdaUpdateWrapper<Photo>()
                        .eq(Photo::getAlbumId, albumId)
                        .set(Photo::getDeleted, 1));
        // 删除相册（逻辑删除）
        albumMapper.deleteById(albumId);
    }

    @Override
    @Transactional
    public void setCover(Long albumId, SetCoverDTO dto, Long userId) {
        Album album = albumMapper.selectById(albumId);
        if (album == null) {
            throw BusinessException.notFound("相册不存在");
        }
        // 校验照片是否属于该相册
        Photo photo = photoMapper.selectById(dto.photoId());
        if (photo == null || !photo.getAlbumId().equals(albumId)) {
            throw BusinessException.paramError("照片不属于该相册");
        }
        album.setCoverPhotoId(dto.photoId());
        albumMapper.updateById(album);
    }

    @Override
    public List<AlbumVO> listFamilyAlbums(Long familyId, Long userId) {
        // 校验用户是否是家庭成员
        FamilyMember member = familyMemberMapper.selectOne(
                new LambdaQueryWrapper<FamilyMember>()
                        .eq(FamilyMember::getFamilyId, familyId)
                        .eq(FamilyMember::getUserId, userId));
        if (member == null) {
            throw BusinessException.forbidden("您不是该家庭成员");
        }
        List<Album> albums = albumMapper.selectList(
                new LambdaQueryWrapper<Album>()
                        .eq(Album::getFamilyId, familyId)
                        .orderByDesc(Album::getUpdateTime));
        return albums.stream().map(this::toVO).toList();
    }
}
