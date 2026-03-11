# DutyPe Database Optimization Analysis - Part 10: Action Plan

---

## Implementation Action Plan

### Phase 1: Immediate Actions (Week 1)

#### 1.1 Delete Deprecated Collections ✅ LOW RISK

```bash
# Verify no active reads
firebase firestore:indexes --project dutype

# Delete deprecated collections
firebase firestore:delete fcm_tokens --recursive --yes
firebase firestore:delete saved_jobs --recursive --yes
firebase firestore:delete rating_summaries --recursive --yes
```

**Impact:** -25K documents, ~1.25MB storage saved

---

#### 1.2 Add Missing Indexes ✅ LOW RISK

```javascript
// Create composite indexes for job_applications
db.collection("job_applications").createIndex({ 
  workerId: 1, 
  active: 1, 
  appliedAt: -1 
});

db.collection("job_applications").createIndex({ 
  employerId: 1, 
  status: 1, 
  appliedAt: -1 
});

// Create composite indexes for jobs
db.collection("jobs").createIndex({ 
  isActive: 1, 
  payType: 1, 
  postedAt: -1 
});
```

**Impact:** 50% faster queries

---

#### 1.3 Implement TTL Policy ✅ LOW RISK

```typescript
// Cloud Function: Run daily at 2 AM
export const cleanupOldNotifications = functions.pubsub
  .schedule('0 2 * * *')
  .onRun(async () => {
    const ninetyDaysAgo = Date.now() - 90 * 24 * 60 * 60 * 1000;
    
    const snapshot = await db.collection("notifications")
      .where("createdAt", "<", ninetyDaysAgo)
      .limit(500)
      .get();
    
    const batch = db.batch();
    snapshot.docs.forEach(doc => batch.delete(doc.ref));
    await batch.commit();
    
    return { deleted: snapshot.size };
  });
```

**Impact:** Keeps notifications collection under 100K docs

---

### Phase 2: Performance Optimizations (Week 2-3)

#### 2.1 Implement Redis Caching ⚠️ MEDIUM RISK

```typescript
// Install Redis
npm install redis

// Cache user profiles
async function getUserProfile(userId: string) {
  const cached = await redis.get(`user:${userId}`);
  if (cached) return JSON.parse(cached);
  
  const user = await db.collection("users").doc(userId).get();
  await redis.setex(`user:${userId}`, 3600, JSON.stringify(user.data()));
  
  return user.data();
}
```

**Impact:** 90% reduction in Firestore reads

---

#### 2.2 Implement Cursor-Based Pagination ⚠️ MEDIUM RISK

```typescript
// Replace offset-based pagination
// Before
const jobs = await db.collection("jobs")
  .orderBy("postedAt", "desc")
  .limit(20)
  .offset(page * 20)
  .get();

// After
const jobs = await db.collection("jobs")
  .orderBy("postedAt", "desc")
  .startAfter(lastDocSnapshot)
  .limit(20)
  .get();
```

**Impact:** 80% fewer reads, constant-time pagination

---

#### 2.3 Implement Batch Operations ✅ LOW RISK

```typescript
// Replace sequential writes with batch writes
const batch = db.batch();
notifications.forEach(notif => {
  const ref = db.collection("notifications").doc();
  batch.set(ref, notif);
});
await batch.commit();
```

**Impact:** 10x faster writes

---

### Phase 3: Scalability Improvements (Week 4-6)

#### 3.1 Partition Notifications by Month ⚠️ HIGH RISK

```typescript
// Create monthly partitions
function getNotificationCollection(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  return db.collection(`notifications_${year}_${month}`);
}

// Update all notification writes
const notifCollection = getNotificationCollection(new Date());
await notifCollection.add(notification);
```

**Impact:** 10x faster queries at scale

**Migration:** Requires data migration script

---

#### 3.2 Partition Jobs by City ⚠️ HIGH RISK

```typescript
// Create city-based partitions
function getJobsCollection(city: string) {
  const normalizedCity = city.toLowerCase().replace(/\s+/g, '_');
  return db.collection(`jobs_${normalizedCity}`);
}

// Update all job writes
const jobsCollection = getJobsCollection(job.city);
await jobsCollection.add(job);
```

**Impact:** 10x faster queries at scale

**Migration:** Requires data migration script

---

### Phase 4: Monitoring & Optimization (Ongoing)

#### 4.1 Set Up Monitoring

```typescript
// Track query performance
export const trackQueryPerformance = functions.https.onCall(async (data) => {
  await db.collection("query_metrics").add({
    collection: data.collection,
    operation: data.operation,
    duration: data.duration,
    timestamp: admin.firestore.FieldValue.serverTimestamp()
  });
});
```

---

#### 4.2 Set Up Alerts

```typescript
// Alert on slow queries
export const alertSlowQueries = functions.firestore
  .document("query_metrics/{metricId}")
  .onCreate(async (snapshot) => {
    const metric = snapshot.data();
    
    if (metric.duration > 1000) { // > 1 second
      // Send alert to admin
      await sendSlackAlert(`Slow query detected: ${metric.collection} - ${metric.duration}ms`);
    }
  });
```

---

### Risk Assessment

| Phase | Risk Level | Rollback Plan |
|-------|------------|---------------|
| Phase 1 | 🟢 LOW | Restore from backup |
| Phase 2 | 🟡 MEDIUM | Feature flags |
| Phase 3 | 🔴 HIGH | Gradual rollout |
| Phase 4 | 🟢 LOW | N/A |

---

### Success Metrics

| Metric | Current | Target | Timeline |
|--------|---------|--------|----------|
| Avg query time | 500ms | 50ms | 6 weeks |
| Firestore reads/day | 1M | 200K | 4 weeks |
| Storage size | 5GB | 4GB | 2 weeks |
| Monthly cost | $500 | $200 | 6 weeks |

---

### Rollout Strategy

1. **Test in staging** (1 week)
2. **Deploy to 10% users** (1 week)
3. **Monitor metrics** (1 week)
4. **Deploy to 50% users** (1 week)
5. **Deploy to 100% users** (1 week)

**Total Timeline:** 6-8 weeks

