const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const root = path.resolve(__dirname, '..');
const typescript = require(require.resolve('typescript', {
  paths: [path.join(root, 'functions'), root, path.join(root, 'web')]
}));

function source(filename) {
  return typescript.createSourceFile(filename,
    fs.readFileSync(path.join(root, filename), 'utf8'),
    typescript.ScriptTarget.Latest, true, typescript.ScriptKind.TS);
}

function findNode(tree, predicate) {
  let result;
  function visit(node) {
    if (predicate(node)) result = node;
    if (!result) typescript.forEachChild(node, visit);
  }
  visit(tree);
  assert.ok(result, 'The production code under test must exist');
  return result;
}

function compile(text) {
  const result = typescript.transpileModule(text, {
    reportDiagnostics: true,
    compilerOptions: { target: typescript.ScriptTarget.ES2020, module: typescript.ModuleKind.CommonJS }
  });
  assert.deepEqual(result.diagnostics.filter(diagnostic => diagnostic.category === typescript.DiagnosticCategory.Error), []);
  new vm.Script(result.outputText);
  return result.outputText;
}

test('changed backend modules have valid TypeScript syntax', () => {
  for (const filename of ['functions/src/index.ts', 'functions/src/scheduled-notifications.ts']) {
    compile(source(filename).text);
  }
});

test('event push payload binds the recipient and never delegates private rendering to Android system UI', () => {
  const tree = source('functions/src/index.ts');
  const declaration = findNode(tree, node => typescript.isVariableDeclaration(node) &&
    node.name.getText(tree) === 'message' && node.type?.getText(tree) === 'admin.messaging.Message');
  const context = {
    fcmToken: 'synthetic-token', recipientId: 'worker', notificationId: 'notification',
    notification: { title: 'Test title', message: 'Test body' },
    notificationType: 'APPLICATION_STATUS', deepLink: 'dutype://worker/home', channelId: 'high_priority'
  };
  const message = vm.runInNewContext(`(${declaration.initializer.getText(tree)})`, context);
  assert.equal(message.token, 'synthetic-token');
  assert.equal(message.data.recipientId, 'worker');
  assert.equal(message.data.body, 'Test body');
  assert.equal(message.data.channel, 'high_priority');
  assert.equal(message.notification, undefined);
  assert.equal(message.android.notification, undefined);
});

test('scheduled private pushes bind trusted recipient and use data-only delivery', async () => {
  const tree = source('functions/src/scheduled-notifications.ts');
  const declaration = findNode(tree, node => typescript.isFunctionDeclaration(node) &&
    node.name?.text === 'sendFCMNotification');
  const sent = [];
  const admin = {
    firestore: () => ({ collection: name => {
      assert.equal(name, 'users');
      return { doc: userId => {
        assert.equal(userId, 'worker');
        return { get: async () => ({ exists: true, data: () => ({ fcmToken: 'synthetic-token' }) }) };
      } };
    } }),
    messaging: () => ({ send: async message => { sent.push(message); return 'synthetic-id'; } })
  };
  const sandbox = { admin, console: { log() {}, error() {} }, exports: {} };
  vm.runInNewContext(compile(declaration.getText(tree)), sandbox);
  const success = await sandbox.sendFCMNotification('worker', {
    title: 'Test title', body: 'Test body', channel: 'high_priority', priority: 'high',
    data: { recipientId: 'wrong-user', title: 'Wrong title', type: 'REMINDER' }
  });
  assert.equal(success, true);
  assert.equal(sent.length, 1);
  assert.equal(sent[0].data.recipientId, 'worker');
  assert.equal(sent[0].data.title, 'Test title');
  assert.equal(sent[0].data.body, 'Test body');
  assert.equal(sent[0].notification, undefined);
  assert.equal(sent[0].android.notification, undefined);
});