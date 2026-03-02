package com.basebackend.ticket.ai;

import com.basebackend.ai.client.AiClient;
import com.basebackend.ai.client.AiRequest;
import com.basebackend.ai.client.AiResponse;
import com.basebackend.ai.client.AiUsage;
import com.basebackend.ticket.entity.Ticket;
import com.basebackend.ticket.entity.TicketCategory;
import com.basebackend.ticket.entity.TicketComment;
import com.basebackend.ticket.mapper.TicketCategoryMapper;
import com.basebackend.ticket.mapper.TicketCommentMapper;
import com.basebackend.ticket.mapper.TicketMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketAiServiceImplTest {

    @InjectMocks
    private TicketAiServiceImpl aiService;

    @Mock
    private AiClient aiClient;

    @Mock
    private TicketMapper ticketMapper;

    @Mock
    private TicketCategoryMapper categoryMapper;

    @Mock
    private TicketCommentMapper commentMapper;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    private AiResponse mockResponse(String content) {
        return AiResponse.of(content, "test-model", AiUsage.empty(), "stop", 100);
    }

    @Nested
    @DisplayName("classifyTicket")
    class ClassifyTests {
        @Test
        @DisplayName("应返回正确的分类结果")
        void shouldClassifyTicket() {
            TicketCategory cat = new TicketCategory();
            cat.setId(1L);
            cat.setName("技术支持");
            cat.setStatus(1);
            when(categoryMapper.selectList(any())).thenReturn(List.of(cat));
            when(aiClient.chat(any(AiRequest.class)))
                    .thenReturn(mockResponse("{\"categoryId\":1,\"categoryName\":\"技术支持\",\"confidence\":0.9,\"reasoning\":\"匹配技术问题\"}"));

            var result = aiService.classifyTicket("网络故障", "公司WiFi无法连接");

            assertThat(result).isNotNull();
            assertThat(result.categoryId()).isEqualTo(1L);
            assertThat(result.confidence()).isEqualTo(0.9);
        }

        @Test
        @DisplayName("AI返回无效JSON时应降级处理")
        void shouldHandleInvalidJson() {
            TicketCategory cat = new TicketCategory();
            cat.setId(1L);
            cat.setName("默认");
            cat.setStatus(1);
            when(categoryMapper.selectList(any())).thenReturn(List.of(cat));
            when(aiClient.chat(any(AiRequest.class)))
                    .thenReturn(mockResponse("这不是JSON"));

            var result = aiService.classifyTicket("测试", "描述");

            assertThat(result).isNotNull();
            assertThat(result.confidence()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("无可用分类时应返回空结果")
        void shouldHandleNoCategories() {
            when(categoryMapper.selectList(any())).thenReturn(Collections.emptyList());
            when(aiClient.chat(any(AiRequest.class)))
                    .thenReturn(mockResponse("{\"categoryId\":99,\"categoryName\":\"不存在\",\"confidence\":0.5}"));

            var result = aiService.classifyTicket("测试", null);

            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("summarizeTicket")
    class SummarizeTests {
        @Test
        @DisplayName("应生成工单摘要")
        void shouldSummarizeTicket() {
            Ticket ticket = new Ticket();
            ticket.setId(1L);
            ticket.setTicketNo("TK-001");
            ticket.setTitle("测试工单");
            ticket.setStatus("IN_PROGRESS");
            ticket.setDescription("详细描述");
            when(ticketMapper.selectById(1L)).thenReturn(ticket);
            when(commentMapper.selectList(any())).thenReturn(Collections.emptyList());
            when(aiClient.chat(any(AiRequest.class)))
                    .thenReturn(mockResponse("这是一个技术支持工单，报告网络问题。"));

            String summary = aiService.summarizeTicket(1L);

            assertThat(summary).isNotBlank();
        }

        @Test
        @DisplayName("工单不存在时应抛出异常")
        void shouldThrowWhenTicketNotFound() {
            when(ticketMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> aiService.summarizeTicket(999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("工单不存在");
        }
    }

    @Nested
    @DisplayName("suggestReply")
    class SuggestReplyTests {
        @Test
        @DisplayName("应返回推荐回复列表")
        void shouldReturnReplySuggestions() {
            Ticket ticket = new Ticket();
            ticket.setId(1L);
            ticket.setTitle("测试");
            ticket.setStatus("OPEN");
            when(ticketMapper.selectById(1L)).thenReturn(ticket);
            when(commentMapper.selectList(any())).thenReturn(Collections.emptyList());
            when(aiClient.chat(any(AiRequest.class)))
                    .thenReturn(mockResponse("[\"回复1\",\"回复2\",\"回复3\"]"));

            List<String> replies = aiService.suggestReply(1L);

            assertThat(replies).hasSize(3);
        }
    }
}
