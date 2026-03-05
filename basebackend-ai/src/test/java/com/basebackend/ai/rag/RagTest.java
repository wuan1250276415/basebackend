package com.basebackend.ai.rag;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("RAG 模块测试")
class RagTest {

    // ==================== TextSplitter ====================

    @Nested
    @DisplayName("TextSplitter 测试")
    @Timeout(2)
    class TextSplitterTest {

        @Test
        @DisplayName("基本分割")
        void basicSplit() {
            TextSplitter splitter = new TextSplitter(10, 0);
            List<String> chunks = splitter.split("abcdefghijklmnopqrst");
            assertThat(chunks).hasSize(2);
            assertThat(chunks.get(0)).hasSize(10);
            assertThat(chunks.get(1)).hasSize(10);
        }

        @Test
        @DisplayName("带 overlap 分割")
        void splitWithOverlap() {
            TextSplitter splitter = new TextSplitter(10, 3);
            List<String> chunks = splitter.split("abcdefghijklmnopqrst");
            // 第一块 0-10, 第二块 7-17, 第三块 14-20
            assertThat(chunks.size()).isGreaterThanOrEqualTo(2);
            // 验证有重叠部分
            if (chunks.size() >= 2) {
                String chunk1End = chunks.get(0).substring(chunks.get(0).length() - 3);
                assertThat(chunks.get(1)).startsWith(chunk1End);
            }
        }

        @Test
        @DisplayName("overlap 场景能正确到达尾块并结束")
        void splitWithOverlapReachesTail() {
            TextSplitter splitter = new TextSplitter(10, 3);
            List<String> chunks = splitter.split("abcdefghijklmnopqrst");
            assertThat(chunks.getLast()).isEqualTo("opqrst");
        }

        @Test
        @DisplayName("在句号处断开")
        void splitAtSentenceBoundary() {
            TextSplitter splitter = new TextSplitter(20, 0);
            String text = "第一句话结束了。第二句话也结束了。第三句话。";
            List<String> chunks = splitter.split(text);
            // 应该在句号处断开
            assertThat(chunks).isNotEmpty();
            assertThat(chunks.getFirst()).endsWith("。");
        }

        @Test
        @DisplayName("在换行处断开")
        void splitAtNewline() {
            TextSplitter splitter = new TextSplitter(15, 0);
            String text = "第一行内容\n第二行内容\n第三行内容";
            List<String> chunks = splitter.split(text);
            assertThat(chunks).isNotEmpty();
        }

        @Test
        @DisplayName("null 输入返回空列表")
        void nullInput() {
            TextSplitter splitter = new TextSplitter(10, 0);
            assertThat(splitter.split(null)).isEmpty();
        }

        @Test
        @DisplayName("空白输入返回空列表")
        void blankInput() {
            TextSplitter splitter = new TextSplitter(10, 0);
            assertThat(splitter.split("")).isEmpty();
            assertThat(splitter.split("   ")).isEmpty();
        }

        @Test
        @DisplayName("短文本不分割")
        void shortTextNoSplit() {
            TextSplitter splitter = new TextSplitter(100, 0);
            List<String> chunks = splitter.split("短文本");
            assertThat(chunks).hasSize(1);
            assertThat(chunks.getFirst()).isEqualTo("短文本");
        }

        @Test
        @DisplayName("chunkSize 非法值抛异常")
        void invalidChunkSize() {
            assertThatThrownBy(() -> new TextSplitter(0, 0))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new TextSplitter(-1, 0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("chunkOverlap 非法值抛异常")
        void invalidChunkOverlap() {
            assertThatThrownBy(() -> new TextSplitter(10, -1))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new TextSplitter(10, 10))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new TextSplitter(10, 15))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ==================== SimpleVectorStore ====================

    @Nested
    @DisplayName("SimpleVectorStore 测试")
    class SimpleVectorStoreTest {

        private SimpleVectorStore store;

        @BeforeEach
        void setUp() {
            store = new SimpleVectorStore();
        }

        @Test
        @DisplayName("存储和检索")
        void storeAndSearch() {
            store.store("doc1", "Java 虚拟线程", new float[]{1.0f, 0.0f, 0.0f});
            store.store("doc2", "Python 协程", new float[]{0.0f, 1.0f, 0.0f});
            store.store("doc3", "Java 记录类", new float[]{0.9f, 0.1f, 0.0f});

            // 查询最接近 doc1 的向量
            List<VectorStore.SearchResult> results = store.search(
                    new float[]{1.0f, 0.0f, 0.0f}, 2, 0.5
            );

            assertThat(results).hasSize(2);
            assertThat(results.getFirst().id()).isEqualTo("doc1");
            assertThat(results.getFirst().score()).isCloseTo(1.0, within(0.001));
            assertThat(results.get(1).id()).isEqualTo("doc3");
        }

        @Test
        @DisplayName("相似度阈值过滤")
        void thresholdFiltering() {
            store.store("doc1", "内容1", new float[]{1.0f, 0.0f});
            store.store("doc2", "内容2", new float[]{0.0f, 1.0f});

            // 高阈值，只匹配精确的
            List<VectorStore.SearchResult> results = store.search(
                    new float[]{1.0f, 0.0f}, 10, 0.99
            );
            assertThat(results).hasSize(1);
            assertThat(results.getFirst().id()).isEqualTo("doc1");
        }

        @Test
        @DisplayName("topK 限制返回数量")
        void topKLimit() {
            for (int i = 0; i < 10; i++) {
                store.store("doc" + i, "内容" + i, new float[]{(float) i, 1.0f});
            }

            List<VectorStore.SearchResult> results = store.search(
                    new float[]{9.0f, 1.0f}, 3, 0.0
            );
            assertThat(results).hasSize(3);
        }

        @Test
        @DisplayName("delete 删除文档块")
        void deleteDocument() {
            store.store("doc1", "内容1", new float[]{1.0f, 0.0f});
            assertThat(store.size()).isEqualTo(1);

            store.delete("doc1");
            assertThat(store.size()).isZero();
        }

        @Test
        @DisplayName("clear 清空所有数据")
        void clearAll() {
            store.store("doc1", "内容1", new float[]{1.0f, 0.0f});
            store.store("doc2", "内容2", new float[]{0.0f, 1.0f});
            assertThat(store.size()).isEqualTo(2);

            store.clear();
            assertThat(store.size()).isZero();
        }

        @Test
        @DisplayName("空存储检索返回空列表")
        void searchEmptyStore() {
            List<VectorStore.SearchResult> results = store.search(
                    new float[]{1.0f, 0.0f}, 5, 0.0
            );
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("向量维度不匹配抛异常")
        void dimensionMismatch() {
            store.store("doc1", "内容", new float[]{1.0f, 0.0f, 0.0f});

            assertThatThrownBy(() -> store.search(new float[]{1.0f, 0.0f}, 5, 0.0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("维度不匹配");
        }

        @Test
        @DisplayName("覆盖存储同 ID 文档")
        void overwriteSameId() {
            store.store("doc1", "旧内容", new float[]{1.0f, 0.0f});
            store.store("doc1", "新内容", new float[]{0.0f, 1.0f});

            assertThat(store.size()).isEqualTo(1);
            List<VectorStore.SearchResult> results = store.search(
                    new float[]{0.0f, 1.0f}, 1, 0.0
            );
            assertThat(results.getFirst().content()).isEqualTo("新内容");
        }

        @Test
        @DisplayName("余弦相似度 - 正交向量为 0")
        void orthogonalVectors() {
            store.store("doc1", "X方向", new float[]{1.0f, 0.0f});
            store.store("doc2", "Y方向", new float[]{0.0f, 1.0f});

            List<VectorStore.SearchResult> results = store.search(
                    new float[]{1.0f, 0.0f}, 2, 0.0
            );
            // doc1 相似度=1.0, doc2 相似度=0.0
            assertThat(results.get(0).score()).isCloseTo(1.0, within(0.001));
            assertThat(results.get(1).score()).isCloseTo(0.0, within(0.001));
        }

        @Test
        @DisplayName("SearchResult Record 字段")
        void searchResultFields() {
            VectorStore.SearchResult result = new VectorStore.SearchResult("id1", "内容", 0.95);
            assertThat(result.id()).isEqualTo("id1");
            assertThat(result.content()).isEqualTo("内容");
            assertThat(result.score()).isEqualTo(0.95);
        }
    }
}
