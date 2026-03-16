package com.basebackend.ticket.search;

import com.basebackend.common.context.TenantContextHolder;
import com.basebackend.search.client.SearchClient;
import com.basebackend.search.model.SearchResult;
import com.basebackend.search.query.SearchQuery;
import com.basebackend.ticket.dto.TicketQueryDTO;
import com.basebackend.ticket.entity.Ticket;
import com.basebackend.ticket.entity.TicketCategory;
import com.basebackend.ticket.mapper.TicketCategoryMapper;
import com.basebackend.ticket.mapper.TicketMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketSearchServiceImplTest {

    @InjectMocks
    private TicketSearchServiceImpl searchService;

    @Mock
    private SearchClient searchClient;

    @Mock
    private TicketMapper ticketMapper;

    @Mock
    private TicketCategoryMapper categoryMapper;

    @AfterEach
    void cleanupContext() {
        TenantContextHolder.clear();
    }

    private Ticket buildTicket(Long id, String ticketNo, String title) {
        Ticket t = new Ticket();
        t.setId(id);
        t.setTicketNo(ticketNo);
        t.setTitle(title);
        t.setDescription("test description");
        t.setStatus("OPEN");
        t.setPriority(3);
        t.setCategoryId(1L);
        t.setReporterName("reporter");
        t.setAssigneeName("assignee");
        t.setCreateTime(LocalDateTime.now());
        return t;
    }

    @Nested
    @DisplayName("indexTicket")
    class IndexTests {
        @Test
        @DisplayName("应成功索引工单文档")
        void shouldIndexTicketDocument() {
            Ticket ticket = buildTicket(1L, "TK-001", "测试工单");
            ticket.setAssigneeId(321L);
            ticket.setTenantId(9L);
            TicketCategory cat = new TicketCategory();
            cat.setId(1L);
            cat.setName("技术支持");
            when(categoryMapper.selectById(1L)).thenReturn(cat);
            when(searchClient.index(anyString(), anyString(), any())).thenReturn(true);

            searchService.indexTicket(ticket);

            ArgumentCaptor<TicketSearchDocument> captor = ArgumentCaptor.forClass(TicketSearchDocument.class);
            verify(searchClient).index(eq("ticket"), eq("1"), captor.capture());
            assertThat(captor.getValue().getAssigneeId()).isEqualTo(321L);
            assertThat(captor.getValue().getTenantId()).isEqualTo(9L);
        }

        @Test
        @DisplayName("分类不存在时也应正常索引")
        void shouldIndexWithNullCategory() {
            Ticket ticket = buildTicket(2L, "TK-002", "无分类工单");
            ticket.setCategoryId(999L);
            when(categoryMapper.selectById(999L)).thenReturn(null);
            when(searchClient.index(anyString(), anyString(), any())).thenReturn(true);

            searchService.indexTicket(ticket);

            verify(searchClient).index(eq("ticket"), eq("2"), any(TicketSearchDocument.class));
        }
    }

    @Nested
    @DisplayName("removeTicket")
    class RemoveTests {
        @Test
        @DisplayName("应成功删除工单索引")
        void shouldRemoveTicketIndex() {
            when(searchClient.delete("ticket", "1")).thenReturn(true);

            searchService.removeTicket(1L);

            verify(searchClient).delete("ticket", "1");
        }
    }

    @Nested
    @DisplayName("search")
    class SearchTests {
        @Test
        @DisplayName("应根据关键词搜索工单")
        void shouldSearchByKeyword() {
            SearchResult<TicketSearchDocument> mockResult = SearchResult.empty();
            when(searchClient.search(any(SearchQuery.class), eq(TicketSearchDocument.class)))
                    .thenReturn(mockResult);

            SearchResult<TicketSearchDocument> result = searchService.search("测试", null, 1, 10);

            assertThat(result).isNotNull();
            verify(searchClient).search(any(SearchQuery.class), eq(TicketSearchDocument.class));
        }

        @Test
        @DisplayName("应支持带过滤条件的搜索")
        void shouldSearchWithFilters() {
            TicketQueryDTO filters = new TicketQueryDTO();
            filters.setStatus("OPEN");
            filters.setPriority(1);
            filters.setAssigneeId(123L);
            TenantContextHolder.set(() -> 10L);

            SearchResult<TicketSearchDocument> mockResult = SearchResult.empty();
            when(searchClient.search(any(SearchQuery.class), eq(TicketSearchDocument.class)))
                    .thenReturn(mockResult);

            SearchResult<TicketSearchDocument> result = searchService.search("关键词", filters, 1, 20);

            assertThat(result).isNotNull();
            ArgumentCaptor<SearchQuery> captor = ArgumentCaptor.forClass(SearchQuery.class);
            verify(searchClient).search(captor.capture(), eq(TicketSearchDocument.class));
            SearchQuery query = captor.getValue();
            assertThat(query.getFilterConditions())
                    .anySatisfy(c -> {
                        assertThat(c.field()).isEqualTo("assigneeId");
                        assertThat(c.value()).isEqualTo(123L);
                    })
                    .anySatisfy(c -> {
                        assertThat(c.field()).isEqualTo("tenantId");
                        assertThat(c.value()).isEqualTo(10L);
                    });
        }
    }

    @Nested
    @DisplayName("reindexAll")
    class ReindexTests {
        @Test
        @DisplayName("应全量重建索引")
        void shouldReindexAll() {
            Ticket t1 = buildTicket(1L, "TK-001", "工单1");
            Ticket t2 = buildTicket(2L, "TK-002", "工单2");
            when(ticketMapper.selectList(any())).thenReturn(List.of(t1, t2));
            TicketCategory cat = new TicketCategory();
            cat.setId(1L);
            cat.setName("分类");
            when(categoryMapper.selectById(anyLong())).thenReturn(cat);
            when(searchClient.index(anyString(), anyString(), any())).thenReturn(true);

            searchService.reindexAll();

            verify(searchClient, times(2)).index(eq("ticket"), anyString(), any());
        }

        @Test
        @DisplayName("空数据时不应调用索引")
        void shouldHandleEmptyData() {
            when(ticketMapper.selectList(any())).thenReturn(Collections.emptyList());

            searchService.reindexAll();

            verify(searchClient, never()).index(anyString(), anyString(), any());
        }
    }
}
