package com.basebackend.ticket.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.basebackend.common.event.DomainEventPublisher;
import com.basebackend.ticket.dto.TicketCreateDTO;
import com.basebackend.ticket.entity.Ticket;
import com.basebackend.ticket.entity.TicketCategory;
import com.basebackend.ticket.entity.TicketStatusLog;
import com.basebackend.ticket.enums.TicketSource;
import com.basebackend.ticket.enums.TicketStatus;
import com.basebackend.ticket.event.TicketAssignedEvent;
import com.basebackend.ticket.event.TicketCreatedEvent;
import com.basebackend.ticket.event.TicketStatusChangedEvent;
import com.basebackend.ticket.mapper.*;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TicketServiceImpl 工单服务测试")
class TicketServiceImplTest {

    @Mock private TicketMapper ticketMapper;
    @Mock private TicketCategoryMapper categoryMapper;
    @Mock private TicketCommentMapper commentMapper;
    @Mock private TicketAttachmentMapper attachmentMapper;
    @Mock private TicketStatusLogMapper statusLogMapper;
    @Mock private TicketApprovalMapper approvalMapper;
    @Mock private TicketCcMapper ccMapper;
    @Mock private AuditHelper auditHelper;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private DomainEventPublisher eventPublisher;

    @InjectMocks
    private TicketServiceImpl ticketService;

    private Ticket testTicket;

    @BeforeEach
    void setUp() {
        testTicket = new Ticket();
        testTicket.setId(1L);
        testTicket.setTicketNo("TK-20260301-0001");
        testTicket.setTitle("测试工单");
        testTicket.setDescription("测试描述");
        testTicket.setCategoryId(10L);
        testTicket.setStatus(TicketStatus.OPEN.name());
        testTicket.setPriority(3);
        testTicket.setSource(TicketSource.WEB.name());
        testTicket.setReporterId(100L);
        testTicket.setReporterName("张三");
        testTicket.setTenantId(1L);
        testTicket.setCommentCount(0);
        testTicket.setAttachmentCount(0);
        testTicket.setSlaBreached(0);
        testTicket.setCreateTime(LocalDateTime.now());

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
    }

    @Nested
    @DisplayName("创建工单")
    class CreateTests {

        @Test
        @DisplayName("创建工单 - 应设置默认值并发布事件")
        void shouldSetDefaultsAndPublishEvent() {
            // Given
            TicketCreateDTO dto = new TicketCreateDTO(
                    "新工单", "描述内容", 10L, null, null,
                    null, null, null, null, null, null, null
            );
            TicketCategory category = new TicketCategory();
            category.setSlaHours(24);
            given(categoryMapper.selectById(10L)).willReturn(category);
            given(valueOperations.increment(any(String.class))).willReturn(1L);

            // When
            Ticket result = ticketService.create(dto);

            // Then
            assertThat(result.getStatus()).isEqualTo(TicketStatus.OPEN.name());
            assertThat(result.getSource()).isEqualTo(TicketSource.WEB.name());
            assertThat(result.getPriority()).isEqualTo(3);
            assertThat(result.getCommentCount()).isZero();
            assertThat(result.getAttachmentCount()).isZero();
            assertThat(result.getSlaDeadline()).isNotNull();

            verify(ticketMapper).insert(any(Ticket.class));
            verify(auditHelper).setCreateAuditFields(any(Ticket.class));
            verify(eventPublisher).publish(any(TicketCreatedEvent.class));
        }

        @Test
        @DisplayName("创建工单 - 无分类时不设置SLA截止时间")
        void shouldNotSetSlaWhenNoCategoryFound() {
            // Given
            TicketCreateDTO dto = new TicketCreateDTO(
                    "无分类工单", "描述", 99L, null, null,
                    null, null, null, null, null, null, null
            );
            given(categoryMapper.selectById(99L)).willReturn(null);
            given(valueOperations.increment(any(String.class))).willReturn(2L);

            // When
            Ticket result = ticketService.create(dto);

            // Then
            assertThat(result.getSlaDeadline()).isNull();
            verify(ticketMapper).insert(any(Ticket.class));
        }
    }

    @Nested
    @DisplayName("查询工单")
    class QueryTests {

        @Test
        @DisplayName("根据ID查询 - 存在时应返回工单")
        void shouldReturnTicketWhenExists() {
            given(ticketMapper.selectById(1L)).willReturn(testTicket);

            Ticket result = ticketService.getById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getTicketNo()).isEqualTo("TK-20260301-0001");
        }

        @Test
        @DisplayName("根据ID查询 - 不存在时应抛出异常")
        void shouldThrowWhenNotFound() {
            given(ticketMapper.selectById(999L)).willReturn(null);

            assertThatThrownBy(() -> ticketService.getById(999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("工单不存在");
        }
    }

    @Nested
    @DisplayName("状态变更")
    class StatusChangeTests {

        @Test
        @DisplayName("合法状态变更 - 应更新状态并记录日志")
        void shouldChangeStatusAndLogWhenValid() {
            // Given
            testTicket.setStatus(TicketStatus.OPEN.name());
            given(ticketMapper.selectById(1L)).willReturn(testTicket);

            // When
            ticketService.changeStatus(1L, TicketStatus.IN_PROGRESS, "开始处理");

            // Then
            assertThat(testTicket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS.name());
            verify(ticketMapper).updateById(testTicket);
            verify(statusLogMapper).insert(any(TicketStatusLog.class));
            verify(eventPublisher).publish(any(TicketStatusChangedEvent.class));
        }

        @Test
        @DisplayName("合法状态变更 - RESOLVED 应设置解决时间")
        void shouldSetResolvedAtWhenResolved() {
            testTicket.setStatus(TicketStatus.IN_PROGRESS.name());
            given(ticketMapper.selectById(1L)).willReturn(testTicket);

            ticketService.changeStatus(1L, TicketStatus.RESOLVED, "已解决");

            assertThat(testTicket.getResolvedAt()).isNotNull();
        }

        @Test
        @DisplayName("合法状态变更 - CLOSED 应设置关闭时间")
        void shouldSetClosedAtWhenClosed() {
            testTicket.setStatus(TicketStatus.RESOLVED.name());
            given(ticketMapper.selectById(1L)).willReturn(testTicket);

            ticketService.changeStatus(1L, TicketStatus.CLOSED, "关闭");

            assertThat(testTicket.getClosedAt()).isNotNull();
        }

        @Test
        @DisplayName("非法状态变更 - 应抛出异常")
        void shouldThrowWhenInvalidTransition() {
            testTicket.setStatus(TicketStatus.CLOSED.name());
            given(ticketMapper.selectById(1L)).willReturn(testTicket);

            assertThatThrownBy(() -> ticketService.changeStatus(1L, TicketStatus.OPEN, "重新打开"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("非法状态变更");
        }

        @Test
        @DisplayName("SLA 超时 - 应标记违约")
        void shouldMarkSlaBreachedWhenOverdue() {
            testTicket.setStatus(TicketStatus.IN_PROGRESS.name());
            testTicket.setSlaDeadline(LocalDateTime.now().minusHours(1));
            given(ticketMapper.selectById(1L)).willReturn(testTicket);

            ticketService.changeStatus(1L, TicketStatus.RESOLVED, "迟到解决");

            assertThat(testTicket.getSlaBreached()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("分配工单")
    class AssignTests {

        @Test
        @DisplayName("分配处理人 - 应更新处理人并发布事件")
        void shouldAssignAndPublishEvent() {
            given(ticketMapper.selectById(1L)).willReturn(testTicket);

            ticketService.assign(1L, 200L, "李四");

            assertThat(testTicket.getAssigneeId()).isEqualTo(200L);
            assertThat(testTicket.getAssigneeName()).isEqualTo("李四");
            verify(ticketMapper).updateById(testTicket);
            verify(eventPublisher).publish(any(TicketAssignedEvent.class));
        }

        @Test
        @DisplayName("分配处理人 - OPEN 状态应自动变更为 IN_PROGRESS")
        void shouldTransitionToInProgressWhenOpen() {
            testTicket.setStatus(TicketStatus.OPEN.name());
            given(ticketMapper.selectById(1L)).willReturn(testTicket);

            ticketService.assign(1L, 200L, "李四");

            assertThat(testTicket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS.name());
            verify(statusLogMapper).insert(any(TicketStatusLog.class));
        }

        @Test
        @DisplayName("分配处理人 - 非 OPEN 状态不应变更状态")
        void shouldNotTransitionWhenNotOpen() {
            testTicket.setStatus(TicketStatus.IN_PROGRESS.name());
            given(ticketMapper.selectById(1L)).willReturn(testTicket);

            ticketService.assign(1L, 200L, "李四");

            assertThat(testTicket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS.name());
            verify(statusLogMapper, never()).insert(any(TicketStatusLog.class));
        }
    }

    @Nested
    @DisplayName("工单号生成")
    class TicketNoTests {

        @Test
        @DisplayName("生成工单号 - 格式应为 TK-yyyyMMdd-NNNN")
        void shouldGenerateCorrectFormat() {
            given(valueOperations.increment(any(String.class))).willReturn(42L);

            String ticketNo = ticketService.generateTicketNo();

            assertThat(ticketNo).startsWith("TK-");
            assertThat(ticketNo).matches("TK-\\d{8}-\\d{4}");
            assertThat(ticketNo).endsWith("-0042");
        }

        @Test
        @DisplayName("生成工单号 - 首次生成应设置过期时间")
        void shouldSetExpiryOnFirstGeneration() {
            given(valueOperations.increment(any(String.class))).willReturn(1L);

            ticketService.generateTicketNo();

            verify(redisTemplate).expire(any(String.class), eq(2L), any());
        }
    }

    @Nested
    @DisplayName("删除工单")
    class DeleteTests {

        @Test
        @DisplayName("删除工单 - 应调用 mapper 删除")
        void shouldDeleteTicket() {
            given(ticketMapper.selectById(1L)).willReturn(testTicket);

            ticketService.delete(1L);

            verify(ticketMapper).deleteById(1L);
        }
    }
}
