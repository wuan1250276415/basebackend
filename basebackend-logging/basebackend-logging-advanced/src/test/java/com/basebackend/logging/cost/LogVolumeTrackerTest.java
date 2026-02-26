package com.basebackend.logging.cost;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LogVolumeTracker 日志量跟踪器测试")
class LogVolumeTrackerTest {

    private LogVolumeTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new LogVolumeTracker(60);
    }

    @Test
    @DisplayName("记录事件后应正确统计事件数和字节数")
    void record_shouldTrackEventsAndBytes() {
        tracker.record("service-a", 100);
        tracker.record("service-a", 200);
        tracker.record("service-b", 50);

        assertThat(tracker.getEventCount("service-a")).isEqualTo(2);
        assertThat(tracker.getByteCount("service-a")).isEqualTo(300);
        assertThat(tracker.getEventCount("service-b")).isEqualTo(1);
        assertThat(tracker.getByteCount("service-b")).isEqualTo(50);
    }

    @Test
    @DisplayName("未记录的 key 应返回 0")
    void get_shouldReturnZero_whenKeyNotRecorded() {
        assertThat(tracker.getEventCount("unknown")).isZero();
        assertThat(tracker.getByteCount("unknown")).isZero();
    }

    @Test
    @DisplayName("getAllSnapshots 应返回所有 key 的快照")
    void getAllSnapshots_shouldReturnAllKeys() {
        tracker.record("svc-1", 100);
        tracker.record("svc-2", 200);

        Map<String, LogVolumeTracker.VolumeSnapshot> snapshots = tracker.getAllSnapshots();
        assertThat(snapshots).hasSize(2);
        assertThat(snapshots.get("svc-1").windowEvents()).isEqualTo(1);
        assertThat(snapshots.get("svc-2").windowBytes()).isEqualTo(200);
    }

    @Test
    @DisplayName("totalEvents/totalBytes 应累加跨窗口")
    void record_shouldAccumulateTotals() {
        tracker.record("svc", 100);
        tracker.record("svc", 200);

        Map<String, LogVolumeTracker.VolumeSnapshot> snapshots = tracker.getAllSnapshots();
        assertThat(snapshots.get("svc").totalEvents()).isEqualTo(2);
        assertThat(snapshots.get("svc").totalBytes()).isEqualTo(300);
    }

    @Test
    @DisplayName("窗口过期后应重置窗口计数器")
    void windowCounter_shouldResetAfterExpiry() {
        // 使用 1 秒窗口以便测试过期
        LogVolumeTracker shortTracker = new LogVolumeTracker(1);
        shortTracker.record("svc", 100);
        assertThat(shortTracker.getEventCount("svc")).isEqualTo(1);

        // 等待窗口过期
        try {
            Thread.sleep(1100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 窗口计数器应已重置
        assertThat(shortTracker.getEventCount("svc")).isZero();
    }
}
