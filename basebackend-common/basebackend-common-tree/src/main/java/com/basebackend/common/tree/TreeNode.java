package com.basebackend.common.tree;

import java.util.List;

public interface TreeNode<T> {

    T getId();

    T getParentId();

    List<? extends TreeNode<T>> getChildren();

    void setChildren(List children);

    default Integer getSort() {
        return 0;
    }
}
