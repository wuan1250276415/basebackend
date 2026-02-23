package com.basebackend.cache.pipeline;

import java.util.List;

/**
 * Pipeline 执行结果
 * 按照操作提交顺序保存每个操作的结果
 */
public class PipelineResult {

    private final List<Object> results;

    public PipelineResult(List<Object> results) {
        this.results = results;
    }

    /**
     * 获取第 i 个操作的结果
     *
     * @param index 操作索引（从 0 开始）
     * @return 操作结果：GET 返回值/null，SET 返回 true/false，DELETE 返回 true/false，
     *         EXISTS 返回 true/false，INCR 返回 Long，EXPIRE 返回 true/false
     */
    public Object get(int index) {
        if (index < 0 || index >= results.size()) {
            throw new IndexOutOfBoundsException(
                    "Pipeline result index " + index + " out of bounds, size=" + results.size());
        }
        return results.get(index);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(int index, Class<T> type) {
        Object value = get(index);
        if (value == null) {
            return null;
        }
        return (T) value;
    }

    public int size() {
        return results.size();
    }

    public List<Object> asList() {
        return results;
    }
}
