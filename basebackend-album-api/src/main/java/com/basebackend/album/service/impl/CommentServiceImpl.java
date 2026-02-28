package com.basebackend.album.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.basebackend.album.dto.CreateCommentDTO;
import com.basebackend.album.entity.Photo;
import com.basebackend.album.entity.PhotoComment;
import com.basebackend.album.mapper.PhotoCommentMapper;
import com.basebackend.album.mapper.PhotoMapper;
import com.basebackend.album.service.CommentService;
import com.basebackend.album.vo.CommentVO;
import com.basebackend.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 评论服务实现
 *
 * @author BearTeam
 */
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final PhotoCommentMapper commentMapper;
    private final PhotoMapper photoMapper;

    @Override
    @Transactional
    public CommentVO addComment(CreateCommentDTO dto, Long userId) {
        // 校验照片是否存在
        Photo photo = photoMapper.selectById(dto.photoId());
        if (photo == null) {
            throw BusinessException.notFound("照片不存在");
        }

        PhotoComment comment = new PhotoComment();
        comment.setPhotoId(dto.photoId());
        comment.setUserId(userId);
        comment.setContent(dto.content());
        comment.setParentId(dto.parentId());
        commentMapper.insert(comment);

        // 更新照片评论计数
        photo.setCommentCount(photo.getCommentCount() + 1);
        photoMapper.updateById(photo);

        return new CommentVO(
                comment.getId(), comment.getPhotoId(),
                comment.getUserId(), null,
                comment.getContent(), comment.getParentId(),
                comment.getCreateTime()
        );
    }

    @Override
    public List<CommentVO> listComments(Long photoId) {
        List<PhotoComment> comments = commentMapper.selectList(
                new LambdaQueryWrapper<PhotoComment>()
                        .eq(PhotoComment::getPhotoId, photoId)
                        .orderByAsc(PhotoComment::getCreateTime));
        return comments.stream().map(c -> new CommentVO(
                c.getId(), c.getPhotoId(),
                c.getUserId(), null,
                c.getContent(), c.getParentId(),
                c.getCreateTime()
        )).toList();
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        PhotoComment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw BusinessException.notFound("评论不存在");
        }
        // 权限校验：评论者本人或照片所有者
        Photo photo = photoMapper.selectById(comment.getPhotoId());
        if (!comment.getUserId().equals(userId)
                && (photo == null || !photo.getOwnerId().equals(userId))) {
            throw BusinessException.forbidden("无权删除此评论");
        }
        commentMapper.deleteById(commentId);

        // 更新照片评论计数
        if (photo != null && photo.getCommentCount() > 0) {
            photo.setCommentCount(photo.getCommentCount() - 1);
            photoMapper.updateById(photo);
        }
    }
}
