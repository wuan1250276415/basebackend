package com.basebackend.backup.infrastructure.executor;

import com.basebackend.backup.domain.entity.BackupHistory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("IncrementalChain 链恢复时间校验测试")
class IncrementalChainTest {

    @Test
    @DisplayName("无增量时不允许恢复到全量快照之后")
    void shouldRejectTargetAfterFullWhenNoIncremental() {
        LocalDateTime baseTime = LocalDateTime.of(2026, 3, 5, 9, 0, 0);
        IncrementalChain chain = chainWith(baseTime, List.of());

        assertThat(chain.canRestoreTo(baseTime.plusMinutes(1))).isFalse();
    }

    @Test
    @DisplayName("无增量时允许恢复到全量快照时点")
    void shouldAllowTargetAtFullSnapshotTimeWhenNoIncremental() {
        LocalDateTime baseTime = LocalDateTime.of(2026, 3, 5, 9, 0, 0);
        IncrementalChain chain = chainWith(baseTime, List.of());

        assertThat(chain.canRestoreTo(baseTime)).isTrue();
    }

    @Test
    @DisplayName("有增量时不允许恢复到最后增量之后")
    void shouldRejectTargetAfterLatestIncremental() {
        LocalDateTime baseTime = LocalDateTime.of(2026, 3, 5, 9, 0, 0);
        BackupHistory inc = incremental(101L, baseTime.plusMinutes(10), 100L);
        IncrementalChain chain = chainWith(baseTime, List.of(inc));

        assertThat(chain.canRestoreTo(baseTime.plusMinutes(11))).isFalse();
    }

    private IncrementalChain chainWith(LocalDateTime fullStartedAt, List<BackupHistory> incrementals) {
        BackupHistory full = full(100L, fullStartedAt);
        IncrementalChain chain = IncrementalChain.builder()
                .chainId("chain-test")
                .fullBackup(full)
                .incrementalBackups(incrementals)
                .build();

        chain.setIncrementalCount(incrementals.size());
        if (!incrementals.isEmpty()) {
            chain.setEarliestIncrementalTime(incrementals.get(0).getStartedAt());
            chain.setLatestIncrementalTime(incrementals.get(incrementals.size() - 1).getStartedAt());
        }
        chain.checkChainIntegrity();
        return chain;
    }

    private BackupHistory full(Long id, LocalDateTime startedAt) {
        BackupHistory history = new BackupHistory();
        history.setId(id);
        history.setStatus("SUCCESS");
        history.setBackupType("full");
        history.setStartedAt(startedAt);
        return history;
    }

    private BackupHistory incremental(Long id, LocalDateTime startedAt, Long baseFullId) {
        BackupHistory history = new BackupHistory();
        history.setId(id);
        history.setStatus("SUCCESS");
        history.setBackupType("incremental");
        history.setBaseFullId(baseFullId);
        history.setStartedAt(startedAt);
        return history;
    }
}
