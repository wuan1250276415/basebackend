package com.basebackend.common.audit;

public interface AuditEventListener {

    void onAuditEvent(AuditEvent event);
}
