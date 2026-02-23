# Database Schema Governance

## Purpose
Define ownership, approval, and migration workflow for schema changes in BaseBackend.

## Ownership
- Primary owner: database module maintainers (basebackend-database).
- Service owners are responsible for schema changes in their bounded context.
- Any shared schema changes require cross-service review.

## Change workflow
1. Proposal
   - Document the change intent, impact, and rollback strategy.
   - Attach migration plan and data backfill notes (if any).
2. Review and approval
   - Schema changes must be reviewed by database maintainers.
   - Breaking changes require sign-off from impacted service owners.
3. Migration implementation
   - Use Flyway for all schema changes.
   - Each migration includes forward and rollback considerations.
4. Verification
   - Validate migration in non-production environment.
   - Confirm data integrity checks and application startup.
5. Release
   - Apply migrations with backup enabled and confirmation gate in production.

## Required checklist
- [ ] Migration script reviewed and approved
- [ ] Backup and rollback plan documented
- [ ] Impacted services notified
- [ ] Data backfill steps documented (if needed)
- [ ] Production confirmation token flow verified

## References
- `basebackend-database/docs/MIGRATION_FAILURE_HANDLING.md`
- `basebackend-database/docs/FLYWAY_MIGRATION_GUIDE.md`
