package com.basebackend.scheduler.workflow;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 工作流定义，包含节点、边与版本信息。
 * 提供拓扑校验能力，确保 DAG 合法。
 */
public final class WorkflowDefinition {

    private final String id;
    private final String name;
    private final String description;
    private final Map<String, WorkflowNode> nodes;
    private final List<WorkflowEdge> edges;
    private final long version;
    private final Instant createdAt;
    private final Instant updatedAt;

    private WorkflowDefinition(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "id");
        this.name = Objects.requireNonNull(builder.name, "name");
        this.description = builder.description;
        this.nodes = unmodifiableNodeCopy(builder.nodes);
        this.edges = unmodifiableEdgeCopy(builder.edges);
        this.version = builder.version;
        this.createdAt = builder.createdAt != null ? builder.createdAt : Instant.now();
        this.updatedAt = builder.updatedAt != null ? builder.updatedAt : this.createdAt;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, WorkflowNode> getNodes() {
        return nodes;
    }

    public List<WorkflowEdge> getEdges() {
        return edges;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 校验拓扑合法性，检测环并返回排序结果。
     *
     * @return 拓扑排序结果
     */
    public TopologicalSorter.Result validateTopology() {
        ensureEdgesBoundToNodes();
        TopologicalSorter.Result result = TopologicalSorter.sort(nodes, edges);
        if (result.hasCycle()) {
            throw new IllegalStateException("Workflow definition contains cycle: " + result.getUnresolvedNodes());
        }
        return result;
    }

    public WorkflowNode getNode(String nodeId) {
        return nodes.get(nodeId);
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static Builder builder(String id, String name) {
        return new Builder(id, name);
    }

    private void ensureEdgesBoundToNodes() {
        for (WorkflowEdge edge : edges) {
            if (!nodes.containsKey(edge.getFrom())) {
                throw new IllegalStateException("Unknown from node: " + edge.getFrom());
            }
            if (!nodes.containsKey(edge.getTo())) {
                throw new IllegalStateException("Unknown to node: " + edge.getTo());
            }
        }
    }

    private static Map<String, WorkflowNode> unmodifiableNodeCopy(Map<String, WorkflowNode> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }

    private static List<WorkflowEdge> unmodifiableEdgeCopy(List<WorkflowEdge> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(source));
    }

    /**
     * 构建器，统一定义与时间戳管理。
     */
    public static final class Builder {
        private final String id;
        private final String name;
        private String description;
        private Map<String, WorkflowNode> nodes = Collections.emptyMap();
        private List<WorkflowEdge> edges = Collections.emptyList();
        private long version = 1L;
        private Instant createdAt;
        private Instant updatedAt;

        private Builder(String id, String name) {
            this.id = id;
            this.name = name;
        }

        private Builder(WorkflowDefinition source) {
            this.id = source.id;
            this.name = source.name;
            this.description = source.description;
            this.nodes = source.nodes;
            this.edges = source.edges;
            this.version = source.version;
            this.createdAt = source.createdAt;
            this.updatedAt = source.updatedAt;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder nodes(Map<String, WorkflowNode> nodes) {
            if (nodes != null) {
                this.nodes = new LinkedHashMap<>(nodes);
            }
            return this;
        }

        public Builder edges(List<WorkflowEdge> edges) {
            if (edges != null) {
                this.edges = new ArrayList<>(edges);
            }
            return this;
        }

        public Builder version(long version) {
            this.version = version;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public WorkflowDefinition build() {
            return new WorkflowDefinition(this);
        }
    }
}
