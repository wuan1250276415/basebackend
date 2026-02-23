package com.basebackend.common.tree;

import java.util.*;
import java.util.stream.Collectors;

public final class TreeBuilder {

    private TreeBuilder() {
    }

    @SuppressWarnings("unchecked")
    public static <T extends TreeNode<ID>, ID> List<T> buildTree(List<T> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return new ArrayList<>();
        }

        Map<ID, T> nodeMap = new LinkedHashMap<>();
        for (T node : nodes) {
            nodeMap.put(node.getId(), node);
        }

        List<T> roots = new ArrayList<>();
        for (T node : nodes) {
            ID parentId = node.getParentId();
            if (parentId == null || !nodeMap.containsKey(parentId)) {
                roots.add(node);
            } else {
                T parent = nodeMap.get(parentId);
                List<T> children = (List<T>) parent.getChildren();
                if (children == null) {
                    children = new ArrayList<>();
                    parent.setChildren(children);
                }
                children.add(node);
            }
        }

        sortTree(roots);
        return roots;
    }

    public static <T extends TreeNode<ID>, ID> List<T> flatten(List<T> roots) {
        List<T> result = new ArrayList<>();
        if (roots == null) {
            return result;
        }
        for (T root : roots) {
            flattenDfs(root, result);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static <T extends TreeNode<ID>, ID> void flattenDfs(T node, List<T> result) {
        result.add(node);
        List<T> children = (List<T>) node.getChildren();
        if (children != null) {
            for (T child : children) {
                flattenDfs(child, result);
            }
        }
    }

    public static <T extends TreeNode<ID>, ID> T findNode(List<T> roots, ID id) {
        if (roots == null || id == null) {
            return null;
        }
        for (T root : roots) {
            T found = findNodeDfs(root, id);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T extends TreeNode<ID>, ID> T findNodeDfs(T node, ID id) {
        if (id.equals(node.getId())) {
            return node;
        }
        List<T> children = (List<T>) node.getChildren();
        if (children != null) {
            for (T child : children) {
                T found = findNodeDfs(child, id);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    public static <T extends TreeNode<ID>, ID> List<T> findPath(List<T> roots, ID id) {
        if (roots == null || id == null) {
            return new ArrayList<>();
        }
        List<T> path = new ArrayList<>();
        for (T root : roots) {
            if (findPathDfs(root, id, path)) {
                return path;
            }
        }
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    private static <T extends TreeNode<ID>, ID> boolean findPathDfs(T node, ID id, List<T> path) {
        path.add(node);
        if (id.equals(node.getId())) {
            return true;
        }
        List<T> children = (List<T>) node.getChildren();
        if (children != null) {
            for (T child : children) {
                if (findPathDfs(child, id, path)) {
                    return true;
                }
            }
        }
        path.remove(path.size() - 1);
        return false;
    }

    public static <T extends TreeNode<ID>, ID> List<T> findChildren(List<T> roots, ID parentId) {
        if (roots == null || parentId == null) {
            return new ArrayList<>();
        }
        T parent = findNode(roots, parentId);
        if (parent == null) {
            return new ArrayList<>();
        }
        List<T> descendants = new ArrayList<>();
        collectDescendants(parent, descendants);
        return descendants;
    }

    @SuppressWarnings("unchecked")
    private static <T extends TreeNode<ID>, ID> void collectDescendants(T node, List<T> result) {
        List<T> children = (List<T>) node.getChildren();
        if (children != null) {
            for (T child : children) {
                result.add(child);
                collectDescendants(child, result);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends TreeNode<ID>, ID> void sortTree(List<T> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        nodes.sort(Comparator.comparingInt(n -> n.getSort() != null ? n.getSort() : 0));
        for (T node : nodes) {
            sortTree((List<T>) node.getChildren());
        }
    }
}
