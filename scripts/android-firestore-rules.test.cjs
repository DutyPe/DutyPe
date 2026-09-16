const { before, test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const project = 'demo-dutype-android-fixes';
const origin = 'http://127.0.0.1:8185';
const documents = `/v1/projects/${project}/databases/(default)/documents`;

function auth(uid, claims = {}) {
  const encode = value => Buffer.from(JSON.stringify(value)).toString('base64url');
  const now = Math.floor(Date.now() / 1000);
  return `${encode({ alg: 'none', typ: 'JWT' })}.${encode({
    sub: uid, user_id: uid, aud: project, iss: `https://securetoken.google.com/${project}`,
    iat: now, exp: now + 3600, email: 'test@example.invalid', email_verified: false,
    firebase: { sign_in_provider: 'password', identities: {} }, ...claims
  })}.`;
}

function value(data) {
  if (data === null) return { nullValue: null };
  if (typeof data === 'string') return { stringValue: data };
  if (typeof data === 'boolean') return { booleanValue: data };
  if (typeof data === 'number') return { integerValue: String(data) };
  if (Array.isArray(data)) return { arrayValue: { values: data.map(value) } };
  return { mapValue: { fields: fields(data) } };
}

function fields(data) {
  return Object.fromEntries(Object.entries(data).map(([key, entry]) => [key, value(entry)]));
}

async function request(method, endpoint, body, token) {
  const response = await fetch(origin + endpoint, {
    method, headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: body === undefined ? undefined : JSON.stringify(body),
    signal: AbortSignal.timeout(20000), redirect: 'error'
  });
  return { status: response.status, body: await response.json() };
}

function set(document, data, token = 'owner') {
  return request('PATCH', `${documents}/${document}`, { fields: fields(data) }, token);
}

function update(document, data, token, mask = Object.keys(data)) {
  const query = mask.map(key => `updateMask.fieldPaths=${encodeURIComponent(key)}`).join('&');
  return request('PATCH', `${documents}/${document}?${query}`, { fields: fields(data) }, token);
}

async function expectStatus(operation, expected) {
  const result = await operation;
  assert.equal(result.status, expected, JSON.stringify(result.body));
  return result.body;
}

function list(collection, token, userId) {
  return request('POST', `${documents}:runQuery`, { structuredQuery: {
    from: [{ collectionId: collection }],
    ...(userId ? { where: { fieldFilter: { field: { fieldPath: 'userId' }, op: 'EQUAL', value: value(userId) } } } : {})
  } }, token);
}

before(async () => {
  await expectStatus(request('DELETE', `/emulator/v1/projects/${project}/databases/(default)/documents`), 200);
  const rules = fs.readFileSync(path.resolve(__dirname, '../firestore.rules'), 'utf8');
  await expectStatus(request('PUT', `/emulator/v1/projects/${project}:securityRules`, {
    rules: { files: [{ name: 'firestore.rules', content: rules }] }
  }), 200);
  for (const [document, data] of Object.entries({
    'users/worker': { roles: ['WORKER'], activeRole: 'WORKER', fullName: 'Worker', referralCode: 'DUTYTEST', referralStats: { availableBalance: 100, successfulReferrals: 5, canWithdraw: true, isBlocked: true, freeJobPostings: 3 } },
    'users/employer': { roles: ['EMPLOYER'], activeRole: 'EMPLOYER' },
    'public_profiles/worker': { id: 'worker', fullName: 'Public Worker', skills: 'Cooking' },
    'jobs/job': { employerId: 'employer', title: 'Test job', vacancies: 2 },
    'job_applications/application': { workerId: 'worker', employerId: 'employer', jobId: 'job', status: 'ACCEPTED', verificationCode: 'DTP-AAAAAA' },
    'withdrawal_requests/withdrawal': { userId: 'worker', amount: 100 },
    'referrals/referral': { referrerUserId: 'worker', referredUserId: 'employer' }
  })) await expectStatus(set(document, data), 200);
});

test('owner can edit profile without changing server-owned referral data', async () => {
  await expectStatus(update('users/worker', { fullName: 'Updated Worker' }, auth('worker')), 200);
  await expectStatus(update('users/employer', { referralStats: { lastUpdated: 1 } }, auth('employer')), 200);
});

test('user cannot mint or remove financial balances, eligibility, or fraud blocks', async () => {
  for (const referralStats of [{ availableBalance: 99999 }, {}, { successfulReferrals: 5, canWithdraw: true, isBlocked: false }]) {
    await expectStatus(update('users/worker', { referralStats }, auth('worker')), 403);
  }
  await expectStatus(set('users/new-worker', { referralStats: { availableBalance: 1000 } }, auth('new-worker')), 403);
  await expectStatus(set('users/new-worker', { fullName: 'New Worker' }, auth('new-worker')), 200);
});

test('referral codes cannot be assigned, removed, or replaced by clients', async () => {
  await expectStatus(update('users/worker', {}, auth('worker'), ['referralCode']), 403);
  await expectStatus(update('users/worker', { referralCode: 'REPLACED' }, auth('worker')), 403);
  await expectStatus(update('users/employer', { referralCode: 'INVENTED' }, auth('employer')), 403);
});

test('existing free postings may be consumed but never increased by a client', async () => {
  await expectStatus(update('users/worker', { 'referralStats': { availableBalance: 100, successfulReferrals: 5, canWithdraw: true, isBlocked: true, freeJobPostings: 2 } }, auth('worker')), 200);
  await expectStatus(update('users/worker', { 'referralStats': { availableBalance: 100, successfulReferrals: 5, canWithdraw: true, isBlocked: true, freeJobPostings: 999 } }, auth('worker')), 403);
});

test('job ownership cannot be claimed or transferred by rewriting employerId', async () => {
  await expectStatus(update('jobs/job', { employerId: 'attacker', title: 'Replacement' }, auth('attacker')), 403);
  await expectStatus(update('jobs/job', { employerId: 'attacker' }, auth('employer')), 403);
  await expectStatus(update('jobs/job', { title: 'Legitimate edit' }, auth('employer')), 200);
  await expectStatus(set('jobs/own-job', { employerId: 'employer', title: 'New job' }, auth('employer')), 200);
});

test('application ownership cannot be taken over and valid partial edits preserve verification', async () => {
  await expectStatus(update('job_applications/application', { workerId: 'attacker', status: 'COMPLETED' }, auth('attacker')), 403);
  await expectStatus(update('job_applications/application', { employerId: 'attacker' }, auth('worker')), 403);
  const updated = await expectStatus(update('job_applications/application', { updatedAt: 2 }, auth('employer')), 200);
  assert.equal(updated.fields.verificationCode.stringValue, 'DTP-AAAAAA');
});

test('applications start pending and must match the existing job employer', async () => {
  await expectStatus(set('job_applications/new', { workerId: 'worker', employerId: 'employer', jobId: 'job', status: 'PENDING' }, auth('worker')), 200);
  await expectStatus(set('job_applications/forged', { workerId: 'worker', employerId: 'attacker', jobId: 'job', status: 'PENDING' }, auth('worker')), 403);
  await expectStatus(set('job_applications/completed', { workerId: 'worker', employerId: 'employer', jobId: 'job', status: 'COMPLETED' }, auth('worker')), 403);
});

test('worker may withdraw pending work and generate a pending code, but cannot self-verify or self-complete', async () => {
  await expectStatus(update('job_applications/new', { status: 'WITHDRAWN', updatedAt: 3 }, auth('worker')), 200);
  for (const status of ['ACCEPTED', 'IN_PROGRESS', 'COMPLETED']) {
    await expectStatus(update('job_applications/new', { status }, auth('worker')), 403);
  }
  const verification = {
    applicationId: 'application', workerId: 'worker', employerId: 'employer', jobId: 'job',
    status: 'PENDING', verificationCode: 'DTP-BBBBBB'
  };
  await expectStatus(update('job_applications/application', {
    verification, verificationCode: 'DTP-BBBBBB', verificationStatus: 'PENDING'
  }, auth('worker')), 200);
  await expectStatus(update('job_applications/application', {
    status: 'IN_PROGRESS', verificationStatus: 'VERIFIED'
  }, auth('worker')), 403);
  await expectStatus(update('job_applications/application', {
    verification: { ...verification, workerId: 'attacker' }
  }, auth('worker')), 403);
  await expectStatus(update('job_applications/application', { status: 'IN_PROGRESS', verificationStatus: 'VERIFIED' }, auth('employer')), 200);
});

test('corporate email alone is not administrative authority', async () => {
  for (const verified of [false, true]) {
    await expectStatus(set('announcements/forged', { title: 'Fake' }, auth('attacker', { email: 'admin@dutype.com', email_verified: verified })), 403);
  }
  await expectStatus(set('announcements/authorized', { title: 'Authorized' }, auth('admin', { admin: true })), 200);
});

test('withdrawal lists require ownership or a trusted admin claim', async () => {
  await expectStatus(list('withdrawal_requests', auth('attacker')), 403);
  await expectStatus(list('withdrawal_requests', auth('worker'), 'worker'), 200);
  await expectStatus(list('withdrawal_requests', auth('admin', { admin: true })), 200);
});

test('employers cannot rewind started work or accept a withdrawn application', async () => {
  await expectStatus(update('job_applications/application', { status: 'PENDING' }, auth('employer')), 403);
  await expectStatus(update('job_applications/new', { status: 'ACCEPTED' }, auth('employer')), 403);
  await expectStatus(update('job_applications/application', { status: 'COMPLETED' }, auth('employer')), 200);
  await expectStatus(update('job_applications/application', { status: 'ACCEPTED' }, auth('employer')), 403);
});

test('unrelated users cannot enumerate referral history', async () => {
  await expectStatus(list('referrals', auth('attacker')), 403);
});

test('private profiles reject anonymous bounded queries and unrelated authenticated reads', async () => {
  await expectStatus(request('GET', `${documents}/users/worker`), 403);
  await expectStatus(request('GET', `${documents}/users/worker`, undefined, auth('attacker')), 403);
  await expectStatus(request('POST', `${documents}:runQuery`, { structuredQuery: { from: [{ collectionId: 'users' }], limit: 2 } }), 403);
  await expectStatus(request('GET', `${documents}/users/worker`, undefined, auth('worker')), 200);
  await expectStatus(request('GET', `${documents}/public_profiles/worker`), 200);
  await expectStatus(update('public_profiles/worker', { phone: 'private' }, auth('worker')), 403);
});

test('applications can be read only by participants or trusted admins', async () => {
  await expectStatus(request('GET', `${documents}/job_applications/application`, undefined, auth('attacker')), 403);
  await expectStatus(list('job_applications', auth('attacker')), 403);
  for (const userId of ['worker', 'employer']) {
    await expectStatus(request('GET', `${documents}/job_applications/application`, undefined, auth(userId)), 200);
  }
});

test('direct review writes and profile rating inflation are denied', async () => {
  await expectStatus(set('ratings/fabricated', { raterId: 'worker', rating: 5, applicationId: 'application' }, auth('worker')), 403);
  await expectStatus(update('users/worker', { averageRating: 5, totalRatings: 999 }, auth('worker')), 403);
  await expectStatus(set('users/inflated', { fullName: 'New', averageRating: 5 }, auth('inflated')), 403);
});

test('notifications must be server-created and recipients can only mark them read', async () => {
  await expectStatus(set('notifications/forged', { recipientId: 'employer', title: 'Fake' }, auth('worker')), 403);
  await expectStatus(set('notifications/own-forged', { recipientId: 'worker', title: 'Fake' }, auth('worker')), 403);
  await expectStatus(set('notifications/real', { recipientId: 'worker', title: 'Server title', isRead: false }), 200);
  await expectStatus(update('notifications/real', { isRead: true }, auth('worker')), 200);
  await expectStatus(update('notifications/real', { title: 'Modified' }, auth('worker')), 403);
  await expectStatus(update('notifications/real', { recipientId: 'employer' }, auth('worker')), 403);
});

test('the Android OR-participant job query works without reopening global application access', async () => {
  for (const userId of ['worker', 'employer', 'attacker']) {
    const equal = (fieldPath, fieldValue) => ({ fieldFilter: { field: { fieldPath }, op: 'EQUAL', value: value(fieldValue) } });
    const result = await expectStatus(request('POST', `${documents}:runQuery`, { structuredQuery: {
      from: [{ collectionId: 'job_applications' }],
      where: { compositeFilter: { op: 'AND', filters: [
        equal('jobId', 'job'),
        { compositeFilter: { op: 'OR', filters: [equal('workerId', userId), equal('employerId', userId)] } }
      ] } }, limit: 200
    } }, auth(userId)), 200);
    assert.equal(result.filter(entry => entry.document).length, userId === 'attacker' ? 0 : 2);
  }
});

test('phone-role mappings require the matching verified phone identity', async () => {
  const phone = '9000000000';
  await expectStatus(set(`phone_roles/${phone}`, { roles: ['WORKER'] }, auth('worker', { phone_number: '+919000000000' })), 200);
  await expectStatus(request('GET', `${documents}/phone_roles/${phone}`), 403);
  await expectStatus(request('GET', `${documents}/phone_roles/${phone}`, undefined, auth('attacker', { phone_number: '+919000000001' })), 403);
  await expectStatus(list('phone_roles', auth('worker', { phone_number: '+919000000000' })), 403);
});

test('clients cannot alter authoritative balances, daily withdrawal counters, or event receipts', async () => {
  for (const collection of ['referral_stats', 'withdrawal_daily', 'notification_events']) {
    await expectStatus(set(`${collection}/worker`, { userId: 'worker', amount: 0, availableBalance: 9999 }, auth('worker')), 403);
  }
});