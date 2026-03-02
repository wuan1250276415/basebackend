package com.basebackend.ticket.service.impl;

import com.basebackend.ticket.entity.Ticket;
import com.basebackend.ticket.entity.TicketAttachment;
import com.basebackend.ticket.mapper.TicketAttachmentMapper;
import com.basebackend.ticket.mapper.TicketMapper;
import com.basebackend.ticket.util.AuditHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("TicketAttachmentServiceImpl 附件服务测试")
class TicketAttachmentServiceImplTest {

    @Mock
    private TicketAttachmentMapper attachmentMapper;
    @Mock
    private TicketMapper ticketMapper;
    @Mock
    private AuditHelper auditHelper;

    @InjectMocks
    private TicketAttachmentServiceImpl attachmentService;

    @Test
    @DisplayName("delete - 附件不属于当前工单时应拒绝")
    void shouldRejectDeleteWhenAttachmentNotBelongTicket() {
        TicketAttachment attachment = new TicketAttachment();
        attachment.setId(20L);
        attachment.setTicketId(2L);
        given(attachmentMapper.selectById(20L)).willReturn(attachment);

        assertThatThrownBy(() -> attachmentService.delete(1L, 20L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("附件不存在或不属于该工单");

        verify(attachmentMapper, never()).deleteById(20L);
        verify(ticketMapper, never()).updateById(any(Ticket.class));
    }
}

