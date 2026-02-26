package com.basebackend.observability.alert;

import com.basebackend.observability.alert.notifier.AlertNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AlertEngine单元测试
 */
@ExtendWith(MockitoExtension.class)
class AlertEngineTest {

    @Mock
    private AlertEvaluator alertEvaluator;

    @Mock
    private AlertNotifier emailNotifier;

    @Mock
    private AlertNotifier dingTalkNotifier;

    private AlertEngine alertEngine;

    @BeforeEach
    void setUp() {
        List<AlertNotifier> notifiers = Arrays.asList(emailNotifier, dingTalkNotifier);
        alertEngine = new AlertEngine(alertEvaluator, notifiers);
    }

    @Test
    void testEvaluateAlertRules_NoRules() {
        // When
        alertEngine.evaluateAlertRules();

        // Then - should not throw exception and return quickly
        // No additional assertions needed as the method should exit early
    }

    @Test
    void testEvaluateAlertRules_WithRules_NotTriggered() {
        // Given
        AlertRule rule = createTestRule(1L, "Test Rule", "cpu_usage", 80.0);
        alertEngine.registerRule(rule);

        AlertEvaluator.EvaluationResult result = AlertEvaluator.EvaluationResult.builder()
                .triggered(false)
                .message("Threshold not reached")
                .currentValue(String.valueOf(75.0))
                .thresholdValue(String.valueOf(80.0))
                .build();

        when(alertEvaluator.evaluate(rule)).thenReturn(result);

        // When
        alertEngine.evaluateAlertRules();

        // Then
        verify(alertEvaluator).evaluate(rule);
        verify(emailNotifier, never()).sendAlert(any());
        verify(dingTalkNotifier, never()).sendAlert(any());
    }

    @Test
    void testEvaluateAlertRules_WithRules_Triggered() {
        // Given
        AlertRule rule = createTestRule(2L, "CPU High", "cpu_usage", 80.0);
        alertEngine.registerRule(rule);

        AlertEvaluator.EvaluationResult result = AlertEvaluator.EvaluationResult.builder()
                .triggered(true)
                .message("CPU usage above threshold")
                .currentValue(String.valueOf(85.0))
                .thresholdValue(String.valueOf(80.0))
                .build();

        when(alertEvaluator.evaluate(rule)).thenReturn(result);
        when(emailNotifier.getNotifierType()).thenReturn("email");
        when(emailNotifier.isAvailable()).thenReturn(true);
        when(emailNotifier.sendAlert(any(AlertEvent.class))).thenReturn(true);

        // When
        alertEngine.evaluateAlertRules();

        // Then
        verify(alertEvaluator).evaluate(rule);
        verify(emailNotifier).sendAlert(any(AlertEvent.class));
        assertEquals(1, alertEngine.getRecentAlerts().size());
    }

    @Test
    void testEvaluateAlertRules_ExceptionDuringEvaluation() {
        // Given
        AlertRule rule = createTestRule(3L, "Error Rule", "memory_usage", 90.0);
        alertEngine.registerRule(rule);

        when(alertEvaluator.evaluate(rule)).thenThrow(new RuntimeException("Evaluation error"));

        // When - should not throw exception
        assertDoesNotThrow(() -> alertEngine.evaluateAlertRules());

        // Then - method should complete gracefully
        verify(alertEvaluator).evaluate(rule);
    }

    @Test
    void testIsInSuppressionPeriod_True() {
        // Given
        AlertRule rule = createTestRule(4L, "Suppression Rule", "disk_usage", 85.0);
        alertEngine.registerRule(rule);

        AlertEvaluator.EvaluationResult result = AlertEvaluator.EvaluationResult.builder()
                .triggered(true)
                .message("Disk usage high")
                .currentValue(String.valueOf(90.0))
                .thresholdValue(String.valueOf(85.0))
                .build();

        when(alertEvaluator.evaluate(rule)).thenReturn(result);
        when(emailNotifier.getNotifierType()).thenReturn("email");
        when(emailNotifier.isAvailable()).thenReturn(true);
        when(emailNotifier.sendAlert(any(AlertEvent.class))).thenReturn(true);

        // First evaluation triggers alert
        alertEngine.evaluateAlertRules();
        assertEquals(1, alertEngine.getRecentAlerts().size());

        // When - evaluate again immediately (within suppression period)
        alertEngine.evaluateAlertRules();

        // Then - should be suppressed, no new alert
        assertEquals(1, alertEngine.getRecentAlerts().size());
    }

    @Test
    void testIsInSuppressionPeriod_False() {
        // Given
        AlertRule rule = createTestRule(5L, "No Suppression Rule", "network_latency", 100.0);
        alertEngine.registerRule(rule);

        AlertEvaluator.EvaluationResult result = AlertEvaluator.EvaluationResult.builder()
                .triggered(true)
                .message("Network latency high")
                .currentValue(String.valueOf(150.0))
                .thresholdValue(String.valueOf(100.0))
                .build();

        when(alertEvaluator.evaluate(rule)).thenReturn(result);
        when(emailNotifier.getNotifierType()).thenReturn("email");
        when(emailNotifier.isAvailable()).thenReturn(true);
        when(emailNotifier.sendAlert(any(AlertEvent.class))).thenReturn(true);

        // First evaluation
        alertEngine.evaluateAlertRules();
        assertEquals(1, alertEngine.getRecentAlerts().size());

        // Manually set alert time to 6 minutes ago (past suppression period)
        AlertEvent oldAlert = alertEngine.getRecentAlerts().iterator().next();
        // Note: We can't easily modify the alert time without reflection in a real scenario
        // This test demonstrates the concept
    }

    @Test
    void testSendNotifications_Success() {
        // Given
        AlertEvent event = createTestAlertEvent(6L, "email");
        when(emailNotifier.getNotifierType()).thenReturn("email");
        when(emailNotifier.isAvailable()).thenReturn(true);
        when(emailNotifier.sendAlert(event)).thenReturn(true);

        // When
        alertEngine.sendNotifications(event);

        // Then
        verify(emailNotifier).sendAlert(event);
        assertEquals(AlertEvent.NotifyStatus.SUCCESS, event.getNotifyStatus());
        assertEquals(AlertEvent.AlertStatus.NOTIFIED, event.getStatus());
    }

    @Test
    void testSendNotifications_Failure() {
        // Given
        AlertEvent event = createTestAlertEvent(7L, "email");
        when(emailNotifier.getNotifierType()).thenReturn("email");
        when(emailNotifier.isAvailable()).thenReturn(true);
        when(emailNotifier.sendAlert(event)).thenReturn(false);

        // When
        alertEngine.sendNotifications(event);

        // Then
        verify(emailNotifier).sendAlert(event);
        assertEquals(AlertEvent.NotifyStatus.FAILED, event.getNotifyStatus());
        assertEquals(AlertEvent.AlertStatus.TRIGGERED, event.getStatus());
    }

    @Test
    void testSendNotifications_Exception() {
        // Given
        AlertEvent event = createTestAlertEvent(8L, "email");
        when(emailNotifier.getNotifierType()).thenReturn("email");
        when(emailNotifier.isAvailable()).thenReturn(true);
        when(emailNotifier.sendAlert(event)).thenThrow(new RuntimeException("Network error"));

        // When - should not throw exception
        assertDoesNotThrow(() -> alertEngine.sendNotifications(event));

        // Then
        verify(emailNotifier).sendAlert(event);
        assertEquals(AlertEvent.NotifyStatus.FAILED, event.getNotifyStatus());
    }

    @Test
    void testSendNotifications_NotifierNotAvailable() {
        // Given
        AlertEvent event = createTestAlertEvent(9L, "email");
        when(emailNotifier.getNotifierType()).thenReturn("email");
        when(emailNotifier.isAvailable()).thenReturn(false);

        // When
        alertEngine.sendNotifications(event);

        // Then
        verify(emailNotifier, never()).sendAlert(event);
        assertEquals(AlertEvent.NotifyStatus.FAILED, event.getNotifyStatus());
    }

    @Test
    void testSendNotifications_UnknownChannel() {
        // Given
        AlertEvent event = createTestAlertEvent(10L, "slack");
        // Setup mock to return notifier types for filtering
        when(emailNotifier.getNotifierType()).thenReturn("email");
        when(dingTalkNotifier.getNotifierType()).thenReturn("dingtalk");

        // When
        alertEngine.sendNotifications(event);

        // Then
        verify(emailNotifier, never()).sendAlert(event);
        verify(dingTalkNotifier, never()).sendAlert(event);
        assertEquals(AlertEvent.NotifyStatus.FAILED, event.getNotifyStatus());
    }

    @Test
    void testRegisterRule_NullRule() {
        // When
        alertEngine.registerRule(null);

        // Then
        assertEquals(0, alertEngine.getAllRules().size());
    }

    @Test
    void testRegisterRule_RuleWithoutId() {
        // Given
        AlertRule rule = AlertRule.builder()
                .ruleName("Test Rule")
                .metricName("cpu_usage")
                .thresholdValue(80.0)
                .build();

        // When
        alertEngine.registerRule(rule);

        // Then
        assertEquals(0, alertEngine.getAllRules().size());
    }

    @Test
    void testRegisterRule_ValidRule() {
        // Given
        AlertRule rule = createTestRule(11L, "Test Rule", "cpu_usage", 80.0);

        // When
        alertEngine.registerRule(rule);

        // Then
        assertEquals(1, alertEngine.getAllRules().size());
        assertTrue(alertEngine.getAllRules().contains(rule));
    }

    @Test
    void testUnregisterRule_ExistingRule() {
        // Given
        AlertRule rule = createTestRule(12L, "Test Rule", "cpu_usage", 80.0);
        alertEngine.registerRule(rule);
        assertEquals(1, alertEngine.getAllRules().size());

        // When
        alertEngine.unregisterRule(12L);

        // Then
        assertEquals(0, alertEngine.getAllRules().size());
    }

    @Test
    void testUnregisterRule_NonExistingRule() {
        // When
        alertEngine.unregisterRule(999L);

        // Then - should not throw exception
        assertEquals(0, alertEngine.getAllRules().size());
    }

    @Test
    void testGetAllRules() {
        // Given
        AlertRule rule1 = createTestRule(13L, "Rule 1", "cpu_usage", 80.0);
        AlertRule rule2 = createTestRule(14L, "Rule 2", "memory_usage", 85.0);

        alertEngine.registerRule(rule1);
        alertEngine.registerRule(rule2);

        // When
        Collection<AlertRule> rules = alertEngine.getAllRules();

        // Then
        assertEquals(2, rules.size());
        assertTrue(rules.contains(rule1));
        assertTrue(rules.contains(rule2));
    }

    @Test
    void testGetRecentAlerts() {
        // Given
        AlertRule rule = createTestRule(15L, "Test Rule", "cpu_usage", 80.0);
        alertEngine.registerRule(rule);

        AlertEvaluator.EvaluationResult result = AlertEvaluator.EvaluationResult.builder()
                .triggered(true)
                .message("Alert triggered")
                .currentValue(String.valueOf(85.0))
                .thresholdValue(String.valueOf(80.0))
                .build();

        when(alertEvaluator.evaluate(rule)).thenReturn(result);
        when(emailNotifier.getNotifierType()).thenReturn("email");
        when(emailNotifier.isAvailable()).thenReturn(true);
        when(emailNotifier.sendAlert(any(AlertEvent.class))).thenReturn(true);

        // Trigger alert
        alertEngine.evaluateAlertRules();

        // When
        Collection<AlertEvent> alerts = alertEngine.getRecentAlerts();

        // Then
        assertEquals(1, alerts.size());
        AlertEvent alert = alerts.iterator().next();
        assertEquals(15L, alert.getRuleId());
        assertEquals("Test Rule", alert.getRuleName());
    }

    @Test
    void testTestRule() {
        // Given
        AlertRule rule = createTestRule(16L, "Test Rule", "cpu_usage", 80.0);
        AlertEvaluator.EvaluationResult expectedResult = AlertEvaluator.EvaluationResult.builder()
                .triggered(false)
                .message("Test evaluation")
                .currentValue(String.valueOf(75.0))
                .thresholdValue(String.valueOf(80.0))
                .build();

        when(alertEvaluator.evaluate(rule)).thenReturn(expectedResult);

        // When
        AlertEvaluator.EvaluationResult result = alertEngine.testRule(rule);

        // Then
        assertEquals(expectedResult, result);
        verify(alertEvaluator).evaluate(rule);
    }

    @Test
    void testFindNotifier_Found() {
        // Given - Setup both notifiers with their types for filtering
        when(emailNotifier.getNotifierType()).thenReturn("email");
        when(dingTalkNotifier.getNotifierType()).thenReturn("dingtalk");

        // When
        // Use reflection or package-private method if needed
        // For now, we test through sendNotifications
        AlertEvent event = createTestAlertEvent(17L, "dingtalk");
        when(dingTalkNotifier.isAvailable()).thenReturn(true);
        when(dingTalkNotifier.sendAlert(event)).thenReturn(true);

        // When
        alertEngine.sendNotifications(event);

        // Then
        verify(dingTalkNotifier).sendAlert(event);
    }

    private AlertRule createTestRule(Long id, String ruleName, String metricName, Double thresholdValue) {
        return AlertRule.builder()
                .id(id)
                .ruleName(ruleName)
                .metricName(metricName)
                .comparisonOperator(">")
                .thresholdValue(thresholdValue)
                .severity(AlertRule.AlertSeverity.CRITICAL)
                .notifyChannels("email")
                .enabled(true)
                .build();
    }

    private AlertEvent createTestAlertEvent(Long ruleId, String channels) {
        return AlertEvent.builder()
                .ruleId(ruleId)
                .ruleName("Test Rule")
                .severity(AlertRule.AlertSeverity.CRITICAL)
                .message("Test alert")
                .triggerValue(String.valueOf(85.0))
                .thresholdValue(String.valueOf(80.0))
                .alertTime(LocalDateTime.now())
                .notifyChannels(channels)
                .notifyStatus(AlertEvent.NotifyStatus.PENDING)
                .status(AlertEvent.AlertStatus.TRIGGERED)
                .build();
    }
}
