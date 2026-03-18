# Messaging Management Smoke Runbook

## Scope
This runbook verifies the management endpoints exposed from `basebackend-system-api`
and forwarded by `basebackend-gateway`.

## Preconditions
- `basebackend-system-api` has `messaging.management.enabled=true`
- `basebackend-gateway` contains `/messaging/** -> lb://basebackend-system-api`
- External dependencies such as Nacos, MySQL, and Redis are reachable

## Direct system-api smoke checks
Use the service port directly when you only need to verify controller and data access:

```bash
curl -H "Authorization: Bearer <token>" \
  http://127.0.0.1:18082/messaging/monitor/statistics

curl -H "Authorization: Bearer <token>" \
  "http://127.0.0.1:18082/messaging/webhook/page?page=1&size=10"

curl -H "Authorization: Bearer <token>" \
  "http://127.0.0.1:18082/messaging/dead-letter/page?page=1&size=10"

curl -H "Authorization: Bearer <token>" \
  "http://127.0.0.1:18082/messaging/webhook-log/page?page=1&size=10"
```

## Gateway smoke checks
Use the gateway port when you need to verify routing and gateway authentication together:

```bash
curl -H "Authorization: Bearer <token>" \
  http://127.0.0.1:18080/messaging/monitor/statistics

curl -H "Authorization: Bearer <token>" \
  "http://127.0.0.1:18080/messaging/webhook/page?page=1&size=10"

curl -H "Authorization: Bearer <token>" \
  "http://127.0.0.1:18080/messaging/dead-letter/page?page=1&size=10"

curl -H "Authorization: Bearer <token>" \
  "http://127.0.0.1:18080/messaging/webhook-log/page?page=1&size=10"
```

## Authentication note
- Preferred: obtain the bearer token through the normal login flow.
- If you need to bypass login for local debugging, ensure Redis key
  `login_tokens:{userId}` stores the same token value expected by the gateway.
  The current gateway `ReactiveRedisTemplate` uses JSON serialization for values,
  so manually written test data should match that format.
