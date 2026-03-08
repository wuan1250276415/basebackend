package com.basebackend.database.migration.service.impl;

import com.basebackend.database.config.DatabaseEnhancedProperties;
import com.basebackend.database.exception.MigrationException;
import com.basebackend.database.migration.model.MigrationConfirmation;
import com.basebackend.database.migration.service.MigrationBackupService;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.*;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MigrationServiceImpl 测试")
class MigrationServiceImplTest {

    @Mock
    private Flyway flyway;

    @Mock
    private MigrationBackupService backupService;

    private MigrationServiceImpl migrationService;

    @BeforeEach
    void setUp() {
        DatabaseEnhancedProperties properties = new DatabaseEnhancedProperties();
        properties.getMigration().setRequireConfirmation(true);
        properties.getMigration().setTokenValidityMinutes(30);

        migrationService = new MigrationServiceImpl(flyway, backupService, properties);
        ReflectionTestUtils.setField(migrationService, "activeProfile", "prod");
    }

    @Test
    @DisplayName("生产环境开启确认门禁时禁止直接执行 migrate")
    void shouldBlockDirectMigrateInProductionWhenConfirmationRequired() {
        assertThatThrownBy(() -> migrationService.migrate())
                .isInstanceOf(MigrationException.class)
                .hasMessageContaining("migrate-with-confirmation");

        verify(flyway, never()).migrate();
    }

    @Test
    @DisplayName("生产环境开启确认门禁时禁止直接执行 migrateWithBackup")
    void shouldBlockDirectMigrateWithBackupInProductionWhenConfirmationRequired() {
        assertThatThrownBy(() -> migrationService.migrateWithBackup(true))
                .isInstanceOf(MigrationException.class)
                .hasMessageContaining("migrate-with-confirmation");

        verifyNoInteractions(backupService);
        verify(flyway, never()).migrate();
    }

    @Test
    @DisplayName("确认令牌在并发场景下只能被消费一次")
    void shouldConsumeConfirmationTokenAtomically() throws Exception {
        MigrateResult migrateResult = new MigrateResult();
        migrateResult.targetSchemaVersion = "2.0.0";
        migrateResult.migrationsExecuted = 1;
        when(flyway.migrate()).thenReturn(migrateResult);

        String token = migrationService.generateConfirmationToken();
        MigrationConfirmation confirmation = MigrationConfirmation.builder()
                .confirmationToken(token)
                .confirmedBy("admin")
                .reason("发布迁移")
                .createBackup(false)
                .build();

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch startSignal = new CountDownLatch(1);

        Callable<String> task = () -> {
            startSignal.await();
            try {
                return migrationService.migrateWithConfirmation(confirmation);
            } catch (Exception e) {
                return "ERR:" + e.getMessage();
            }
        };

        Future<String> future1 = executorService.submit(task);
        Future<String> future2 = executorService.submit(task);
        startSignal.countDown();

        String result1 = future1.get(5, TimeUnit.SECONDS);
        String result2 = future2.get(5, TimeUnit.SECONDS);
        executorService.shutdownNow();

        long successCount = Stream.of(result1, result2)
                .filter(result -> result.contains("数据库迁移完成"))
                .count();
        long invalidTokenCount = Stream.of(result1, result2)
                .filter(result -> result.contains("无效的确认令牌或令牌已过期"))
                .count();

        assertThat(successCount).isEqualTo(1);
        assertThat(invalidTokenCount).isEqualTo(1);
        verify(flyway, times(1)).migrate();
    }
}
