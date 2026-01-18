# Data and Integration Boundary Review

## Scope
- Database: `basebackend-database`
- Messaging: `basebackend-messaging` (RocketMQ)
- File service: `basebackend-file-service`

## Boundary matrix
| Domain | Contract / schema signals | Retry / timeout strategy | Notes / gaps |
| --- | --- | --- | --- |
| Database | Flyway migration and backup/restore endpoints; enhanced database config and feature modules (audit, tenant, security, failover) | Migration supports auto-backup and auto-rollback; failover has retry interval in config guide | DB schema ownership and versioning policy not consolidated in a single contract doc |
| Messaging | JSON message converter (`MappingJackson2MessageConverter`); configuration properties for RocketMQ, retry, dead-letter, transaction; transaction compensation job | Retry, dead-letter, transaction compensation intervals are configurable | Topic naming conventions and payload schema/versioning not explicitly documented |
| File service | Upload request/response objects (`UploadRequest`, `UploadResult`); chunk upload APIs; multiple storage backends (Local/MinIO/OSS/S3) | Retry/timeout policy for storage adapters not explicitly documented; error handling uses BusinessException in review report | External storage timeout/retry defaults need explicit documentation; API error contract not consolidated |

## Contract gaps to address
- Define topic naming and payload schema/versioning for RocketMQ messages.
- Document file-service API error contract and storage adapter timeout/retry defaults.
- Document DB schema ownership and migration governance (who approves version changes).

## Sources
- `basebackend-database/docs/MIGRATION_FAILURE_HANDLING.md`
- `basebackend-database/docs/CONFIG_GUIDE.md`
- `basebackend-messaging/src/main/java/com/basebackend/messaging/config/RocketMQConfig.java`
- `basebackend-messaging/src/main/java/com/basebackend/messaging/config/MessagingProperties.java`
- `basebackend-messaging/src/main/java/com/basebackend/messaging/transaction/TransactionalMessageService.java`
- `basebackend-file-service/docs/PHASE1_COMPLETION_REPORT.md`
- `basebackend-file-service/docs/CODE_REVIEW_REPORT.md`
