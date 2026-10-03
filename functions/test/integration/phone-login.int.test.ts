/**
 * WhatsApp OTP and Truecaller sign-in against the Firestore + Auth emulators. Meta and Truecaller
 * are stubbed (no network). Run: npm run test:integration (needs `--only firestore,auth`).
 */
import { after, beforeEach, describe, it } from "node:test";
import * as assert from "node:assert/strict";
import * as admin from "firebase-admin";
import functionsTest from "firebase-functions-test";

process.env.GCLOUD_PROJECT = "demo-dutype";
if (!process.env.FIRESTORE_EMULATOR_HOST) throw new Error("Run inside `firebase emulators:exec`");
if (!process.env.FIREBASE_AUTH_EMULATOR_HOST) throw new Error("Start the Auth emulator too (--only firestore,auth)");
if (!admin.apps.length) admin.initializeApp({ projectId: "demo-dutype" });
const fft = functionsTest({ projectId: "demo-dutype" });
/* eslint-disable @typescript-eslint/no-var-requires, @typescript-eslint/no-explicit-any */
const login = require("../../src/phone-login") as typeof import("../../src/phone-login");
const profiles = require("../../src/profiles") as typeof import("../../src/profiles");
const db = admin.firestore();
const app = { app: { appId: "t" } };
const call = (fn: unknown) => fft.wrap(fn as never) as unknown as (d: unknown, c: unknown) => Promise<any>;
const PHONE = "+919876543210";

/** Captured outgoing requests; responses are scripted per test. */
let sent: Array<{ url: string; body: string }> = [];
let respond: (url: string) => Response = () => new Response("{}", { status: 200 });
login.net.fetch = async (url: string, init?: RequestInit) => {
  sent.push({ url, body: String(init?.body ?? "") });
  return respond(url);
};
const sentCode = () => JSON.parse(sent.at(-1)!.body).template.components[0].parameters[0].text as string;

async function clear() {
  const fs = await fetch(`http://${process.env.FIRESTORE_EMULATOR_HOST}/emulator/v1/projects/demo-dutype/databases/(default)/documents`, { method: "DELETE" });
  const au = await fetch(`http://${process.env.FIREBASE_AUTH_EMULATOR_HOST}/emulator/v1/projects/demo-dutype/accounts`, { method: "DELETE" });
  assert.ok(fs.ok && au.ok);
}

function errorCode(e: unknown) {
  return (e as { code?: string }).code;
}

describe("WhatsApp OTP (emulator)", () => {
  beforeEach(async () => {
    await clear();
    sent = [];
    respond = () => new Response("{}", { status: 200 });
    Object.assign(process.env, { WHATSAPP_TOKEN: "tok", WHATSAPP_PHONE_NUMBER_ID: "123", WHATSAPP_TEMPLATE: "dutype_login_otp" });
    delete process.env.WHATSAPP_DAILY_CAP;
    delete process.env.SMS_PROVIDER;
    delete process.env.SMS_DAILY_CAP;
  });
  after(() => fft.cleanup());

  it("sends the same code by our SMS gateway when asked, or when WhatsApp fails", async () => {
    Object.assign(process.env, { SMS_PROVIDER: "2factor", SMS_API_KEY: "key1", SMS_TEMPLATE: "DUTYPE_OTP" });
    const gatewayOk = (url: string) => url.includes("2factor.in") ?
      new Response('{"Status":"Success","Details":"x"}', { status: 200 }) : new Response("{}", { status: 200 });
    respond = gatewayOk;
    // "Didn't get it? Send by SMS" right after the WhatsApp code: allowed at once.
    await call(login.sendWhatsappOtp)({ phone: "9876543210" }, app);
    const whatsappCode = sentCode();
    const r = await call(login.sendWhatsappOtp)({ phone: "9876543210", channel: "sms" }, app);
    assert.equal(r.sameCode, true, "a resend within 10 minutes sends the same code");
    assert.deepEqual([r.sent, r.channel], [true, "sms_gateway"]);
    const m = sent.at(-1)!.url.match(/2factor\.in\/API\/V1\/key1\/SMS\/9876543210\/(\d{6})\/DUTYPE_OTP$/);
    assert.ok(m, sent.at(-1)!.url);
    assert.equal(m![1], whatsappCode, "the SMS carries the same digits as the WhatsApp message");
    const v = await call(login.verifyWhatsappOtp)({ phone: PHONE, code: m![1] }, app);
    assert.ok(v.token);

    // WhatsApp down → the gateway is tried with the same request.
    await clear();
    sent = [];
    respond = (url) => url.includes("facebook") ? new Response("down", { status: 500 }) : gatewayOk(url);
    const r2 = await call(login.sendWhatsappOtp)({ phone: "9876543211" }, app);
    assert.deepEqual([r2.sent, r2.channel], [true, "sms_gateway"]);
    assert.equal(sent.length, 2);

    // Gateway also down → Firebase SMS.
    await clear();
    respond = () => new Response("down", { status: 500 });
    const r3 = await call(login.sendWhatsappOtp)({ phone: "9876543212", channel: "sms" }, app);
    assert.deepEqual(r3, { sent: false, channel: "sms" });
  });

  it("falls back to SMS when WhatsApp is not set up, without sending anything", async () => {
    process.env.WHATSAPP_TOKEN = "unset";
    const r = await call(login.sendWhatsappOtp)({ phone: "9876543210" }, app);
    assert.deepEqual(r, { sent: false, channel: "sms" });
    assert.equal(sent.length, 0);
  });

  it("sends a code, signs in the existing SMS account with it, and the code works once", async () => {
    const existing = await admin.auth().createUser({ phoneNumber: PHONE });
    const r = await call(login.sendWhatsappOtp)({ phone: "9876543210", role: "WORKER" }, app);
    assert.equal(r.sent, true);
    assert.match(sent[0].url, /graph\.facebook\.com\/v21\.0\/123\/messages$/);
    const body = JSON.parse(sent[0].body);
    assert.equal(body.to, "919876543210");
    const code = sentCode();
    assert.match(code, /^\d{6}$/);
    const stored = await db.doc(`otp_codes/${PHONE}`).get();
    assert.notEqual(stored.get("hash"), code, "only a hash is stored");

    const v = await call(login.verifyWhatsappOtp)({ phone: PHONE, code }, app);
    const claims = JSON.parse(Buffer.from(v.token.split(".")[1], "base64url").toString());
    assert.equal(claims.uid, existing.uid, "same uid as SMS login");

    await assert.rejects(call(login.verifyWhatsappOtp)({ phone: PHONE, code }, app), (e) => errorCode(e) === "deadline-exceeded");
  });

  it("creates the account for a new number", async () => {
    await call(login.sendWhatsappOtp)({ phone: PHONE }, app);
    const v = await call(login.verifyWhatsappOtp)({ phone: PHONE, code: sentCode() }, app);
    const user = await admin.auth().getUserByPhoneNumber(PHONE);
    assert.equal(JSON.parse(Buffer.from(v.token.split(".")[1], "base64url").toString()).uid, user.uid);
  });

  it("locks the code after 5 wrong tries", async () => {
    await call(login.sendWhatsappOtp)({ phone: PHONE }, app);
    const code = sentCode();
    const wrong = code === "000000" ? "111111" : "000000";
    for (let i = 0; i < 4; i++) {
      await assert.rejects(call(login.verifyWhatsappOtp)({ phone: PHONE, code: wrong }, app), (e) => errorCode(e) === "invalid-argument");
    }
    await assert.rejects(call(login.verifyWhatsappOtp)({ phone: PHONE, code: wrong }, app), (e) => errorCode(e) === "resource-exhausted");
    await assert.rejects(call(login.verifyWhatsappOtp)({ phone: PHONE, code }, app), (e) => errorCode(e) === "resource-exhausted");
  });

  it("refuses a number registered with the other role, before sending", async () => {
    await db.doc(`phoneRoles/${PHONE}`).set({ uid: "u1", role: "EMPLOYER" });
    await assert.rejects(call(login.sendWhatsappOtp)({ phone: PHONE, role: "WORKER" }, app),
      (e) => errorCode(e) === "failed-precondition");
    assert.equal(sent.length, 0);
  });

  it("asks to wait 30 s between codes and does not send", async () => {
    await call(login.sendWhatsappOtp)({ phone: PHONE }, app);
    const r = await call(login.sendWhatsappOtp)({ phone: PHONE }, app);
    assert.equal(r.sent, false);
    assert.equal(r.channel, "whatsapp");
    assert.ok(r.retryAfterSec > 0 && r.retryAfterSec <= 30);
    assert.equal(sent.length, 1);
  });

  it("switches to SMS when Meta refuses the message, and that code cannot be used", async () => {
    respond = () => new Response('{"error":{"code":131026}}', { status: 400 });
    const r = await call(login.sendWhatsappOtp)({ phone: PHONE }, app);
    assert.deepEqual(r, { sent: false, channel: "sms" });
    await assert.rejects(call(login.verifyWhatsappOtp)({ phone: PHONE, code: sentCode() }, app), (e) => errorCode(e) === "deadline-exceeded");
  });

  it("switches everyone to SMS once the daily cap is reached", async () => {
    process.env.WHATSAPP_DAILY_CAP = "1";
    assert.equal((await call(login.sendWhatsappOtp)({ phone: PHONE }, app)).sent, true);
    assert.deepEqual(await call(login.sendWhatsappOtp)({ phone: "+919876543211" }, app), { sent: false, channel: "sms" });
    assert.equal(sent.length, 1);
  });
});

describe("Truecaller sign-in (emulator)", () => {
  const verifier = "v".repeat(43);
  let userinfo: Record<string, unknown> = {};
  beforeEach(async () => {
    await clear();
    sent = [];
    process.env.TRUECALLER_CLIENT_ID = "client123";
    userinfo = { given_name: "Ravi", family_name: "Kumar", phone_number: "919876543210", email: "ravi@example.com" };
    respond = (url) => url.endsWith("/v1/token") ?
      new Response(JSON.stringify({ access_token: "at" }), { status: 200 }) :
      new Response(JSON.stringify(userinfo), { status: 200 });
  });

  it("signs up a new number and the email reaches the new profile", async () => {
    const r = await call(login.truecallerSignIn)({ authorizationCode: "c", codeVerifier: verifier, role: "WORKER", mode: "register" }, app);
    assert.equal(r.allowed, true);
    assert.equal(r.phone, PHONE);
    assert.equal(r.name, "Ravi Kumar");
    assert.equal(r.email, "ravi@example.com");
    assert.ok(r.token);
    const tokenReq = new URLSearchParams(sent[0].body);
    assert.equal(tokenReq.get("grant_type"), "authorization_code");
    assert.equal(tokenReq.get("client_id"), "client123");
    assert.equal(tokenReq.get("code_verifier"), verifier);

    const user = await admin.auth().getUserByPhoneNumber(PHONE);
    await call(profiles.completeRegistration)({ role: "WORKER", name: "Ravi Kumar" },
      { auth: { uid: user.uid, token: { phone_number: PHONE } }, app: { appId: "t" } });
    const profile = await db.doc(`worker_profiles/${user.uid}`).get();
    assert.equal(profile.get("email"), "ravi@example.com");
    assert.equal(profile.get("name"), "Ravi Kumar");
  });

  it("logs in an existing account and keeps a missing email", async () => {
    const user = await admin.auth().createUser({ phoneNumber: PHONE });
    await db.doc(`phoneRoles/${PHONE}`).set({ uid: user.uid, role: "EMPLOYER" });
    await db.doc(`employer_profiles/${user.uid}`).set({ ownerName: "Ravi" });
    const r = await call(login.truecallerSignIn)({ authorizationCode: "c", codeVerifier: verifier, role: "EMPLOYER", mode: "login" }, app);
    assert.equal(r.allowed, true);
    assert.equal(r.existingRole, "EMPLOYER");
    assert.equal(JSON.parse(Buffer.from(r.token.split(".")[1], "base64url").toString()).uid, user.uid);
    assert.equal((await db.doc(`employer_profiles/${user.uid}`).get()).get("email"), "ravi@example.com");
  });

  it("gives no token when login and account do not match", async () => {
    const none = await call(login.truecallerSignIn)({ authorizationCode: "c", codeVerifier: verifier, role: "WORKER", mode: "login" }, app);
    assert.equal(none.allowed, false);
    assert.equal(none.existingRole, null);
    assert.equal(none.token, undefined);
    await db.doc(`phoneRoles/${PHONE}`).set({ uid: "x", role: "EMPLOYER" });
    const other = await call(login.truecallerSignIn)({ authorizationCode: "c", codeVerifier: verifier, role: "WORKER", mode: "register" }, app);
    assert.equal(other.allowed, false);
    assert.equal(other.existingRole, "EMPLOYER");
    await assert.rejects(admin.auth().getUserByPhoneNumber(PHONE), "no auth account was created");
  });

  it("rejects foreign numbers and failed Truecaller exchanges", async () => {
    userinfo = { phone_number: "14155552671" };
    await assert.rejects(call(login.truecallerSignIn)({ authorizationCode: "c", codeVerifier: verifier, role: "WORKER" }, app),
      (e) => errorCode(e) === "failed-precondition");
    respond = () => new Response("bad", { status: 400 });
    await assert.rejects(call(login.truecallerSignIn)({ authorizationCode: "c", codeVerifier: verifier, role: "WORKER" }, app),
      (e) => errorCode(e) === "unauthenticated");
  });
});
