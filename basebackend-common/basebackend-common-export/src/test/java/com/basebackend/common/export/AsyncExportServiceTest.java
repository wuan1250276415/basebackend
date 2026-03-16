package com.basebackend.common.export;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

class AsyncExportServiceTest {

    @Test
    void constructor_appliesConfiguredThreadPoolSize() throws Exception {
        AsyncExportService service = createService(2, 24);
        try {
            ExecutorService executor = readField(service, "executor", ExecutorService.class);
            assertInstanceOf(ThreadPoolExecutor.class, executor);
            ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executor;

            assertEquals(2, threadPoolExecutor.getCorePoolSize());
            assertEquals(2, threadPoolExecutor.getMaximumPoolSize());
        } finally {
            service.close();
        }
    }

    @Test
    void exportAsync_limitsConcurrentWorkersByConfiguredPoolSize() throws Exception {
        AsyncExportService service = createService(1, 24);
        CountDownLatch releaseLatch = new CountDownLatch(1);
        AtomicInteger startedCount = new AtomicInteger();
        try {
            String taskId1 = service.exportAsync(() -> blockingSupplier(startedCount, releaseLatch),
                    TestRow.class, ExportFormat.CSV);
            String taskId2 = service.exportAsync(() -> blockingSupplier(startedCount, releaseLatch),
                    TestRow.class, ExportFormat.CSV);
            String taskId3 = service.exportAsync(() -> blockingSupplier(startedCount, releaseLatch),
                    TestRow.class, ExportFormat.CSV);

            waitUntil(() -> startedCount.get() == 1, Duration.ofSeconds(2));

            long pendingCount = List.of(taskId2, taskId3).stream()
                    .map(service::getExportStatus)
                    .filter(status -> status != null && status.getStatus() == ExportTaskStatus.Status.PENDING)
                    .count();
            assertEquals(2, pendingCount);

            releaseLatch.countDown();
            waitUntil(() -> isCompleted(service.getExportStatus(taskId1))
                    && isCompleted(service.getExportStatus(taskId2))
                    && isCompleted(service.getExportStatus(taskId3)), Duration.ofSeconds(2));
        } finally {
            releaseLatch.countDown();
            service.close();
        }
    }

    @Test
    void cleanupExpiredTasks_removesOnlyTasksBeyondTtl() throws Exception {
        AsyncExportService service = createService(1, 1);
        try {
            String expiredCompletedTaskId = service.exportAsync(AsyncExportServiceTest::sampleData,
                    TestRow.class, ExportFormat.CSV);
            waitUntil(() -> isCompleted(service.getExportStatus(expiredCompletedTaskId)), Duration.ofSeconds(2));

            String recentCompletedTaskId = service.exportAsync(AsyncExportServiceTest::sampleData,
                    TestRow.class, ExportFormat.CSV);
            waitUntil(() -> isCompleted(service.getExportStatus(recentCompletedTaskId)), Duration.ofSeconds(2));

            String expiredFailedTaskId = service.exportAsync(() -> {
                throw new IllegalStateException("boom");
            }, TestRow.class, ExportFormat.CSV);
            waitUntil(() -> isFailed(service.getExportStatus(expiredFailedTaskId)), Duration.ofSeconds(2));

            long ttlMillis = readField(service, "taskTtlMillis", Long.class);
            long now = System.currentTimeMillis();

            ExportTaskStatus expiredCompletedStatus = service.getExportStatus(expiredCompletedTaskId);
            expiredCompletedStatus.setCompletedAt(now - ttlMillis - 1);

            ExportTaskStatus recentCompletedStatus = service.getExportStatus(recentCompletedTaskId);
            recentCompletedStatus.setCompletedAt(now - Math.max(ttlMillis / 2, 1));

            ExportTaskStatus expiredFailedStatus = service.getExportStatus(expiredFailedTaskId);
            expiredFailedStatus.setCompletedAt(now - ttlMillis - 1);

            service.cleanupExpiredTasks();

            assertNull(service.getExportStatus(expiredCompletedTaskId));
            assertNotNull(service.getExportStatus(recentCompletedTaskId));
            assertNull(service.getExportStatus(expiredFailedTaskId));
        } finally {
            service.close();
        }
    }

    private static List<TestRow> blockingSupplier(AtomicInteger startedCount, CountDownLatch releaseLatch) {
        startedCount.incrementAndGet();
        awaitLatch(releaseLatch);
        return sampleData();
    }

    private static void awaitLatch(CountDownLatch releaseLatch) {
        try {
            releaseLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Task interrupted while waiting", e);
        }
    }

    private static List<TestRow> sampleData() {
        return List.of(new TestRow("Alice"));
    }

    private static boolean isCompleted(ExportTaskStatus status) {
        return status != null && status.getStatus() == ExportTaskStatus.Status.COMPLETED;
    }

    private static boolean isFailed(ExportTaskStatus status) {
        return status != null && status.getStatus() == ExportTaskStatus.Status.FAILED;
    }

    private static AsyncExportService createService(int threadPoolSize, long taskTtlHours) {
        ExportManager exportManager = new ExportManager(List.of(new StubCsvExportService()));
        return new AsyncExportService(exportManager, threadPoolSize, taskTtlHours);
    }

    private static void waitUntil(BooleanSupplier condition, Duration timeout) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(20);
        }
        fail("Condition was not met within " + timeout);
    }

    private static <T> T readField(Object target, String fieldName, Class<T> type) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return type.cast(field.get(target));
    }

    private static class StubCsvExportService implements ExportService {
        @Override
        public ExportFormat supportedFormat() {
            return ExportFormat.CSV;
        }

        @Override
        public <T> ExportResult export(List<T> data, Class<T> clazz) {
            return ExportResult.builder()
                    .fileName(clazz.getSimpleName() + ".csv")
                    .contentType("text/csv")
                    .content(("rows=" + data.size()).getBytes(StandardCharsets.UTF_8))
                    .build();
        }
    }

    private static class TestRow {
        @ExportField(label = "Name", order = 1)
        private String name;

        public TestRow(String name) {
            this.name = name;
        }
    }
}
