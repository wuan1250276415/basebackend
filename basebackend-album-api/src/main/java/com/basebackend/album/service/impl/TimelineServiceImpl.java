package com.basebackend.album.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.basebackend.album.entity.FamilyMember;
import com.basebackend.album.entity.Photo;
import com.basebackend.album.entity.Album;
import com.basebackend.album.entity.PhotoLike;
import com.basebackend.album.mapper.AlbumMapper;
import com.basebackend.album.mapper.FamilyMemberMapper;
import com.basebackend.album.mapper.PhotoLikeMapper;
import com.basebackend.album.mapper.PhotoMapper;
import com.basebackend.album.service.TimelineService;
import com.basebackend.album.vo.PhotoVO;
import com.basebackend.album.vo.TimelineVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 时间轴服务实现
 *
 * @author BearTeam
 */
@Service
@RequiredArgsConstructor
public class TimelineServiceImpl implements TimelineService {

    private final PhotoMapper photoMapper;
    private final AlbumMapper albumMapper;
    private final FamilyMemberMapper familyMemberMapper;
    private final PhotoLikeMapper photoLikeMapper;

    @Override
    public List<TimelineVO> getTimeline(Long userId, int page, int size) {
        // 查询用户所有可见的相册ID（个人 + 所属家庭）
        List<Long> familyIds = familyMemberMapper.selectList(
                new LambdaQueryWrapper<FamilyMember>()
                        .eq(FamilyMember::getUserId, userId))
                .stream().map(FamilyMember::getFamilyId).toList();

        LambdaQueryWrapper<Album> albumWrapper = new LambdaQueryWrapper<Album>()
                .and(w -> {
                    w.eq(Album::getOwnerId, userId);
                    if (!familyIds.isEmpty()) {
                        w.or().in(Album::getFamilyId, familyIds);
                    }
                });
        List<Long> albumIds = albumMapper.selectList(albumWrapper)
                .stream().map(Album::getId).toList();

        if (albumIds.isEmpty()) {
            return List.of();
        }

        // 查询所有照片，按 taken_at 或 create_time 排序
        List<Photo> photos = photoMapper.selectList(
                new LambdaQueryWrapper<Photo>()
                        .in(Photo::getAlbumId, albumIds)
                        .orderByDesc(Photo::getTakenAt)
                        .orderByDesc(Photo::getCreateTime));

        // 按日期分组
        Map<LocalDate, List<Photo>> grouped = photos.stream()
                .collect(Collectors.groupingBy(
                        p -> {
                            LocalDateTime dt = p.getTakenAt() != null ? p.getTakenAt() : p.getCreateTime();
                            return dt != null ? dt.toLocalDate() : LocalDate.now();
                        },
                        LinkedHashMap::new,
                        Collectors.toList()));

        // 分页：按日期分组数分页
        List<Map.Entry<LocalDate, List<Photo>>> entries = new ArrayList<>(grouped.entrySet());
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, entries.size());
        if (fromIndex >= entries.size()) {
            return List.of();
        }

        List<Map.Entry<LocalDate, List<Photo>>> pageEntries = entries.subList(fromIndex, toIndex);

        return pageEntries.stream().map(entry -> {
            List<PhotoVO> photoVOs = entry.getValue().stream()
                    .map(p -> toVO(p, userId)).toList();
            return new TimelineVO(entry.getKey(), photoVOs.size(), photoVOs);
        }).toList();
    }

    /**
     * 将照片实体转为 VO
     */
    private PhotoVO toVO(Photo photo, Long userId) {
        Boolean liked = false;
        if (userId != null) {
            Long likeCount = photoLikeMapper.selectCount(
                    new LambdaQueryWrapper<PhotoLike>()
                            .eq(PhotoLike::getPhotoId, photo.getId())
                            .eq(PhotoLike::getUserId, userId));
            liked = likeCount > 0;
        }
        return new PhotoVO(
                photo.getId(), photo.getAlbumId(),
                photo.getFileName(), photo.getFilePath(),
                photo.getThumbnailPath(), photo.getFileSize(),
                photo.getMimeType(), photo.getWidth(), photo.getHeight(),
                photo.getMediaType(), photo.getDuration(),
                photo.getTakenAt(), photo.getLocation(),
                photo.getLatitude(), photo.getLongitude(),
                photo.getDescription(), photo.getTags(),
                photo.getLikeCount(), photo.getCommentCount(),
                liked, null, photo.getCreateTime()
        );
    }
}
