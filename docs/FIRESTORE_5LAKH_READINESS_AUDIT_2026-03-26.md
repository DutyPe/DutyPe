# Firestore 5L+ Readiness Audit

Date: 2026-03-26
Scope: Android service-layer query patterns, index coverage, and high-scale recommendations.

## Completed Improvements In This Pass

1. Notification fetch query hardened with server-side ordering:
- Query now uses `whereEqualTo(recipientId)` + `orderBy(createdAt DESC)` + `limit(50)`.
- File: `app/src/main/java/com/example/dutype/services/NotificationService.kt`

2. Notification mark-all-read made scale-safe:
- Processes unread docs in repeated 500-write batches until drained.
- File: `app/src/main/java/com/example/dutype/services/NotificationService.kt`

3. Missing indexes added for high-volume notification/application access:
- `notifications`: `(recipientId ASC, isRead ASC, createdAt DESC)`
- `notifications`: `(recipientId ASC, isRead ASC)`
- `applications`: `(employerId ASC, createdAt DESC)`
- File: `firestore.indexes.json`

4. Categories screen avoids unnecessary GPS refresh when cache is fresh (TTL guard).
- File: `app/src/main/java/com/example/dutype/worker/screens/CategoriesScreen.kt`

## Query/Index Coverage Snapshot

### Notifications
- Query: recipient notifications ordered by createdAt and capped.
- Index required: `(recipientId, createdAt DESC)`.
- Status: Covered in `firestore.indexes.json`.

- Query: unread count via aggregation (`where recipientId + isRead`).
- Index required: `(recipientId, isRead)`.
- Status: Added.

- Query: bulk unread updates (`where recipientId + isRead`, limited chunk reads).
- Index required: `(recipientId, isRead)`.
- Status: Added.

### Applications
- Common query pattern: employer applications sorted by createdAt.
- Index required: `(employerId, createdAt DESC)`.
- Status: Added.

- Existing index already present for worker timeline:
- `(workerId, createdAt DESC)`.

### Referrals
- Common query pattern: `where referrerId` + `orderBy createdAt DESC` + `limit`.
- Status: Covered.

### Jobs
- Geo + status + createdAt patterns are indexed.
- Status: Covered.

## Risk Areas Still To Address (Next Iteration)

1. Replace fixed large limits (e.g., 200/500) with cursor-based pagination for all list APIs:
- `JobApplicationService` and `ApplicationManagementService` still use large bounded reads in several methods.

2. Introduce server-maintained counters for frequently requested dashboard values:
- Some screens still derive stats via list scans after fetch.

3. Enforce read-time windows for hot feeds:
- Consider additional query filters by recency where business-safe.

4. Add request-level tracing and alerting:
- Firebase Performance traces for: home feed load, employer applications load, notifications load.

## Deployment Action Required

Run:

```bash
firebase deploy --only firestore:indexes
```

Without index deployment, new query patterns may fail or underperform in production.

## Suggested Next Engineering Step

Create a cursor-pagination contract for all high-volume list endpoints:
- `limit`
- `lastVisible`/cursor token
- `hasMore`
- deterministic order field

This is the key step for predictable performance at 5L+ active users.
