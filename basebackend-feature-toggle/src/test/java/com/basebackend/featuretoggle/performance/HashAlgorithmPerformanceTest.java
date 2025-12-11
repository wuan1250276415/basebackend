package com.basebackend.featuretoggle.performance;

import com.basebackend.featuretoggle.abtest.HashAlgorithm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HashAlgorithm性能测试
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@DisplayName("哈希算法性能测试")
class HashAlgorithmPerformanceTest {

    private static final int WARMUP_ITERATIONS = 10_000;
    private static final int TEST_ITERATIONS = 100_000;
    private static final List<String> testData = new ArrayList<>();

    @BeforeEach
    void setUp() {
        // 准备测试数据
        testData.clear();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            testData.add("user-" + i + "@example.com");
        }
    }

    @Test
    @DisplayName("MurMurHash3性能测试")
    void testMurmur3Performance() {
        System.out.println("\n=== MurMurHash3性能测试 ===");

        // 预热
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            HashAlgorithm.murmur3_32("warmup-" + i);
        }

        // 正式测试
        long startTime = System.nanoTime();
        long[] hashes = new long[TEST_ITERATIONS];
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            hashes[i] = HashAlgorithm.murmur3_32(testData.get(i));
        }
        long endTime = System.nanoTime();

        double avgTimeMs = (endTime - startTime) / 1_000_000.0 / TEST_ITERATIONS;
        double qps = TEST_ITERATIONS / ((endTime - startTime) / 1_000_000_000.0);

        System.out.printf("MurMurHash3平均耗时: %.4f ms/次\n", avgTimeMs);
        System.out.printf("MurMurHash3 QPS: %.2f 次/秒\n", qps);
        System.out.printf("哈希值范围: %d ~ %d\n",
                Arrays.stream(hashes).min().orElse(0),
                Arrays.stream(hashes).max().orElse(0));

        // 性能断言：单次操作应该在0.001ms内完成
        assertTrue(avgTimeMs < 0.001, "MurMurHash3性能不达标: " + avgTimeMs + " ms");
    }

    @Test
    @DisplayName("CRC32性能测试")
    void testCrc32Performance() {
        System.out.println("\n=== CRC32性能测试 ===");

        // 预热
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            HashAlgorithm.crc32("warmup-" + i);
        }

        // 正式测试
        long startTime = System.nanoTime();
        long[] hashes = new long[TEST_ITERATIONS];
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            hashes[i] = HashAlgorithm.crc32(testData.get(i));
        }
        long endTime = System.nanoTime();

        double avgTimeMs = (endTime - startTime) / 1_000_000.0 / TEST_ITERATIONS;
        double qps = TEST_ITERATIONS / ((endTime - startTime) / 1_000_000_000.0);

        System.out.printf("CRC32平均耗时: %.4f ms/次\n", avgTimeMs);
        System.out.printf("CRC32 QPS: %.2f 次/秒\n", qps);
        System.out.printf("哈希值范围: %d ~ %d\n",
                Arrays.stream(hashes).min().orElse(0),
                Arrays.stream(hashes).max().orElse(0));

        assertTrue(avgTimeMs < 0.001, "CRC32性能不达标: " + avgTimeMs + " ms");
    }

    @Test
    @DisplayName("MD5性能测试")
    void testMd5Performance() {
        System.out.println("\n=== MD5性能测试 ===");

        // 预热
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            HashAlgorithm.md5("warmup-" + i);
        }

        // 正式测试
        long startTime = System.nanoTime();
        long[] hashes = new long[TEST_ITERATIONS];
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            hashes[i] = HashAlgorithm.md5(testData.get(i));
        }
        long endTime = System.nanoTime();

        double avgTimeMs = (endTime - startTime) / 1_000_000.0 / TEST_ITERATIONS;
        double qps = TEST_ITERATIONS / ((endTime - startTime) / 1_000_000_000.0);

        System.out.printf("MD5平均耗时: %.4f ms/次\n", avgTimeMs);
        System.out.printf("MD5 QPS: %.2f 次/秒\n", qps);
        System.out.printf("哈希值范围: %d ~ %d\n",
                Arrays.stream(hashes).min().orElse(0),
                Arrays.stream(hashes).max().orElse(0));

        // MD5相对较慢，允许更长时间
        assertTrue(avgTimeMs < 0.005, "MD5性能不达标: " + avgTimeMs + " ms");
    }

    @Test
    @DisplayName("SHA1性能测试")
    void testSha1Performance() {
        System.out.println("\n=== SHA1性能测试 ===");

        // 预热
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            HashAlgorithm.sha1("warmup-" + i);
        }

        // 正式测试
        long startTime = System.nanoTime();
        long[] hashes = new long[TEST_ITERATIONS];
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            hashes[i] = HashAlgorithm.sha1(testData.get(i));
        }
        long endTime = System.nanoTime();

        double avgTimeMs = (endTime - startTime) / 1_000_000.0 / TEST_ITERATIONS;
        double qps = TEST_ITERATIONS / ((endTime - startTime) / 1_000_000_000.0);

        System.out.printf("SHA1平均耗时: %.4f ms/次\n", avgTimeMs);
        System.out.printf("SHA1 QPS: %.2f 次/秒\n", qps);
        System.out.printf("哈希值范围: %d ~ %d\n",
                Arrays.stream(hashes).min().orElse(0),
                Arrays.stream(hashes).max().orElse(0));

        // SHA1也相对较慢
        assertTrue(avgTimeMs < 0.005, "SHA1性能不达标: " + avgTimeMs + " ms");
    }

    @Test
    @DisplayName("算法比较测试")
    void testAlgorithmComparison() {
        System.out.println("\n=== 算法性能比较 ===");

        Map<String, Double> results = new HashMap<>();

        // 测试所有算法
        // MurMurHash3
        long startTime = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            HashAlgorithm.murmur3_32(testData.get(i));
        }
        long endTime = System.nanoTime();
        results.put("murmur3_32", (endTime - startTime) / 1_000_000.0 / TEST_ITERATIONS);

        // CRC32
        startTime = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            HashAlgorithm.crc32(testData.get(i));
        }
        endTime = System.nanoTime();
        results.put("crc32", (endTime - startTime) / 1_000_000.0 / TEST_ITERATIONS);

        // MD5
        startTime = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            HashAlgorithm.md5(testData.get(i));
        }
        endTime = System.nanoTime();
        results.put("md5", (endTime - startTime) / 1_000_000.0 / TEST_ITERATIONS);

        // SHA1
        startTime = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            HashAlgorithm.sha1(testData.get(i));
        }
        endTime = System.nanoTime();
        results.put("sha1", (endTime - startTime) / 1_000_000.0 / TEST_ITERATIONS);

        // 打印结果
        results.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .forEach(entry -> System.out.printf("%-12s: %.4f ms/次\n",
                        entry.getKey(), entry.getValue()));

        // 验证MurMurHash3性能良好（不要求一定最快）
        double murmurTime = results.get("murmur3_32");
        double crcTime = results.get("crc32");
        // MurMurHash3不应该比CRC32慢太多（允许2倍差异）
        assertTrue(murmurTime <= crcTime * 2.0,
                "MurMurHash3性能不应明显慢于CRC32");
    }

    @Test
    @DisplayName("哈希分布性测试")
    void testHashDistribution() {
        System.out.println("\n=== 哈希分布性测试 ===");

        int bucketCount = 100;
        int[] buckets = new int[bucketCount];

        // MurMurHash3分布性测试
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            long hash = HashAlgorithm.murmur3_32(testData.get(i));
            int bucket = HashAlgorithm.toPercentileBucket(hash);
            buckets[bucket]++;
        }

        // 计算分布标准差
        double mean = TEST_ITERATIONS / (double) bucketCount;
        double variance = Arrays.stream(buckets)
                .mapToDouble(b -> (b - mean) * (b - mean))
                .sum() / bucketCount;
        double stdDev = Math.sqrt(variance);

        System.out.printf("期望每桶数量: %.2f\n", mean);
        System.out.printf("实际分布标准差: %.4f\n", stdDev);
        System.out.printf("分布均匀性(标准差/均值): %.4f\n", stdDev / mean);

        // 标准差应该在均值的20%以内
        assertTrue(stdDev / mean < 0.2, "哈希分布不够均匀，标准差/均值: " + (stdDev / mean));
    }

    @Test
    @DisplayName("并发性能测试")
    void testConcurrentPerformance() throws InterruptedException {
        System.out.println("\n=== 并发性能测试 ===");

        int threadCount = 10;
        int iterationsPerThread = TEST_ITERATIONS / threadCount;

        long startTime = System.nanoTime();

        Thread[] threads = new Thread[threadCount];
        for (int t = 0; t < threadCount; t++) {
            final int threadIndex = t;
            threads[t] = new Thread(() -> {
                for (int i = 0; i < iterationsPerThread; i++) {
                    HashAlgorithm.murmur3_32("user-" + threadIndex + "-" + i);
                }
            });
            threads[t].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        long endTime = System.nanoTime();
        double totalTimeMs = (endTime - startTime) / 1_000_000.0;
        double qps = TEST_ITERATIONS / (totalTimeMs / 1000.0);

        System.out.printf("并发线程数: %d\n", threadCount);
        System.out.printf("总耗时: %.2f ms\n", totalTimeMs);
        System.out.printf("并发QPS: %.2f 次/秒\n", qps);

        // 并发性能应该和单线程相近（允许50%性能损失）
        assertTrue(qps > 50000, "并发性能不达标: " + qps + " QPS");
    }

    @Test
    @DisplayName("长时间稳定性测试")
    void testLongRunningStability() {
        System.out.println("\n=== 长时间稳定性测试 ===");

        int iterations = 1_000_000;
        long startTime = System.nanoTime();
        int errorCount = 0;

        for (int i = 0; i < iterations; i++) {
            try {
                long hash = HashAlgorithm.murmur3_32("user-" + i);
                if (hash < 0) {
                    errorCount++;
                }
            } catch (Exception e) {
                errorCount++;
            }
        }

        long endTime = System.nanoTime();
        double avgTimeMs = (endTime - startTime) / 1_000_000.0 / iterations;

        System.out.printf("测试迭代数: %d\n", iterations);
        System.out.printf("总耗时: %.2f ms\n", (endTime - startTime) / 1_000_000.0);
        System.out.printf("平均耗时: %.6f ms/次\n", avgTimeMs);
        System.out.printf("错误数: %d\n", errorCount);

        assertEquals(0, errorCount, "长时间运行不应有错误");
        assertTrue(avgTimeMs < 0.001, "长时间运行性能不达标: " + avgTimeMs + " ms");
    }
}
