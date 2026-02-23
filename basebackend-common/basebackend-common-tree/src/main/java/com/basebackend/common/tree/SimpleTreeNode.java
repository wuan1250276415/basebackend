package com.basebackend.common.tree;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SimpleTreeNode<T> implements TreeNode<T> {

    private T id;
    private T parentId;
    private String label;
    private Integer sort = 0;
    private List<SimpleTreeNode<T>> children = new ArrayList<>();

    @Override
    @SuppressWarnings("unchecked")
    public void setChildren(List children) {
        this.children = children;
    }
}
