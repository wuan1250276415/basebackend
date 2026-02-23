package com.basebackend.common.audit;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class AuditEvent extends ApplicationEvent {

    private final String eventId;
    private final String module;
    private final String action;
    private final String description;
    private final String operator;
    private final String operatorIp;
    private final String params;
    private final String result;
    private final LocalDateTime auditTime;
    private final Long duration;

    private AuditEvent(Object source, Builder builder) {
        super(source);
        this.eventId = UUID.randomUUID().toString();
        this.module = builder.module;
        this.action = builder.action;
        this.description = builder.description;
        this.operator = builder.operator;
        this.operatorIp = builder.operatorIp;
        this.params = builder.params;
        this.result = builder.result;
        this.auditTime = LocalDateTime.now();
        this.duration = builder.duration;
    }

    public static Builder builder(Object source) {
        return new Builder(source);
    }

    public static class Builder {
        private final Object source;
        private String module;
        private String action;
        private String description;
        private String operator;
        private String operatorIp;
        private String params;
        private String result;
        private Long duration;

        private Builder(Object source) {
            this.source = source;
        }

        public Builder module(String module) { this.module = module; return this; }
        public Builder action(String action) { this.action = action; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder operator(String operator) { this.operator = operator; return this; }
        public Builder operatorIp(String operatorIp) { this.operatorIp = operatorIp; return this; }
        public Builder params(String params) { this.params = params; return this; }
        public Builder result(String result) { this.result = result; return this; }
        public Builder duration(Long duration) { this.duration = duration; return this; }

        public AuditEvent build() {
            return new AuditEvent(source, this);
        }
    }
}
