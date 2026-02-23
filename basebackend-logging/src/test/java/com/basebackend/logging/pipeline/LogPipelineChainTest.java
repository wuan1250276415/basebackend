package com.basebackend.logging.pipeline;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LogPipelineChain 日志管道链测试")
class LogPipelineChainTest {

    private static LogTransport.LogEvent sampleEvent() {
        return new LogTransport.LogEvent(
                "2025-12-10T00:00:00Z", "INFO", "com.example.Test",
                "hello", "main", "trace-001", Map.of(), null);
    }

    @Test
    @DisplayName("无处理器时应直接发送到传输后端")
    void execute_sendsDirectly_whenNoProcessors() {
        AtomicInteger sent = new AtomicInteger();
        LogTransport transport = new LogTransport() {
            @Override
            public void send(LogEvent event) {
                sent.incrementAndGet();
            }

            @Override
            public boolean isAvailable() {
                return true;
            }
        };

        LogPipelineChain chain = new LogPipelineChain(Collections.emptyList(), transport);
        chain.execute(sampleEvent());
        assertThat(sent.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("处理器应按顺序执行")
    void execute_processorsInOrder() {
        StringBuilder order = new StringBuilder();
        LogPipelineProcessor p1 = event -> {
            order.append("1");
            return event;
        };
        LogPipelineProcessor p2 = event -> {
            order.append("2");
            return event;
        };

        AtomicInteger sent = new AtomicInteger();
        LogTransport transport = new LogTransport() {
            @Override
            public void send(LogEvent event) {
                sent.incrementAndGet();
            }

            @Override
            public boolean isAvailable() {
                return true;
            }
        };

        LogPipelineChain chain = new LogPipelineChain(List.of(p1, p2), transport);
        chain.execute(sampleEvent());
        assertThat(order.toString()).isEqualTo("12");
        assertThat(sent.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("处理器返回 null 时应中止链传递")
    void execute_stopsChain_whenProcessorReturnsNull() {
        LogPipelineProcessor filter = event -> null;
        LogPipelineProcessor shouldNotRun = event -> {
            throw new RuntimeException("should not be called");
        };

        AtomicInteger sent = new AtomicInteger();
        LogTransport transport = new LogTransport() {
            @Override
            public void send(LogEvent event) {
                sent.incrementAndGet();
            }

            @Override
            public boolean isAvailable() {
                return true;
            }
        };

        LogPipelineChain chain = new LogPipelineChain(List.of(filter, shouldNotRun), transport);
        chain.execute(sampleEvent());
        assertThat(sent.get()).isZero();
    }

    @Test
    @DisplayName("传输失败时不应抛出异常")
    void execute_handlesTransportFailure() {
        LogTransport failingTransport = new LogTransport() {
            @Override
            public void send(LogEvent event) throws TransportException {
                throw new TransportException("connection refused");
            }

            @Override
            public boolean isAvailable() {
                return false;
            }
        };

        LogPipelineChain chain = new LogPipelineChain(Collections.emptyList(), failingTransport);
        // should not throw
        chain.execute(sampleEvent());
    }

    @Test
    @DisplayName("getProcessorCount 应返回处理器数量")
    void getProcessorCount_returnsCorrectCount() {
        LogPipelineProcessor p1 = event -> event;
        LogPipelineProcessor p2 = event -> event;
        LogTransport transport = new ConsoleLogTransport();

        LogPipelineChain chain = new LogPipelineChain(List.of(p1, p2), transport);
        assertThat(chain.getProcessorCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("ConsoleLogTransport 应始终可用")
    void consoleTransport_isAlwaysAvailable() {
        ConsoleLogTransport transport = new ConsoleLogTransport();
        assertThat(transport.isAvailable()).isTrue();
    }
}
