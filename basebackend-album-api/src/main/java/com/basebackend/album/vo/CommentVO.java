package com.basebackend.album.vo;

import java.time.LocalDateTime;

/**
 * 评论响应 VO
 *
 * @param id         评论ID
 * @param photoId    照片ID
 * @param userId     评论者ID
 * @param userName   评论者名称
 * @param content    评论内容
 * @param parentId   父评论ID
 * @param createTime 评论时间
 * @author BearTeam
 */
public record CommentVO(
        Long id,
        Long photoId,
        Long userId,
        String userName,
        String content,
        Long parentId,
        LocalDateTime createTime
) {
}
