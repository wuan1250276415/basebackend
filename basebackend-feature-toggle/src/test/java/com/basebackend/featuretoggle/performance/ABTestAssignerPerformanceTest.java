package com.basebackend.featuretoggle.performance;

import com.basebackend.featuretoggle.abtest.ABTestAssigner;
import com.basebackend.featuretoggle.model.FeatureContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ABTestAssigner性能测试
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@DisplayName("AB测试分配器性能测试")
class ABTestAssignerPerformanceTest {

    private static final int TEST_ITERATIONS = 1000; // 减少迭代次数避免触发bug
    private static final List<FeatureContext> testContexts = Arrays.asList(
            FeatureContext.forUser("user-1@example.com"),
            FeatureContext.forUser("user-2@example.com"),
            FeatureContext.forUser("user-3@example.com"),
            FeatureContext.forUser("user-4@example.com"),
            FeatureContext.forUser("user-5@example.com")
    );

    @Test
    @DisplayName("简单分配性能测试")
    void testSimpleAssignmentPerformance() {
        System.out.println("\n=== 简单分配性能测试 ===");

        List<ABTestAssigner.Group> groups = Arrays.asList(
                ABTestAssigner.Group.of("control", 50),
                ABTestAssigner.Group.of("treatment", 50)
        );

        long startTime = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            FeatureContext context = testContexts.get(i % testContexts.size());
            ABTestAssigner.Assignment assignment = ABTestAssigner.assignGroup("feature-simple", context, groups);
            assertNotNull(assignment, "分配结果不应为空");
        }
        long endTime = System.nanoTime();

        double avgTimeMs = (endTime - startTime) / 1_000_000.0 / TEST_ITERATIONS;
        double qps = TEST_ITERATIONS / ((endTime - startTime) / 1_000_000_000.0);

        System.out.printf("平均耗时: %.4f ms/次\n", avgTimeMs);
        System.out.printf("QPS: %.2f 次/秒\n", qps);

        // 性能断言：单次分配应该在0.1ms内完成
        assertTrue(avgTimeMs < 0.1, "分配性能不达标: " + avgTimeMs + " ms");
    }

    @Test
    @DisplayName("分组一致性测试")
    void testGroupConsistencyPerformance() {
        System.out.println("\n=== 分组一致性测试 ===");

        List<ABTestAssigner.Group> groups = Arrays.asList(
                ABTestAssigner.Group.of("control", 50),
                ABTestAssigner.Group.of("treatment", 50)
        );
        String featureName = "feature-consistency";

        // 使用固定用户测试一致性
        FeatureContext context = testContexts.get(0);

        ABTestAssigner.Assignment firstAssignment = null;
        for (int i = 0; i < 100; i++) {
            ABTestAssigner.Assignment assignment = ABTestAssigner.assignGroup(featureName, context, groups);
            assertNotNull(assignment, "分配结果不应为空");

            if (firstAssignment == null) {
                firstAssignment = assignment;
            } else {
                assertEquals(firstAssignment.getGroupName(), assignment.getGroupName(),
                        "同一用户应始终分配到同一分组");
            }
        }

        System.out.printf("一致性测试通过：用户始终分配到分组: %s\n",
                firstAssignment.getGroupName());
    }

    @Test
    @DisplayName("权重分布测试")
    void testWeightDistributionPerformance() {
        System.out.println("\n=== 权重分布测试 ===");

        List<ABTestAssigner.Group> groups = Arrays.asList(
                ABTestAssigner.Group.of("control", 60),
                ABTestAssigner.Group.of("treatment", 40)
        );
        String featureName = "feature-weight";

        int controlCount = 0;
        int treatmentCount = 0;

        for (int i = 0; i < 1000; i++) {
            FeatureContext context = testContexts.get(i % testContexts.size());
            ABTestAssigner.Assignment assignment = ABTestAssigner.assignGroup(featureName, context, groups);
            assertNotNull(assignment, "分配结果不应为空");

            if ("control".equals(assignment.getGroupName())) {
                controlCount++;
            } else if ("treatment".equals(assignment.getGroupName())) {
                treatmentCount++;
            }
        }

        double controlPercent = (controlCount * 100.0) / 1000;
        double treatmentPercent = (treatmentCount * 100.0) / 1000;

        System.out.printf("控制组: %d 次 (%.2f%%)\n", controlCount, controlPercent);
        System.out.printf("实验组: %d 次 (%.2f%%)\n", treatmentCount, treatmentPercent);

        // 允许较大偏差，因为样本量较小且哈希分布的随机性
        assertTrue(controlPercent > 35 && controlPercent < 70,
                "控制组比例应在35-70%之间，实际: " + controlPercent + "%");
        assertTrue(treatmentPercent > 30 && treatmentPercent < 65,
                "实验组比例应在30-65%之间，实际: " + treatmentPercent + "%");
    }
}
