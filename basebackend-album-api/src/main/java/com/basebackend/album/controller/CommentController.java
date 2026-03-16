package com.basebackend.album.controller;

import com.basebackend.album.dto.CreateCommentDTO;
import com.basebackend.album.service.CommentService;
import com.basebackend.album.vo.CommentVO;
import com.basebackend.common.context.UserContextHolder;
import com.basebackend.common.model.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评论控制器
 *
 * @author BearTeam
 */
@RestController
@RequestMapping("/api/album/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /** 发表评论 */
    @PostMapping
    public Result<CommentVO> addComment(@Valid @RequestBody CreateCommentDTO dto) {
        Long userId = UserContextHolder.requireUserId();
        return Result.success(commentService.addComment(dto, userId));
    }

    /** 照片评论列表 */
    @GetMapping("/photo/{photoId}")
    public Result<List<CommentVO>> listComments(@PathVariable Long photoId) {
        return Result.success(commentService.listComments(photoId));
    }

    /** 删除评论 */
    @DeleteMapping("/{id}")
    public Result<Void> deleteComment(@PathVariable Long id) {
        Long userId = UserContextHolder.requireUserId();
        commentService.deleteComment(id, userId);
        return Result.success();
    }
}
