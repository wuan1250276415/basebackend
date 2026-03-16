package com.basebackend.ticket.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.basebackend.ticket.dto.TicketCategoryDTO;
import com.basebackend.ticket.entity.TicketCategory;
import com.basebackend.ticket.mapper.TicketCategoryMapper;
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
import java.util.Collections;

@ExtendWith(MockitoExtension.class)
@DisplayName("TicketCategoryServiceImpl 工单分类服务测试")
class TicketCategoryServiceImplTest {

    @Mock private TicketCategoryMapper categoryMapper;
    @Mock private AuditHelper auditHelper;

    @InjectMocks
    private TicketCategoryServiceImpl categoryService;

    private TicketCategory rootCategory;
    private TicketCategory childCategory;

    @BeforeEach
    void setUp() {
        rootCategory = new TicketCategory();
        rootCategory.setId(1L);
        rootCategory.setName("技术支持");
        rootCategory.setParentId(0L);
        rootCategory.setSortOrder(1);
        rootCategory.setSlaHours(24);
        rootCategory.setStatus(1);

        childCategory = new TicketCategory();
        childCategory.setId(2L);
        childCategory.setName("网络故障");
        childCategory.setParentId(1L);
        childCategory.setSortOrder(1);
        childCategory.setSlaHours(8);
        childCategory.setStatus(1);
    }

    @Nested
    @DisplayName("分类树")
    class TreeTests {

        @Test
        @DisplayName("tree - 应返回树形结构")
        void shouldReturnTreeStructure() {
            given(categoryMapper.selectList(any())).willReturn(Arrays.asList(rootCategory, childCategory));

            var tree = categoryService.tree();

            assertThat(tree).hasSize(1);
            assertThat(tree.getFirst().getName()).isEqualTo("技术支持");
            assertThat(tree.getFirst().getChildren()).hasSize(1);
            assertThat(tree.getFirst().getChildren().getFirst().getName()).isEqualTo("网络故障");
        }

        @Test
        @DisplayName("tree - 空列表应返回空树")
        void shouldReturnEmptyTreeWhenNoCategories() {
            given(categoryMapper.selectList(any())).willReturn(Collections.emptyList());

            var tree = categoryService.tree();

            assertThat(tree).isEmpty();
        }
    }

    @Nested
    @DisplayName("查询分类")
    class QueryTests {

        @Test
        @DisplayName("getById - 存在时应返回分类")
        void shouldReturnCategoryWhenExists() {
            given(categoryMapper.selectById(1L)).willReturn(rootCategory);

            TicketCategory result = categoryService.getById(1L);

            assertThat(result.getName()).isEqualTo("技术支持");
        }

        @Test
        @DisplayName("getById - 不存在时应抛出异常")
        void shouldThrowWhenNotFound() {
            given(categoryMapper.selectById(999L)).willReturn(null);

            assertThatThrownBy(() -> categoryService.getById(999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("工单分类不存在");
        }
    }

    @Nested
    @DisplayName("创建分类")
    class CreateTests {

        @Test
        @DisplayName("create - 应设置默认值并插入")
        void shouldSetDefaultsAndInsert() {
            TicketCategoryDTO dto = new TicketCategoryDTO(
                    "新分类", null, null, null, null, null, null
            );

            categoryService.create(dto);

            ArgumentCaptor<TicketCategory> captor = ArgumentCaptor.forClass(TicketCategory.class);
            verify(categoryMapper).insert(captor.capture());

            TicketCategory inserted = captor.getValue();
            assertThat(inserted.getParentId()).isEqualTo(0L);
            assertThat(inserted.getSortOrder()).isZero();
            assertThat(inserted.getSlaHours()).isEqualTo(24);
            assertThat(inserted.getStatus()).isEqualTo(1);
            verify(auditHelper).setCreateAuditFields(any(TicketCategory.class));
        }
    }

    @Nested
    @DisplayName("删除分类")
    class DeleteTests {

        @Test
        @DisplayName("delete - 无子分类时应成功删除")
        void shouldDeleteWhenNoChildren() {
            given(categoryMapper.selectCount(any())).willReturn(0L);

            categoryService.delete(1L);

            verify(categoryMapper).deleteById(1L);
        }

        @Test
        @DisplayName("delete - 有子分类时应抛出异常")
        void shouldThrowWhenHasChildren() {
            given(categoryMapper.selectCount(any())).willReturn(2L);

            assertThatThrownBy(() -> categoryService.delete(1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("存在子分类");
        }
    }
}
