# Messaging Topic and Schema Conventions

## Scope
This document defines RocketMQ topic naming and payload schema/versioning rules for BaseBackend messaging.

## Topic naming
- Pattern: `bb.<domain>.<event>.<version>`
  - `domain`: bounded context or service (e.g., `user`, `order`, `notify`)
  - `event`: business event (e.g., `created`, `updated`, `failed`)
  - `version`: semantic schema version (e.g., `v1`, `v2`)
- Examples:
  - `bb.user.created.v1`
  - `bb.order.payment_failed.v2`

## Tags and keys
- Tag should describe routing subtype, short and lowercase (e.g., `email`, `sms`, `retry`).
- Message key should be globally unique (e.g., `event_id`).

## Payload schema versioning
- Every payload must include:
  - `schema_version`: semantic version string (e.g., `v1`, `v1.1`)
  - `event_id`: globally unique identifier
  - `occurred_at`: ISO8601 timestamp
  - `producer`: service name
- Backward compatibility rules:
  - `v1.x` is backward compatible with `v1` consumers.
  - Breaking changes require a new major version and a new topic suffix (e.g., `v2`).
- Deprecation:
  - Keep old version topics active until all consumers are migrated and verified.

## Sample payload
```json
{
  "schema_version": "v1",
  "event_id": "evt_20260118_142500_001",
  "occurred_at": "2026-01-18T14:25:00+08:00",
  "producer": "basebackend-user-api",
  "data": {
    "user_id": 12345,
    "email": "user@example.com",
    "status": "ACTIVE"
  }
}
```

## References
- `basebackend-messaging/src/main/java/com/basebackend/messaging/config/MessagingProperties.java`
- `basebackend-messaging/src/main/java/com/basebackend/messaging/config/RocketMQConfig.java`
