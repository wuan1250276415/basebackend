package com.basebackend.common.tree;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TreeBuilder 树形结构构建器单元测试
 */
class TreeBuilderTest {

    private SimpleTreeNode<Long> node(Long id, Long parentId, String label, int sort) {
        var n = new SimpleTreeNode<Long>();
        n.setId(id);
        n.setParentId(parentId);
        n.setLabel(label);
        n.setSort(sort);
        return n;
    }

    private List<SimpleTreeNode<Long>> sampleNodes() {
        return new ArrayList<>(List.of(
                node(1L, null, "根1", 1),
                node(2L, null, "根2", 2),
                node(3L, 1L, "子1-1", 2),
                node(4L, 1L, "子1-2", 1),
                node(5L, 3L, "孙1-1-1", 1),
                node(6L, 2L, "子2-1", 1)
        ));
    }

    // ========== buildTree ==========

    @Nested
    @DisplayName("buildTree")
    class BuildTree {

        @Test
        @DisplayName("构建正确的树结构")
        void shouldBuildCorrectTree() {
            var roots = TreeBuilder.buildTree(sampleNodes());
            assertThat(roots).hasSize(2);
            assertThat(roots.get(0).getLabel()).isEqualTo("根1");
            assertThat(roots.get(1).getLabel()).isEqualTo("根2");
        }

        @Test
        @DisplayName("子节点正确挂载")
        void shouldAttachChildren() {
            var roots = TreeBuilder.buildTree(sampleNodes());
            var root1 = roots.get(0);
            assertThat(root1.getChildren()).hasSize(2);
        }

        @Test
        @DisplayName("按 sort 排序")
        void shouldSortBySort() {
            var roots = TreeBuilder.buildTree(sampleNodes());
            var root1Children = roots.get(0).getChildren();
            // sort=1 的 "子1-2" 应排在 sort=2 的 "子1-1" 前面
            assertThat(root1Children.get(0).getLabel()).isEqualTo("子1-2");
            assertThat(root1Children.get(1).getLabel()).isEqualTo("子1-1");
        }

        @Test
        @DisplayName("三层嵌套正确")
        void shouldHandleThreeLevels() {
            var roots = TreeBuilder.buildTree(sampleNodes());
            var grandchild = roots.get(0).getChildren().get(1).getChildren(); // 子1-1 的子节点
            assertThat(grandchild).hasSize(1);
            assertThat(grandchild.get(0).getLabel()).isEqualTo("孙1-1-1");
        }

        @Test
        @DisplayName("null 输入返回空列表")
        void shouldReturnEmptyForNull() {
            assertThat(TreeBuilder.buildTree(null)).isEmpty();
        }

        @Test
        @DisplayName("空列表返回空列表")
        void shouldReturnEmptyForEmptyList() {
            assertThat(TreeBuilder.buildTree(new ArrayList<>())).isEmpty();
        }
    }

    // ========== flatten ==========

    @Nested
    @DisplayName("flatten")
    class Flatten {

        @Test
        @DisplayName("展平树为列表（DFS 顺序）")
        void shouldFlattenTree() {
            var roots = TreeBuilder.buildTree(sampleNodes());
            var flat = TreeBuilder.flatten(roots);
            assertThat(flat).hasSize(6);
        }

        @Test
        @DisplayName("null 返回空列表")
        void shouldReturnEmptyForNull() {
            assertThat(TreeBuilder.flatten(null)).isEmpty();
        }
    }

    // ========== findNode ==========

    @Nested
    @DisplayName("findNode")
    class FindNode {

        @Test
        @DisplayName("根节点查找")
        void shouldFindRoot() {
            var roots = TreeBuilder.buildTree(sampleNodes());
            var found = TreeBuilder.findNode(roots, 1L);
            assertThat(found).isNotNull();
            assertThat(found.getLabel()).isEqualTo("根1");
        }

        @Test
        @DisplayName("深层节点查找")
        void shouldFindDeepNode() {
            var roots = TreeBuilder.buildTree(sampleNodes());
            var found = TreeBuilder.findNode(roots, 5L);
            assertThat(found).isNotNull();
            assertThat(found.getLabel()).isEqualTo("孙1-1-1");
        }

        @Test
        @DisplayName("不存在的节点返回 null")
        void shouldReturnNullForMissing() {
            var roots = TreeBuilder.buildTree(sampleNodes());
            assertThat(TreeBuilder.findNode(roots, 999L)).isNull();
        }

        @Test
        @DisplayName("null 参数返回 null")
        void shouldReturnNullForNullParams() {
            SimpleTreeNode<Long> result1 = TreeBuilder.findNode(null, 1L);
            assertThat(result1).isNull();
            var roots = TreeBuilder.buildTree(sampleNodes());
            SimpleTreeNode<Long> result2 = TreeBuilder.findNode(roots, null);
            assertThat(result2).isNull();
        }
    }

    // ========== findPath ==========

    @Nested
    @DisplayName("findPath")
    class FindPath {

        @Test
        @DisplayName("查找路径正确")
        void shouldFindCorrectPath() {
            var roots = TreeBuilder.buildTree(sampleNodes());
            var path = TreeBuilder.findPath(roots, 5L);
            assertThat(path).hasSize(3);
            assertThat(path.get(0).getLabel()).isEqualTo("根1");
            assertThat(path.get(2).getLabel()).isEqualTo("孙1-1-1");
        }

        @Test
        @DisplayName("根节点路径长度为 1")
        void shouldReturnSingleForRoot() {
            var roots = TreeBuilder.buildTree(sampleNodes());
            var path = TreeBuilder.findPath(roots, 1L);
            assertThat(path).hasSize(1);
        }

        @Test
        @DisplayName("不存在的节点返回空路径")
        void shouldReturnEmptyForMissing() {
            var roots = TreeBuilder.buildTree(sampleNodes());
            assertThat(TreeBuilder.findPath(roots, 999L)).isEmpty();
        }
    }

    // ========== findChildren ==========

    @Nested
    @DisplayName("findChildren")
    class FindChildren {

        @Test
        @DisplayName("查找所有后代节点")
        void shouldFindAllDescendants() {
            var roots = TreeBuilder.buildTree(sampleNodes());
            var descendants = TreeBuilder.findChildren(roots, 1L);
            // 根1 下有：子1-1, 子1-2, 孙1-1-1 共 3 个后代
            assertThat(descendants).hasSize(3);
        }

        @Test
        @DisplayName("叶子节点无后代")
        void shouldReturnEmptyForLeaf() {
            var roots = TreeBuilder.buildTree(sampleNodes());
            var descendants = TreeBuilder.findChildren(roots, 5L);
            assertThat(descendants).isEmpty();
        }

        @Test
        @DisplayName("不存在的节点返回空")
        void shouldReturnEmptyForMissing() {
            var roots = TreeBuilder.buildTree(sampleNodes());
            assertThat(TreeBuilder.findChildren(roots, 999L)).isEmpty();
        }
    }
}
