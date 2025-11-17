package com.basebackend.common.tracing;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ServiceNameConfig;
import io.opentelemetry.sdk.trace.Sampler;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * OpenTelemetry 链路追踪配置
 * 为应用提供分布式链路追踪能力
 *
 * 功能特性:
 * 1. 自动创建 Span 跟踪方法执行
 * 2. 记录 HTTP 请求链路
 * 3. 数据库查询链路追踪
 * 4. Feign 调用链路追踪
 * 5. 自定义业务埋点
 * 6. 链路追踪数据导出到 Jaeger
 *
 * @author basebackend team
 * @version 1.0
 */
@Configuration
public class TracingConfig {

    @Value("${otel.exporter.jaeger.endpoint:http://jaeger-collector.observability.svc.cluster.local:14250}")
    private String jaegerEndpoint;

    @Value("${otel.service.name:basebackend-service}")
    private String serviceName;

    @Value("${otel.exporter.jaeger.timeout:10}")
    private long timeout;

    /**
     * 配置 OpenTelemetry
     */
    @Bean
    public OpenTelemetry openTelemetry() {
        // 1. 配置资源信息
        Resource serviceNameResource = Resource.create(
            Attributes.of(
                ResourceAttributes.SERVICE_NAME, serviceName,
                ResourceAttributes.DEPLOYMENT_ENVIRONMENT, "production",
                ResourceAttributes.SERVICE_VERSION, "1.0.0"
            )
        );

        // 2. 配置 Jaeger 导出器
        JaegerGrpcSpanExporter jaegerExporter = JaegerGrpcSpanExporter.builder()
            .setEndpoint(jaegerEndpoint)
            .setTimeout(timeout, TimeUnit.SECONDS)
            .build();

        // 3. 配置批处理器
        BatchSpanProcessor batchProcessor = BatchSpanProcessor.builder(jaegerExporter)
            .setMaxExportBatchSize(512)
            .setExportTimeout(2, TimeUnit.SECONDS)
            .setMaxQueueSize(2048)
            .build();

        // 4. 构建 TracerProvider
        io.opentelemetry.sdk.trace.TracerProvider tracerProvider = io.opentelemetry.sdk.trace.TracerProvider.builder()
            .setSampler(Sampler.traceIdRatioBased(0.1)) // 10% 采样率
            .addSpanProcessor(batchProcessor)
            .setResource(serviceNameResource)
            .build();

        // 5. 注册 TracerProvider
        io.opentelemetry.api.trace.TracerProvider.set(tracerProvider);

        // 6. 返回 OpenTelemetry 实例
        return OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .build();
    }

    /**
     * 获取 Tracer 实例
     */
    @Bean
    public Tracer tracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer("basebackend.tracing", "1.0.0");
    }

    /**
     * 测试链路追踪的 REST 控制器
     */
    @RestController
    public static class TracingTestController {

        private final Tracer tracer;

        public TracingTestController(Tracer tracer) {
            this.tracer = tracer;
        }

        /**
         * 测试基本链路追踪功能
         */
        @GetMapping("/trace/test")
        public String testTracing() {
            // 创建根 Span
            Span rootSpan = tracer.spanBuilder("root.operation")
                .setSpanKind(SpanKind.INTERNAL)
                .setAttribute("operation.type", "test")
                .setAttribute("user.id", "12345")
                .startSpan();

            try (Scope scope = rootSpan.makeCurrent()) {
                rootSpan.addEvent("Starting root operation");

                // 调用子操作
                String result = performSubOperation();

                rootSpan.setAttribute("result.size", result.length());
                rootSpan.addEvent("Root operation completed");

                return result;
            } catch (Exception e) {
                rootSpan.recordException(e);
                rootSpan.setStatus(StatusCode.ERROR, e.getMessage());
                throw e;
            } finally {
                rootSpan.end();
            }
        }

        /**
         * 测试 HTTP 请求链路追踪
         */
        @GetMapping("/trace/http")
        public String testHttpTracing() {
            Span httpSpan = tracer.spanBuilder("http.request")
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute("http.method", "GET")
                .setAttribute("http.url", "https://api.example.com/data")
                .setAttribute("http.status_code", 200)
                .startSpan();

            try (Scope scope = httpSpan.makeCurrent()) {
                httpSpan.addEvent("Sending HTTP request");

                // 模拟 HTTP 调用
                simulateHttpCall();

                httpSpan.setAttribute("http.response.size", 1024);
                httpSpan.addEvent("HTTP request completed");

                return "HTTP tracing test completed";
            } catch (Exception e) {
                httpSpan.recordException(e);
                httpSpan.setStatus(StatusCode.ERROR, e.getMessage());
                throw e;
            } finally {
                httpSpan.end();
            }
        }

        /**
         * 测试数据库操作链路追踪
         */
        @GetMapping("/trace/database")
        public String testDatabaseTracing() {
            Span dbSpan = tracer.spanBuilder("database.query")
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute("db.system", "mysql")
                .setAttribute("db.connection_string", "mysql://localhost:3306/basebackend")
                .setAttribute("db.statement", "SELECT * FROM users WHERE id = ?")
                .startSpan();

            try (Scope scope = dbSpan.makeCurrent()) {
                dbSpan.addEvent("Executing database query");

                // 模拟数据库查询
                simulateDatabaseQuery();

                dbSpan.setAttribute("db.row_count", 10);
                dbSpan.addEvent("Database query completed");

                return "Database tracing test completed";
            } catch (Exception e) {
                dbSpan.recordException(e);
                dbSpan.setStatus(StatusCode.ERROR, e.getMessage());
                throw e;
            } finally {
                dbSpan.end();
            }
        }

        /**
         * 测试 Feign 调用链路追踪
         */
        @GetMapping("/trace/feign")
        public String testFeignTracing() {
            Span feignSpan = tracer.spanBuilder("feign.client.call")
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute("feign.service", "user-service")
                .setAttribute("feign.method", "getUserById")
                .setAttribute("feign.target", "http://user-service/api/users/")
                .startSpan();

            try (Scope scope = feignSpan.makeCurrent()) {
                feignSpan.addEvent("Making Feign call");

                // 模拟 Feign 调用
                simulateFeignCall();

                feignSpan.setAttribute("feign.response.status", 200);
                feignSpan.addEvent("Feign call completed");

                return "Feign tracing test completed";
            } catch (Exception e) {
                feignSpan.recordException(e);
                feignSpan.setStatus(StatusCode.ERROR, e.getMessage());
                throw e;
            } finally {
                feignSpan.end();
            }
        }

        private String performSubOperation() {
            Span subSpan = tracer.spanBuilder("sub.operation")
                .setSpanKind(SpanKind.INTERNAL)
                .setAttribute("operation.name", "performSubOperation")
                .startSpan();

            try (Scope scope = subSpan.makeCurrent()) {
                // 模拟处理时间
                Thread.sleep(100);
                subSpan.addEvent("Sub operation completed");
                return "Sub operation result";
            } catch (InterruptedException e) {
                subSpan.recordException(e);
                Thread.currentThread().interrupt();
                return "Error";
            } finally {
                subSpan.end();
            }
        }

        private void simulateHttpCall() {
            Span innerSpan = tracer.spanBuilder("internal.http.call")
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();
            try (Scope scope = innerSpan.makeCurrent()) {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                innerSpan.end();
            }
        }

        private void simulateDatabaseQuery() {
            Span innerSpan = tracer.spanBuilder("internal.db.query")
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();
            try (Scope scope = innerSpan.makeCurrent()) {
                Thread.sleep(30);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                innerSpan.end();
            }
        }

        private void simulateFeignCall() {
            Span innerSpan = tracer.spanBuilder("internal.feign.call")
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();
            try (Scope scope = innerSpan.makeCurrent()) {
                Thread.sleep(80);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                innerSpan.end();
            }
        }
    }
}
