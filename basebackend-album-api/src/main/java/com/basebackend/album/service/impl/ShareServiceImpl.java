package com.basebackend.album.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.basebackend.album.dto.CreateShareDTO;
import com.basebackend.album.dto.ShareAccessDTO;
import com.basebackend.album.entity.Album;
import com.basebackend.album.entity.ShareLink;
import com.basebackend.album.mapper.AlbumMapper;
import com.basebackend.album.mapper.ShareLinkMapper;
import com.basebackend.album.service.ShareService;
import com.basebackend.album.vo.AlbumVO;
import com.basebackend.album.vo.ShareLinkVO;
import com.basebackend.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 分享服务实现
 *
 * @author BearTeam
 */
@Service
@RequiredArgsConstructor
public class ShareServiceImpl implements ShareService {

    private final ShareLinkMapper shareLinkMapper;
    private final AlbumMapper albumMapper;

    /**
     * 生成8位分享码
     */
    private String generateShareCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toLowerCase();
    }

    /**
     * 将实体转为 VO
     */
    private ShareLinkVO toVO(ShareLink link) {
        Album album = albumMapper.selectById(link.getAlbumId());
        String albumName = album != null ? album.getName() : null;
        String shareUrl = "/api/album/shares/view/" + link.getShareCode();
        return new ShareLinkVO(
                link.getId(), link.getAlbumId(), albumName,
                link.getShareCode(), shareUrl,
                link.getPassword() != null && !link.getPassword().isEmpty(),
                link.getExpireTime(), link.getMaxViews(),
                link.getViewCount(),
                link.getAllowDownload() != null && link.getAllowDownload() == 1,
                link.getStatus(), link.getCreateTime()
        );
    }

    @Override
    @Transactional
    public ShareLinkVO createShare(CreateShareDTO dto, Long userId) {
        // 校验相册是否存在
        Album album = albumMapper.selectById(dto.albumId());
        if (album == null) {
            throw BusinessException.notFound("相册不存在");
        }
        if (!album.getOwnerId().equals(userId)) {
            throw BusinessException.forbidden("只有相册创建者才能创建分享");
        }

        ShareLink link = new ShareLink();
        link.setAlbumId(dto.albumId());
        link.setCreatorId(userId);
        link.setShareCode(generateShareCode());
        link.setPassword(dto.password());
        link.setExpireTime(dto.expireTime());
        link.setMaxViews(dto.maxViews());
        link.setViewCount(0);
        link.setAllowDownload(dto.allowDownload() != null && dto.allowDownload() ? 1 : 0);
        link.setStatus(1); // 有效

        shareLinkMapper.insert(link);
        return toVO(link);
    }

    @Override
    public List<ShareLinkVO> myShares(Long userId) {
        List<ShareLink> links = shareLinkMapper.selectList(
                new LambdaQueryWrapper<ShareLink>()
                        .eq(ShareLink::getCreatorId, userId)
                        .orderByDesc(ShareLink::getCreateTime));
        return links.stream().map(this::toVO).toList();
    }

    @Override
    @Transactional
    public void deleteShare(Long shareId, Long userId) {
        ShareLink link = shareLinkMapper.selectById(shareId);
        if (link == null) {
            throw BusinessException.notFound("分享链接不存在");
        }
        if (!link.getCreatorId().equals(userId)) {
            throw BusinessException.forbidden("只有创建者才能取消分享");
        }
        // 设置为失效
        link.setStatus(0);
        shareLinkMapper.updateById(link);
    }

    @Override
    @Transactional
    public AlbumVO accessShare(String shareCode, ShareAccessDTO dto) {
        ShareLink link = shareLinkMapper.selectOne(
                new LambdaQueryWrapper<ShareLink>()
                        .eq(ShareLink::getShareCode, shareCode));
        if (link == null) {
            throw BusinessException.notFound("分享链接不存在");
        }
        // 校验状态
        if (link.getStatus() == 0) {
            throw BusinessException.paramError("分享链接已失效");
        }
        // 校验过期时间
        if (link.getExpireTime() != null && link.getExpireTime().isBefore(LocalDateTime.now())) {
            link.setStatus(0);
            shareLinkMapper.updateById(link);
            throw BusinessException.paramError("分享链接已过期");
        }
        // 校验访问次数
        if (link.getMaxViews() != null && link.getViewCount() >= link.getMaxViews()) {
            link.setStatus(0);
            shareLinkMapper.updateById(link);
            throw BusinessException.paramError("分享链接已达最大查看次数");
        }
        // 校验密码
        if (link.getPassword() != null && !link.getPassword().isEmpty()) {
            if (dto == null || dto.password() == null || !link.getPassword().equals(dto.password())) {
                throw BusinessException.paramError("访问密码错误");
            }
        }

        // 更新访问计数
        link.setViewCount(link.getViewCount() + 1);
        shareLinkMapper.updateById(link);

        // 返回相册信息
        Album album = albumMapper.selectById(link.getAlbumId());
        if (album == null) {
            throw BusinessException.notFound("相册不存在");
        }
        return new AlbumVO(
                album.getId(), album.getName(), album.getDescription(),
                null, album.getFamilyId(), null,
                album.getOwnerId(), null,
                album.getType(), album.getVisibility(),
                album.getPhotoCount(),
                album.getCreateTime(), album.getUpdateTime()
        );
    }
}
