package com.basebackend.album.service;

import com.basebackend.album.dto.CreateCommentDTO;
import com.basebackend.album.vo.CommentVO;

import java.util.List;

/**
 * 评论服务接口
 *
 * @author BearTeam
 */
public interface CommentService {

    /** 发表评论 */
    CommentVO addComment(CreateCommentDTO dto, Long userId);

    /** 照片评论列表 */
    List<CommentVO> listComments(Long photoId);

    /** 删除评论 */
    void deleteComment(Long commentId, Long userId);
}
