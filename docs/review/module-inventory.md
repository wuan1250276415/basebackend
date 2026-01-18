# Module Inventory

## Sources
- Root pom: `pom.xml`
- Top-level directories: `basebackend-*`

## Root modules (from pom.xml)
- `basebackend-common`
- `basebackend-web`
- `basebackend-transaction`
- `basebackend-jwt`
- `basebackend-database`
- `basebackend-cache`
- `basebackend-logging`
- `basebackend-security`
- `basebackend-security-starter`
- `basebackend-observability`
- `basebackend-messaging`
- `basebackend-file-service`
- `basebackend-backup`
- `basebackend-nacos`
- `basebackend-feign-api`
- `basebackend-feature-toggle`
- `basebackend-gateway`
- `basebackend-scheduler-parent`
- `basebackend-code-generator`
- `basebackend-user-api`
- `basebackend-system-api`
- `basebackend-notification-service`
- `basebackend-observability-service`

## Top-level basebackend-* directories
- `basebackend-admin-api`
- `basebackend-admin-web`
- `basebackend-backup`
- `basebackend-cache`
- `basebackend-code-generator`
- `basebackend-common`
- `basebackend-database`
- `basebackend-feature-toggle`
- `basebackend-feign-api`
- `basebackend-file-service`
- `basebackend-gateway`
- `basebackend-jwt`
- `basebackend-logging`
- `basebackend-messaging`
- `basebackend-nacos`
- `basebackend-notification-service`
- `basebackend-observability`
- `basebackend-observability-service`
- `basebackend-scheduler`
- `basebackend-scheduler-parent`
- `basebackend-security`
- `basebackend-security-starter`
- `basebackend-system-api`
- `basebackend-transaction`
- `basebackend-user-api`
- `basebackend-web`

## Differences
- In pom but missing directory: None
- Directories not in pom: `basebackend-admin-api`, `basebackend-admin-web`, `basebackend-scheduler`
- Commented modules in pom: `basebackend-admin-api`

## Aggregator submodules (from module pom.xml)
- `basebackend-common`
  - `basebackend-common-core`
  - `basebackend-common-dto`
  - `basebackend-common-util`
  - `basebackend-common-context`
  - `basebackend-common-security`
  - `basebackend-common-starter`
  - `basebackend-common-storage`
- `basebackend-scheduler-parent`
  - `scheduler-core`
  - `scheduler-workflow`
  - `scheduler-processor`
  - `scheduler-metrics`
  - `scheduler-integration`

## Internal dependency map (groupId: `com.basebackend`)
(Only dependencies declared in module `pom.xml` are listed.)

- `basebackend-common` -> `basebackend-common-context`, `basebackend-common-core`, `basebackend-common-dto`, `basebackend-common-security`, `basebackend-common-starter`, `basebackend-common-storage`, `basebackend-common-util`
- `basebackend-web` -> `basebackend-cache`, `basebackend-common-starter`, `basebackend-observability`
- `basebackend-transaction` -> `basebackend-common-starter`
- `basebackend-jwt` -> `basebackend-common-security`, `basebackend-common-starter`
- `basebackend-database` -> `basebackend-common-starter`, `basebackend-observability`, `basebackend-security`
- `basebackend-cache` -> `basebackend-common-starter`, `basebackend-observability`
- `basebackend-logging` -> `basebackend-cache`, `basebackend-common-starter`
- `basebackend-security` -> `basebackend-common-core`, `basebackend-jwt`
- `basebackend-security-starter` -> `basebackend-common-starter`, `basebackend-jwt`, `basebackend-security`
- `basebackend-observability` -> `basebackend-common-core`
- `basebackend-messaging` -> `basebackend-cache`, `basebackend-common-core`, `basebackend-database`
- `basebackend-file-service` -> `basebackend-common-starter`, `basebackend-common-storage`, `basebackend-feign-api`
- `basebackend-backup` -> `basebackend-common-starter`, `basebackend-common-storage`, `basebackend-database`
- `basebackend-nacos` -> `basebackend-common-core`
- `basebackend-feign-api` -> `basebackend-common-core`, `basebackend-common-starter`
- `basebackend-feature-toggle` -> `basebackend-common-starter`
- `basebackend-gateway` -> `basebackend-common-core`, `basebackend-common-util`, `basebackend-jwt`, `basebackend-nacos`
- `basebackend-scheduler-parent` -> None
- `basebackend-code-generator` -> `basebackend-backup`, `basebackend-cache`, `basebackend-common-starter`, `basebackend-database`, `basebackend-feign-api`, `basebackend-logging`, `basebackend-nacos`, `basebackend-security`
- `basebackend-user-api` -> `basebackend-backup`, `basebackend-cache`, `basebackend-common-starter`, `basebackend-database`, `basebackend-feign-api`, `basebackend-logging`, `basebackend-messaging`, `basebackend-nacos`, `basebackend-observability`, `basebackend-security`, `basebackend-web`
- `basebackend-system-api` -> `basebackend-backup`, `basebackend-cache`, `basebackend-common-starter`, `basebackend-database`, `basebackend-feign-api`, `basebackend-file-service`, `basebackend-jwt`, `basebackend-logging`, `basebackend-messaging`, `basebackend-nacos`, `basebackend-observability`, `basebackend-security`, `basebackend-web`
- `basebackend-notification-service` -> `basebackend-backup`, `basebackend-cache`, `basebackend-common-starter`, `basebackend-database`, `basebackend-feign-api`, `basebackend-logging`, `basebackend-messaging`, `basebackend-nacos`, `basebackend-observability`, `basebackend-security`, `basebackend-web`
- `basebackend-observability-service` -> `basebackend-cache`, `basebackend-common-starter`, `basebackend-database`, `basebackend-logging`, `basebackend-observability`, `basebackend-security`, `basebackend-web`
