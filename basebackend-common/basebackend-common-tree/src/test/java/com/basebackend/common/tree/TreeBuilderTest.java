package com.basebackend.common.tree;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TreeBuilderTest {

    @Test
    void shouldBuildTreeFromFlatList() {
        List<SimpleTreeNode<Long>> nodes = Arrays.asList(
                createNode(1L, null, "Root", 0),
                createNode(2L, 1L, "Child1", 1),
                createNode(3L, 1L, "Child2", 2),
                createNode(4L, 2L, "GrandChild", 0)
        );

        List<SimpleTreeNode<Long>> tree = TreeBuilder.buildTree(nodes);

        assertEquals(1, tree.size());
        assertEquals("Root", tree.get(0).getLabel());
        assertEquals(2, tree.get(0).getChildren().size());
        assertEquals(1, tree.get(0).getChildren().get(0).getChildren().size());
    }

    @Test
    void shouldFlattenTree() {
        List<SimpleTreeNode<Long>> nodes = Arrays.asList(
                createNode(1L, null, "Root", 0),
                createNode(2L, 1L, "Child", 0),
                createNode(3L, 2L, "GrandChild", 0)
        );

        List<SimpleTreeNode<Long>> tree = TreeBuilder.buildTree(nodes);
        List<SimpleTreeNode<Long>> flat = TreeBuilder.flatten(tree);

        assertEquals(3, flat.size());
        assertEquals(1L, flat.get(0).getId());
        assertEquals(2L, flat.get(1).getId());
        assertEquals(3L, flat.get(2).getId());
    }

    @Test
    void shouldFindNodeById() {
        List<SimpleTreeNode<Long>> nodes = Arrays.asList(
                createNode(1L, null, "Root", 0),
                createNode(2L, 1L, "Child", 0),
                createNode(3L, 2L, "Target", 0)
        );

        List<SimpleTreeNode<Long>> tree = TreeBuilder.buildTree(nodes);
        SimpleTreeNode<Long> found = TreeBuilder.findNode(tree, 3L);

        assertNotNull(found);
        assertEquals("Target", found.getLabel());
    }

    @Test
    void shouldReturnNullForNonExistentNode() {
        List<SimpleTreeNode<Long>> nodes = Arrays.asList(
                createNode(1L, null, "Root", 0)
        );

        List<SimpleTreeNode<Long>> tree = TreeBuilder.buildTree(nodes);
        assertNull(TreeBuilder.findNode(tree, 999L));
    }

    @Test
    void shouldFindPathToNode() {
        List<SimpleTreeNode<Long>> nodes = Arrays.asList(
                createNode(1L, null, "Root", 0),
                createNode(2L, 1L, "Mid", 0),
                createNode(3L, 2L, "Leaf", 0)
        );

        List<SimpleTreeNode<Long>> tree = TreeBuilder.buildTree(nodes);
        List<SimpleTreeNode<Long>> path = TreeBuilder.findPath(tree, 3L);

        assertEquals(3, path.size());
        assertEquals(1L, path.get(0).getId());
        assertEquals(2L, path.get(1).getId());
        assertEquals(3L, path.get(2).getId());
    }

    @Test
    void shouldFindAllDescendants() {
        List<SimpleTreeNode<Long>> nodes = Arrays.asList(
                createNode(1L, null, "Root", 0),
                createNode(2L, 1L, "Child1", 0),
                createNode(3L, 1L, "Child2", 1),
                createNode(4L, 2L, "GrandChild", 0)
        );

        List<SimpleTreeNode<Long>> tree = TreeBuilder.buildTree(nodes);
        List<SimpleTreeNode<Long>> children = TreeBuilder.findChildren(tree, 1L);

        assertEquals(3, children.size());
    }

    @Test
    void shouldSortBySort() {
        List<SimpleTreeNode<Long>> nodes = Arrays.asList(
                createNode(1L, null, "Root", 0),
                createNode(2L, 1L, "B", 2),
                createNode(3L, 1L, "A", 1)
        );

        List<SimpleTreeNode<Long>> tree = TreeBuilder.buildTree(nodes);
        assertEquals("A", tree.get(0).getChildren().get(0).getLabel());
        assertEquals("B", tree.get(0).getChildren().get(1).getLabel());
    }

    @Test
    void shouldHandleEmptyList() {
        List<SimpleTreeNode<Long>> tree = TreeBuilder.buildTree(List.of());
        assertTrue(tree.isEmpty());
    }

    @Test
    void shouldHandleMultipleRoots() {
        List<SimpleTreeNode<Long>> nodes = Arrays.asList(
                createNode(1L, null, "Root1", 1),
                createNode(2L, null, "Root2", 0)
        );

        List<SimpleTreeNode<Long>> tree = TreeBuilder.buildTree(nodes);
        assertEquals(2, tree.size());
        assertEquals("Root2", tree.get(0).getLabel());
    }

    private SimpleTreeNode<Long> createNode(Long id, Long parentId, String label, int sort) {
        SimpleTreeNode<Long> node = new SimpleTreeNode<>();
        node.setId(id);
        node.setParentId(parentId);
        node.setLabel(label);
        node.setSort(sort);
        return node;
    }
}
