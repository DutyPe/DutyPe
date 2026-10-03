"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.submitFieldLead = exports.FieldLeads = exports.FieldAgents = void 0;
exports.markFieldLeadJoined = markFieldLeadJoined;
/**
 * Field registration ("umbrella desk"): a DutyPe field agent sits at a bus stand, market or
 * labour adda and registers workers, service partners, employers and customers on the web form
 * dutype.in/join?agent=CODE. Each lead is a phone number the team then calls / onboards.
 *
 *   field_agents/{CODE}   agent name, phone, active (admin creates in the admin panel)
 *   field_leads/{+91..}   one lead per phone (the id makes duplicates merge, never double count)
 *
 *   submitFieldLead       public (no login): the agent's form; needs an active agent code and
 *                         the person's consent; capped per agent per day.
 *   markFieldLeadJoined   called at registration: the lead becomes JOINED and the agent gets
 *                         the credit (agents are paid per person who actually joins).
 */
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const secure_callable_1 = require("./secure-callable");
const input_1 = require("./lib/input");
const db = admin.firestore();
const { FieldValue, Timestamp } = admin.firestore;
const IST_OFFSET_MS = 330 * 60 * 1000;
const MAX_LEADS_PER_AGENT_PER_DAY = 300;
exports.FieldAgents = {
    COLLECTION: "field_agents",
    NAME: "name",
    PHONE: "phone",
    ACTIVE: "active",
    LEADS: "leads",
    JOINED: "joined",
    /** day (YYYY-MM-DD, IST) → leads that day */
    DAILY: "daily",
    CREATED_AT: "createdAt",
};
exports.FieldLeads = {
    COLLECTION: "field_leads",
    NAME: "name",
    PHONE: "phone",
    /** WORKER | PARTNER | EMPLOYER | CUSTOMER */
    ROLE: "role",
    SKILLS: "skills",
    AREA: "area",
    NOTE: "note",
    AGENT_CODE: "agentCode",
    /** NEW | CALLED | JOINED | NOT_INTERESTED */
    STATUS: "status",
    CONSENT: "consent",
    JOINED_UID: "joinedUid",
    JOINED_AT: "joinedAt",
    CREATED_AT: "createdAt",
    UPDATED_AT: "updatedAt",
};
const ROLES = ["WORKER", "PARTNER", "EMPLOYER", "CUSTOMER"];
function e164(raw) {
    const digits = String(raw !== null && raw !== void 0 ? raw : "").replace(/\D/g, "");
    if (/^[6-9]\d{9}$/.test(digits))
        return `+91${digits}`;
    if (/^91[6-9]\d{9}$/.test(digits))
        return `+${digits}`;
    return null;
}
exports.submitFieldLead = (0, secure_callable_1.onCallSecured)({ requireAuth: false, enforceAppCheck: false, timeoutSeconds: 15 }, async (raw) => {
    const data = (0, input_1.obj)(raw);
    const agentCode = (0, input_1.str)(data, "agentCode", { min: 3, max: 20, pattern: /^[A-Za-z0-9]+$/ }).toUpperCase();
    const phone = e164(data.phone);
    if (!phone)
        (0, input_1.fail)("invalid-argument", "Enter a valid 10-digit mobile number");
    const role = String(data.role || "").toUpperCase();
    if (!ROLES.includes(role))
        (0, input_1.fail)("invalid-argument", "Choose who is registering");
    if (data.consent !== true)
        (0, input_1.fail)("failed-precondition", "The person must agree to be contacted by DutyPe");
    const name = (0, input_1.str)(data, "name", { min: 2, max: 80 });
    const area = (0, input_1.str)(data, "area", { max: 80, optional: true });
    const note = (0, input_1.text)(data, "note", { max: 300, optional: true });
    const skills = (0, input_1.stringList)(data, "skills", { maxItems: 12, maxLength: 30 });
    const day = new Date(Date.now() + IST_OFFSET_MS).toISOString().slice(0, 10);
    const agentRef = db.collection(exports.FieldAgents.COLLECTION).doc(agentCode);
    const leadRef = db.collection(exports.FieldLeads.COLLECTION).doc(phone);
    const out = await db.runTransaction(async (tx) => {
        const [agent, lead] = await Promise.all([tx.get(agentRef), tx.get(leadRef)]);
        if (!agent.exists || agent.get(exports.FieldAgents.ACTIVE) !== true)
            (0, input_1.fail)("permission-denied", "This agent code is not active. Ask DutyPe for your code.");
        const today = Number((agent.get(exports.FieldAgents.DAILY) || {})[day] || 0);
        if (!lead.exists && today >= MAX_LEADS_PER_AGENT_PER_DAY)
            (0, input_1.fail)("resource-exhausted", "Daily limit reached for this agent code");
        const now = Timestamp.now();
        if (lead.exists) {
            // Same person again (maybe with more skills): update, but the first agent keeps the credit.
            tx.update(leadRef, Object.assign(Object.assign(Object.assign({ [exports.FieldLeads.NAME]: name, [exports.FieldLeads.ROLE]: role, [exports.FieldLeads.SKILLS]: FieldValue.arrayUnion(...skills) }, (area ? { [exports.FieldLeads.AREA]: area } : {})), (note ? { [exports.FieldLeads.NOTE]: note } : {})), { [exports.FieldLeads.UPDATED_AT]: now }));
            return { duplicate: true, today };
        }
        tx.create(leadRef, {
            [exports.FieldLeads.NAME]: name,
            [exports.FieldLeads.PHONE]: phone,
            [exports.FieldLeads.ROLE]: role,
            [exports.FieldLeads.SKILLS]: skills,
            [exports.FieldLeads.AREA]: area,
            [exports.FieldLeads.NOTE]: note,
            [exports.FieldLeads.AGENT_CODE]: agentCode,
            [exports.FieldLeads.STATUS]: "NEW",
            [exports.FieldLeads.CONSENT]: true,
            [exports.FieldLeads.CREATED_AT]: now,
            [exports.FieldLeads.UPDATED_AT]: now,
        });
        tx.update(agentRef, {
            [exports.FieldAgents.LEADS]: FieldValue.increment(1),
            [`${exports.FieldAgents.DAILY}.${day}`]: FieldValue.increment(1),
        });
        return { duplicate: false, today: today + 1 };
    });
    return { ok: true, duplicate: out.duplicate, agentToday: out.today };
});
/** At registration: a lead with this phone becomes JOINED (agent credited once). Never throws. */
async function markFieldLeadJoined(phone, uid) {
    try {
        const leadRef = db.collection(exports.FieldLeads.COLLECTION).doc(phone);
        await db.runTransaction(async (tx) => {
            const lead = await tx.get(leadRef);
            if (!lead.exists || lead.get(exports.FieldLeads.STATUS) === "JOINED")
                return;
            const agentRef = db.collection(exports.FieldAgents.COLLECTION).doc(String(lead.get(exports.FieldLeads.AGENT_CODE)));
            const agent = await tx.get(agentRef);
            tx.update(leadRef, {
                [exports.FieldLeads.STATUS]: "JOINED", [exports.FieldLeads.JOINED_UID]: uid,
                [exports.FieldLeads.JOINED_AT]: Timestamp.now(), [exports.FieldLeads.UPDATED_AT]: Timestamp.now(),
            });
            if (agent.exists)
                tx.update(agentRef, { [exports.FieldAgents.JOINED]: FieldValue.increment(1) });
        });
    }
    catch (e) {
        functions.logger.warn("markFieldLeadJoined", e);
    }
}
//# sourceMappingURL=field-leads.js.map