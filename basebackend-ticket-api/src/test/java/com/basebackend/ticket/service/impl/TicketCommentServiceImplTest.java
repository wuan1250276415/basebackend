package com.basebackend.ticket.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.basebackend.ticket.dto.TicketCommentDTO;
import com.basebackend.ticket.entity.Ticket;
import com.basebackend.ticket.entity.TicketComment;
import com.basebackend.ticket.enums.CommentType;
import com.basebackend.ticket.mapper.TicketCommentMapper;
import com.basebackend.ticket.mapper.TicketMapper;
import com.basebackend.ticket.util.AuditHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

@ExtendWith(MockitoExtension.class)
@DisplayName("TicketCommentServiceImpl 工单评论服务测试")
class TicketCommentServiceImplTest {

    @Mock private TicketCommentMapper commentMapper;
    @Mock private TicketMapper ticketMapper;
    @Mock private AuditHelper auditHelper;

    @InjectMocks
    private TicketCommentServiceImpl commentService;

    private Ticket testTicket;

    @BeforeEach
    void setUp() {
        testTicket = new Ticket();
        testTicket.setId(1L);
        testTicket.setTenantId(1L);
        testTicket.setCommentCount(5);
    }

    @Nested
    @DisplayName("查询评论")
    class ListTests {

        @Test
        @DisplayName("listByTicketId - 应返回评论列表")
        void shouldReturnCommentList() {
            TicketComment c1 = new TicketComment();
            c1.setId(1L);
            c1.setContent("评论1");
            TicketComment c2 = new TicketComment();
            c2.setId(2L);
            c2.setContent("评论2");
            given(commentMapper.selectList(any())).willReturn(Arrays.asList(c1, c2));

            List<TicketComment> result = commentService.listByTicketId(1L);

            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("添加评论")
    class AddTests {

        @Test
        @DisplayName("add - 应创建评论并更新计数")
        void shouldAddCommentAndUpdateCount() {
            TicketCommentDTO dto = new TicketCommentDTO("新评论内容", null, null, null);
            given(ticketMapper.selectById(1L)).willReturn(testTicket);

            TicketComment result = commentService.add(1L, dto);

            assertThat(result.getContent()).isEqualTo("新评论内容");
            assertThat(result.getType()).isEqualTo(CommentType.COMMENT.name());
            assertThat(result.getIsInternal()).isZero();
            assertThat(result.getTenantId()).isEqualTo(1L);

            verify(commentMapper).insert(any(TicketComment.class));
            verify(auditHelper).setCreateAuditFields(any(TicketComment.class));

            assertThat(testTicket.getCommentCount()).isEqualTo(6);
            verify(ticketMapper).updateById(testTicket);
        }

        @Test
        @DisplayName("add - 应设置显式类型和内部标记")
        void shouldRespectExplicitTypeAndInternal() {
            TicketCommentDTO dto = new TicketCommentDTO("审批意见", CommentType.APPROVAL.name(), 1, 100L);
            given(ticketMapper.selectById(1L)).willReturn(testTicket);

            TicketComment result = commentService.add(1L, dto);

            assertThat(result.getType()).isEqualTo(CommentType.APPROVAL.name());
            assertThat(result.getIsInternal()).isEqualTo(1);
            assertThat(result.getParentId()).isEqualTo(100L);
        }
    }

    @Nested
    @DisplayName("删除评论")
    class DeleteTests {

        @Test
        @DisplayName("delete - 应删除评论并减少计数")
        void shouldDeleteAndDecrementCount() {
            TicketComment comment = new TicketComment();
            comment.setId(10L);
            comment.setTicketId(1L);
            given(commentMapper.selectById(10L)).willReturn(comment);
            given(commentMapper.selectCount(any())).willReturn(4L);
            given(ticketMapper.selectById(1L)).willReturn(testTicket);

            commentService.delete(1L, 10L);

            verify(commentMapper).deleteById(10L);
            assertThat(testTicket.getCommentCount()).isEqualTo(4);
            verify(ticketMapper).updateById(testTicket);
        }

        @Test
        @DisplayName("delete - 计数为0时不应变为负数")
        void shouldNotGoNegative() {
            TicketComment comment = new TicketComment();
            comment.setId(10L);
            comment.setTicketId(1L);
            given(commentMapper.selectById(10L)).willReturn(comment);
            given(commentMapper.selectCount(any())).willReturn(0L);
            given(ticketMapper.selectById(1L)).willReturn(testTicket);

            commentService.delete(1L, 10L);

            assertThat(testTicket.getCommentCount()).isZero();
        }

        @Test
        @DisplayName("delete - 评论不属于当前工单时应拒绝")
        void shouldRejectDeleteWhenCommentNotBelongTicket() {
            TicketComment comment = new TicketComment();
            comment.setId(10L);
            comment.setTicketId(2L);
            given(commentMapper.selectById(10L)).willReturn(comment);

            assertThatThrownBy(() -> commentService.delete(1L, 10L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("评论不存在或不属于该工单");

            verify(commentMapper, never()).deleteById(10L);
            verify(ticketMapper, never()).updateById(any(Ticket.class));
        }
    }

    @Nested
    @DisplayName("系统评论")
    class SystemCommentTests {

        @Test
        @DisplayName("addSystemComment - 应创建系统类型评论")
        void shouldAddSystemTypeComment() {
            given(ticketMapper.selectById(1L)).willReturn(testTicket);

            commentService.addSystemComment(1L, "状态已变更");

            ArgumentCaptor<TicketComment> captor = ArgumentCaptor.forClass(TicketComment.class);
            verify(commentMapper).insert(captor.capture());

            TicketComment inserted = captor.getValue();
            assertThat(inserted.getType()).isEqualTo(CommentType.SYSTEM.name());
            assertThat(inserted.getCreatorName()).isEqualTo("系统");
            assertThat(inserted.getContent()).isEqualTo("状态已变更");
        }
    }
}
