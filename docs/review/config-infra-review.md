# Config and Infrastructure Review

## Config sources and precedence
- Nacos configs (primary): `config/nacos-configs/` (dev profiles) and `config/nacos/` (service-specific configs)
- Local configs: module `application.yml` / `bootstrap.yml` files (e.g., `basebackend-gateway/src/main/resources/bootstrap.yml`)
- Docker Compose environment files: `docker/compose/env/.env.example`, `docker/compose/env/.env.dev`
- Sentinel rules: `sentinel-rules/*.json`
- Prometheus config: `config/prometheus/`

Note: `config/nacos/README.md` documents precedence as local `application.yml` > Nacos service config > Nacos common config.

## Dependency inventory (owner + environment notes)
| Dependency | Config source | Required env vars | Owner | Environment notes |
| --- | --- | --- | --- | --- |
| MySQL | `docker/compose/base/docker-compose.base.yml`, `config/nacos-configs/dev/mysql-config.yml` | `MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_DATABASE`, `MYSQL_USERNAME`, `MYSQL_PASSWORD`; Docker also uses `MYSQL_ROOT_PASSWORD`, `MYSQL_USER` | TBD | Dev uses Docker Compose; prod typically external managed DB |
| Redis | `docker/compose/base/docker-compose.base.yml`, `config/nacos-configs/dev/redis-config.yml` | `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`, `REDIS_DATABASE` | TBD | Dev uses Docker Compose; prod external managed cache |
| RocketMQ | `docker/compose/middleware/docker-compose.middleware.yml`, `config/nacos-configs/dev/rocketmq-config.yml` | `ROCKETMQ_NAME_SERVER` | TBD | Dev uses Docker Compose; prod requires broker cluster |
| Nacos | `docker/compose/middleware/docker-compose.middleware.yml`, `basebackend-gateway/src/main/resources/bootstrap.yml` | `NACOS_CONFIG_SERVER`, `NACOS_CONFIG_NAMESPACE`, `NACOS_CONFIG_GROUP`, `NACOS_CONFIG_USERNAME`, `NACOS_CONFIG_PASSWORD` | TBD | Required for config center in all envs |
| Seata | `config/nacos-configs/dev/seata-config.yml` | `SEATA_ENABLED`, `SEATA_SERVER_ADDR` | TBD | Disabled by default; enable for distributed TX |
| Prometheus | `config/prometheus/prometheus.yml` | N/A | TBD | Metrics scrape for observability stack |
| Sentinel | `sentinel-rules/*.json` | N/A | TBD | Gateway flow/degrade rules for rate limiting |

## Mismatches / risks
- Nacos config docs and Docker Compose use different MySQL variable names (`MYSQL_PASSWORD` vs `MYSQL_ROOT_PASSWORD`/`MYSQL_USER`). Ensure `.env` files define both or document a single source of truth.
- Two Nacos config directories exist (`config/nacos-configs/` and `config/nacos/`). Clarify which is authoritative and ensure upload scripts match.
- `basebackend-gateway` bootstrap Nacos vars (`NACOS_CONFIG_*`) must be present in runtime env; confirm docker env templates include them.

## Checklist
- [x] Config sources enumerated with precedence
- [x] Infra dependencies listed with env vars
- [x] Sentinel rules identified for gateway throttling
- [x] Mismatch/risks recorded
