"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import {
  collection,
  doc,
  getDoc,
  getDocs,
  increment,
  query,
  serverTimestamp,
  setDoc,
  updateDoc,
  where
} from "firebase/firestore";

import { getFirebaseServices } from "@/lib/firebase/client";
import { formatCurrency, formatDate } from "@/lib/firebase/firestore-helpers";

type LogTone = "info" | "success" | "warning" | "error";

type LogEntry = {
  id: number;
  tone: LogTone;
  message: string;
};

type ReferralCodeRecord = {
  code?: string;
  userId?: string;
  userRole?: string;
  userName?: string;
  isActive?: boolean;
};

type ReferralDetailRow = {
  id: string;
  referredUserName?: string;
  referredUserPhone?: string;
  referredUserRole?: string;
  status?: string;
  rewardAmount?: number | string;
  createdAt?: unknown;
};

type UserSummary = {
  id: string;
  name?: string;
  fullName?: string;
  phone?: string;
  role?: string;
  activeRole?: string;
  referralCode?: string;
  profileCompleted?: boolean;
  referralEarnings?: number;
  successfulReferrals?: number;
  totalReferrals?: number;
};

type ReferralInspectionResult = {
  code: string;
  codeData: ReferralCodeRecord;
  user: UserSummary | null;
  referrals: ReferralDetailRow[];
};

export function ReferralToolsNav() {
  return (
    <div className="pill-row referral-tools-nav">
      <Link href="/admin/referrals" className="pill pill-link">
        Referrals
      </Link>
      <Link href="/admin/referral-config" className="pill pill-link">
        Config
      </Link>
      <Link href="/admin/check-and-create-code" className="pill pill-link">
        Check code
      </Link>
      <Link href="/admin/create-test-referral" className="pill pill-link">
        Create test referral
      </Link>
      <Link href="/admin/test-referral" className="pill pill-link">
        Test code
      </Link>
    </div>
  );
}

export function AdminCheckAndCreateCodeClient() {
  const services = useMemo(() => getFirebaseServices(), []);
  const [phoneNumber, setPhoneNumber] = useState("+919392992298");
  const [submitting, setSubmitting] = useState(false);
  const [entries, setEntries] = useState<LogEntry[]>([
    createLogEntry("Ready to check user and create referral code.", "info")
  ]);

  async function handleSubmit() {
    if (!services) {
      setEntries([createLogEntry("Firebase is not configured.", "error")]);
      return;
    }

    const phoneInput = phoneNumber.trim();
    if (!phoneInput) {
      setEntries([createLogEntry("Please enter a phone number.", "error")]);
      return;
    }

    setSubmitting(true);
    const nextEntries: LogEntry[] = [];
    const push = createLogger(nextEntries);

    push("Checking user and creating referral code", "info");
    push("=".repeat(60), "info");
    push(`Phone: ${phoneInput}`, "info");
    push("", "info");

    try {
      push("Step 1: Finding user by phone...", "info");
      const usersSnapshot = await getDocs(
        query(collection(services.db, "users"), where("phone", "==", phoneInput))
      );

      if (usersSnapshot.empty) {
        push(`ERROR: No user found with phone ${phoneInput}`, "error");
        setEntries(nextEntries);
        return;
      }

      const userDoc = usersSnapshot.docs[0];
      const userId = userDoc.id;
      const userData = userDoc.data() as UserSummary;
      const userName = userData.name || userData.fullName || "User";

      push("User found!", "success");
      push(`   User ID: ${userId}`, "info");
      push(`   Name: ${userName}`, "info");
      push(`   Role: ${userData.role || "N/A"}`, "info");
      push(`   Profile Completed: ${userData.profileCompleted ? "Yes" : "No"}`, "info");
      push(`   Current Referral Code: ${userData.referralCode || "NONE"}`, "warning");

      if (userData.referralCode) {
        push("", "info");
        push(`User already has referral code: ${userData.referralCode}`, "success");
        push("", "info");
        push("Checking if code exists in referral_codes collection...", "info");

        const codeDoc = await getDoc(doc(services.db, "referral_codes", userData.referralCode));
        if (codeDoc.exists()) {
          push("Code exists in referral_codes collection", "success");
        } else {
          push("Code not found in referral_codes collection. Creating it...", "warning");
          await setDoc(doc(services.db, "referral_codes", userData.referralCode), {
            code: userData.referralCode,
            userId,
            userRole: userData.role || "WORKER",
            userName,
            isActive: true,
            createdAt: serverTimestamp(),
            totalUsed: 0
          });
          push("Created referral_codes document", "success");
        }

        setEntries(nextEntries);
        return;
      }

      push("", "info");
      push("Step 3: Generating new referral code...", "info");

      let referralCode = generateReferralCode(userName);
      let attempts = 0;

      while (attempts < 10) {
        const existingCode = await getDoc(doc(services.db, "referral_codes", referralCode));
        if (!existingCode.exists()) {
          break;
        }

        push("   Code collision, generating new one...", "warning");
        referralCode = generateReferralCode(userName);
        attempts += 1;
      }

      if (attempts >= 10) {
        push("ERROR: Failed to generate a unique code after 10 attempts", "error");
        setEntries(nextEntries);
        return;
      }

      push(`Generated code: ${referralCode}`, "success");
      push("", "info");
      push("Step 4: Saving referral code...", "info");

      await updateDoc(doc(services.db, "users", userId), {
        referralCode,
        referralCodeCreatedAt: serverTimestamp(),
        referralStats: {
          totalReferrals: 0,
          successfulReferrals: 0,
          pendingReferrals: 0,
          totalEarnings: 0,
          availableBalance: 0,
          withdrawnAmount: 0,
          canWithdraw: false,
          nextMilestone: 5,
          currentTier: "BRONZE",
          freeJobPostings: 0,
          freeJobPostingsExpiry: null,
          lastUpdated: serverTimestamp()
        }
      });
      push("Updated user document", "success");

      await setDoc(doc(services.db, "referral_codes", referralCode), {
        code: referralCode,
        userId,
        userRole: userData.role || "WORKER",
        userName,
        isActive: true,
        createdAt: serverTimestamp(),
        totalUsed: 0
      });
      push("Created referral_codes document", "success");

      push("", "info");
      push("REFERRAL CODE CREATED SUCCESSFULLY!", "success");
      push("", "info");
      push(`Your referral code: ${referralCode.toUpperCase()}`, "success");
      push("", "info");
      push("Next steps:", "warning");
      push("   1. Test the code from the Test code page.", "warning");
      push("   2. Or create a test referral from the Create test referral page.", "warning");
      push("   3. Check Refer & Earn inside the app.", "warning");
    } catch (error) {
      push("", "info");
      push(`ERROR: ${readError(error)}`, "error");
    } finally {
      setEntries(nextEntries);
      setSubmitting(false);
    }
  }

  return (
    <div className="admin-section-stack">
      <section className="section">
        <div className="section-header">
          <div>
            <span className="tag">Legacy utility</span>
            <h2>Check user and create referral code</h2>
          </div>
          <p>
            React replacement for the old `check-and-create-code.html` admin tool.
          </p>
        </div>

        <ReferralToolsNav />

        <div className="admin-tool-form card">
          <label className="field-label" htmlFor="check-phone-number">
            Phone number (with +91)
          </label>
          <div className="admin-tool-row">
            <input
              id="check-phone-number"
              className="text-input"
              value={phoneNumber}
              onChange={(event) => setPhoneNumber(event.target.value)}
              placeholder="+919392992298"
            />
            <button
              type="button"
              className="button"
              onClick={() => void handleSubmit()}
              disabled={submitting}
            >
              {submitting ? "Checking..." : "Check User & Create Code"}
            </button>
          </div>
          <p className="route-note">
            This finds the user by phone number and creates a referral code if one does not
            exist yet.
          </p>
        </div>
      </section>

      <LogConsole entries={entries} />
    </div>
  );
}

export function AdminCreateTestReferralClient() {
  const services = useMemo(() => getFirebaseServices(), []);
  const [referralCode, setReferralCode] = useState("WRKG0XBC8");
  const [submitting, setSubmitting] = useState(false);
  const [entries, setEntries] = useState<LogEntry[]>([
    createLogEntry("Ready to test the referral system.", "info")
  ]);

  async function handleSubmit() {
    if (!services) {
      setEntries([createLogEntry("Firebase is not configured.", "error")]);
      return;
    }

    const rawCode = referralCode.trim();
    if (!rawCode) {
      setEntries([createLogEntry("Please enter a referral code.", "error")]);
      return;
    }

    setSubmitting(true);
    const nextEntries: LogEntry[] = [];
    const push = createLogger(nextEntries);

    push("Testing referral system", "info");
    push("=".repeat(60), "info");
    push(`Referral Code: ${rawCode.toUpperCase()}`, "info");
    push("", "info");

    try {
      const resolvedCode = await resolveReferralCode(services.db, rawCode, push);
      if (!resolvedCode) {
        setEntries(nextEntries);
        return;
      }

      push("Code exists!", "success");
      push(`   Referrer ID: ${resolvedCode.codeData.userId || "N/A"}`, "info");
      push("", "info");
      push("Step 2: Getting referrer current stats...", "info");

      const referrerId = resolvedCode.codeData.userId;
      if (!referrerId) {
        push("ERROR: Referrer user ID is missing from the referral code record.", "error");
        setEntries(nextEntries);
        return;
      }

      const referrerDoc = await getDoc(doc(services.db, "users", referrerId));
      if (!referrerDoc.exists()) {
        push("ERROR: Referrer user not found!", "error");
        setEntries(nextEntries);
        return;
      }

      const referrerData = referrerDoc.data() as UserSummary;
      push("Referrer found!", "success");
      push(`   Name: ${referrerData.name || referrerData.fullName || "N/A"}`, "info");
      push(`   Phone: ${referrerData.phone || "N/A"}`, "info");
      push(`   Current Earnings: ${formatCurrency(referrerData.referralEarnings || 0)}`, "info");
      push(`   Successful Referrals: ${referrerData.successfulReferrals || 0}`, "info");

      push("", "info");
      push("Step 3: Creating test referral...", "info");

      const testUserId = `TEST_USER_${Date.now()}`;
      const referralId = `${referrerId}_${testUserId}`;
      await setDoc(doc(services.db, "referrals", referralId), {
        referralCode: resolvedCode.code,
        referrerUserId: referrerId,
        referredUserId: testUserId,
        referredUserName: "Test User",
        referredUserPhone: "+919999999999",
        referredUserRole: "WORKER",
        status: "COMPLETED",
        rewardAmount: 50,
        createdAt: serverTimestamp(),
        completedAt: serverTimestamp(),
        expiresAt: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000)
      });
      push("Test referral created!", "success");
      push(`   Referral ID: ${referralId}`, "info");

      push("", "info");
      push("Step 4: Updating referrer earnings...", "info");
      await updateDoc(doc(services.db, "users", referrerId), {
        referralEarnings: increment(50),
        successfulReferrals: increment(1),
        totalReferrals: increment(1)
      });
      push("Earnings updated! Added Rs 50", "success");

      push("", "info");
      push("Step 5: Verifying results...", "info");
      const updatedReferrerDoc = await getDoc(doc(services.db, "users", referrerId));
      const updatedData = updatedReferrerDoc.data() as UserSummary | undefined;

      push("Updated stats:", "success");
      push(`   New Earnings: ${formatCurrency(updatedData?.referralEarnings || 0)}`, "success");
      push(`   New Successful Referrals: ${updatedData?.successfulReferrals || 0}`, "success");
      push("   Increase: +Rs 50, +1 referral", "success");

      push("", "info");
      push("Step 6: Checking all referrals for this code...", "info");
      const referralsSnapshot = await getDocs(
        query(collection(services.db, "referrals"), where("referralCode", "==", resolvedCode.code))
      );

      push(`Total referrals: ${referralsSnapshot.size}`, "success");
      referralsSnapshot.docs.forEach((item, index) => {
        const referral = item.data() as ReferralDetailRow;
        push("", "info");
        push(`   Referral #${index + 1}:`, "info");
        push(`     User: ${referral.referredUserName || "N/A"}`, "info");
        push(`     Phone: ${referral.referredUserPhone || "N/A"}`, "info");
        push(`     Status: ${referral.status || "N/A"}`, "info");
        push(`     Reward: ${formatCurrency(referral.rewardAmount || 0)}`, "info");
      });

      push("", "info");
      push("TEST COMPLETED SUCCESSFULLY!", "success");
      push("", "info");
      push("Check in app:", "warning");
      push("   - Open Refer & Earn screen", "warning");
      push(
        `   - You should see earnings: ${formatCurrency(updatedData?.referralEarnings || 0)}`,
        "warning"
      );
      push(
        `   - Successful referrals: ${updatedData?.successfulReferrals || 0}`,
        "warning"
      );
      push("", "info");
      push("Check in admin:", "warning");
      push("   - Open the Referrals page or the Test code page", "warning");
      push("   - You should see the newly created test referral", "warning");
    } catch (error) {
      push("", "info");
      push(`ERROR: ${readError(error)}`, "error");
    } finally {
      setEntries(nextEntries);
      setSubmitting(false);
    }
  }

  return (
    <div className="admin-section-stack">
      <section className="section">
        <div className="section-header">
          <div>
            <span className="tag">Legacy utility</span>
            <h2>Create a test referral</h2>
          </div>
          <p>
            React replacement for the old `create-test-referral.html` admin tool.
          </p>
        </div>

        <ReferralToolsNav />

        <div className="admin-tool-form card">
          <label className="field-label" htmlFor="test-referral-code">
            Referral code
          </label>
          <div className="admin-tool-row">
            <input
              id="test-referral-code"
              className="text-input"
              value={referralCode}
              onChange={(event) => setReferralCode(event.target.value.toUpperCase())}
              placeholder="WRKG0XBC8"
            />
            <button
              type="button"
              className="button"
              onClick={() => void handleSubmit()}
              disabled={submitting}
            >
              {submitting ? "Testing..." : "Create Test Referral (+Rs 50)"}
            </button>
          </div>
          <p className="route-note">
            This creates a completed referral entry and credits the referrer with Rs 50.
          </p>
        </div>
      </section>

      <LogConsole entries={entries} />
    </div>
  );
}

export function AdminTestReferralClient() {
  const services = useMemo(() => getFirebaseServices(), []);
  const [referralCode, setReferralCode] = useState("WRKG0XBC8");
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<ReferralInspectionResult | null>(null);
  const [message, setMessage] = useState<string>("Enter a referral code and click Check code.");
  const [messageTone, setMessageTone] = useState<LogTone>("info");

  async function handleCheck() {
    if (!services) {
      setMessage("Firebase is not configured.");
      setMessageTone("error");
      setResult(null);
      return;
    }

    const code = referralCode.trim().toUpperCase();
    if (!code) {
      setMessage("Please enter a referral code.");
      setMessageTone("error");
      setResult(null);
      return;
    }

    setLoading(true);
    setMessage("Checking referral code...");
    setMessageTone("info");

    try {
      const codeDoc = await getDoc(doc(services.db, "referral_codes", code));
      if (!codeDoc.exists()) {
        setMessage("Referral code not found.");
        setMessageTone("error");
        setResult(null);
        return;
      }

      const codeData = codeDoc.data() as ReferralCodeRecord;
      const user =
        codeData.userId
          ? await getDoc(doc(services.db, "users", codeData.userId))
          : null;

      const referralsSnapshot = await getDocs(
        query(collection(services.db, "referrals"), where("referralCode", "==", code))
      );

      setResult({
        code,
        codeData,
        user: user?.exists()
          ? ({
              id: user.id,
              ...(user.data() as Omit<UserSummary, "id">)
            } as UserSummary)
          : null,
        referrals: referralsSnapshot.docs.map((item) => ({
          id: item.id,
          ...(item.data() as Omit<ReferralDetailRow, "id">)
        }))
      });
      setMessage("Referral code is valid.");
      setMessageTone("success");
    } catch (error) {
      setMessage(readError(error));
      setMessageTone("error");
      setResult(null);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void handleCheck();
    // Intentionally match the old page behavior by auto-checking the default code on load.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [services]);

  return (
    <div className="admin-section-stack">
      <section className="section">
        <div className="section-header">
          <div>
            <span className="tag">Legacy utility</span>
            <h2>Test referral code</h2>
          </div>
          <p>
            React replacement for the old `test-referral.html` admin tool.
          </p>
        </div>

        <ReferralToolsNav />

        <div className="admin-tool-form card">
          <label className="field-label" htmlFor="referral-code-inspector">
            Referral code
          </label>
          <div className="admin-tool-row">
            <input
              id="referral-code-inspector"
              className="text-input"
              value={referralCode}
              onChange={(event) => setReferralCode(event.target.value.toUpperCase())}
              placeholder="WRKG0XBC8"
            />
            <button
              type="button"
              className="button"
              onClick={() => void handleCheck()}
              disabled={loading}
            >
              {loading ? "Checking..." : "Check Code"}
            </button>
          </div>
          <p className={`tool-status tone-${messageTone}`}>{message}</p>
        </div>
      </section>

      <section className="section">
        <div className="section-header">
          <div>
            <span className="tag">Referral details</span>
            <h2>Referrer information</h2>
          </div>
          <p>Inspect the referrer record and the list of referrals already credited.</p>
        </div>

        {!result ? (
          <div className="empty-state">No referral details to show yet.</div>
        ) : (
          <div className="admin-detail-stack">
            <div className="table-wrap">
              <table className="data-table">
                <tbody>
                  <tr>
                    <th>Name</th>
                    <td>{result.user?.name || result.user?.fullName || "N/A"}</td>
                  </tr>
                  <tr>
                    <th>Phone</th>
                    <td>{result.user?.phone || "N/A"}</td>
                  </tr>
                  <tr>
                    <th>Role</th>
                    <td>{result.user?.role || result.user?.activeRole || "N/A"}</td>
                  </tr>
                  <tr>
                    <th>Total Earnings</th>
                    <td>{formatCurrency(result.user?.referralEarnings || 0)}</td>
                  </tr>
                  <tr>
                    <th>Successful Referrals</th>
                    <td>{result.user?.successfulReferrals || 0}</td>
                  </tr>
                  <tr>
                    <th>Total Referrals</th>
                    <td>{result.user?.totalReferrals || 0}</td>
                  </tr>
                </tbody>
              </table>
            </div>

            <div className="table-wrap">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Referred User</th>
                    <th>Phone</th>
                    <th>Role</th>
                    <th>Status</th>
                    <th>Reward</th>
                    <th>Date</th>
                  </tr>
                </thead>
                <tbody>
                  {result.referrals.length === 0 ? (
                    <tr>
                      <td colSpan={6}>No one has used this referral code yet.</td>
                    </tr>
                  ) : (
                    result.referrals.map((referral) => (
                      <tr key={referral.id}>
                        <td>{referral.referredUserName || "N/A"}</td>
                        <td>{referral.referredUserPhone || "N/A"}</td>
                        <td>{referral.referredUserRole || "N/A"}</td>
                        <td>
                          <span className={`status-pill ${statusTone(referral.status)}`}>
                            {referral.status || "PENDING"}
                          </span>
                        </td>
                        <td>{formatCurrency(referral.rewardAmount || 0)}</td>
                        <td>{formatDate(referral.createdAt)}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>

            <div className="detail-panel tone-highlight">
              <span className="card-kicker">Test in app</span>
              <h3>How to verify it on mobile</h3>
              <ol className="tool-steps">
                <li>Create a new user account in the app.</li>
                <li>
                  During profile setup, enter code <strong>{result.code}</strong>.
                </li>
                <li>Complete the profile.</li>
                <li>Check Refer & Earn. Earnings should increase by Rs 50 after completion.</li>
                <li>Refresh this page to inspect the new referral row.</li>
              </ol>
            </div>
          </div>
        )}
      </section>
    </div>
  );
}

function LogConsole({ entries }: { entries: LogEntry[] }) {
  return (
    <section className="section">
      <div className="section-header">
        <div>
          <span className="tag">Output</span>
          <h2>Execution log</h2>
        </div>
        <p>Matches the console-style output from the old admin HTML utilities.</p>
      </div>

      <div className="log-console">
        {entries.map((entry) => (
          <p key={entry.id} className={`log-entry ${entry.tone}`}>
            {entry.message}
          </p>
        ))}
      </div>
    </section>
  );
}

function createLogEntry(message: string, tone: LogTone): LogEntry {
  return {
    id: Date.now() + Math.random(),
    tone,
    message
  };
}

function createLogger(entries: LogEntry[]) {
  return (message: string, tone: LogTone) => {
    entries.push(createLogEntry(message, tone));
  };
}

function generateReferralCode(userName: string) {
  const digits = "0123456789";
  const letters = "abcdefghjklmnpqrstuvwxyz";

  let namePrefix = "";
  const firstName = userName.trim().split(/\s+/)[0]?.toLowerCase() || "";
  const cleanName = firstName.replace(/[^a-z]/g, "");

  if (cleanName.length > 0) {
    namePrefix = cleanName.substring(0, Math.min(6, cleanName.length));
  }

  if (namePrefix.length === 0) {
    for (let index = 0; index < 4; index += 1) {
      namePrefix += letters.charAt(Math.floor(Math.random() * letters.length));
    }
  }

  let digitSuffix = "";
  for (let index = 0; index < 4; index += 1) {
    digitSuffix += digits.charAt(Math.floor(Math.random() * digits.length));
  }

  return `${namePrefix}${digitSuffix}`;
}

async function resolveReferralCode(
  db: NonNullable<ReturnType<typeof getFirebaseServices>>["db"],
  rawCode: string,
  push: (message: string, tone: LogTone) => void
) {
  push("Step 1: Checking referral code...", "info");

  let workingCode = rawCode.trim().toLowerCase();
  let codeDoc = await getDoc(doc(db, "referral_codes", workingCode));

  if (!codeDoc.exists()) {
    const upperCode = workingCode.toUpperCase();
    codeDoc = await getDoc(doc(db, "referral_codes", upperCode));
    if (codeDoc.exists()) {
      push(`Found code in uppercase format: ${upperCode}`, "success");
      workingCode = upperCode;
    }
  }

  if (codeDoc.exists()) {
    push("Code exists in referral_codes collection!", "success");
    return {
      code: workingCode,
      codeData: codeDoc.data() as ReferralCodeRecord
    };
  }

  push("Code not found in referral_codes collection", "warning");
  push("   Searching in users collection (trying both cases)...", "info");

  let usersSnapshot = await getDocs(
    query(collection(db, "users"), where("referralCode", "==", workingCode))
  );

  if (usersSnapshot.empty) {
    const upperCode = workingCode.toUpperCase();
    push(`   Trying uppercase: ${upperCode}`, "info");
    usersSnapshot = await getDocs(
      query(collection(db, "users"), where("referralCode", "==", upperCode))
    );

    if (!usersSnapshot.empty) {
      workingCode = upperCode;
    }
  }

  if (usersSnapshot.empty) {
    push("ERROR: Referral code does not exist anywhere!", "error");
    push("   Tried both lowercase and uppercase formats.", "error");
    push('   Use the "Check code" page to create it first.', "error");
    return null;
  }

  const userDoc = usersSnapshot.docs[0];
  const userData = userDoc.data() as UserSummary;
  const actualCode = userData.referralCode || workingCode;

  push("Found code in users collection!", "success");
  push(`   User ID: ${userDoc.id}`, "info");
  push("   Creating referral_codes document...", "info");

  await setDoc(doc(db, "referral_codes", actualCode), {
    code: actualCode,
    userId: userDoc.id,
    userRole: userData.role || "WORKER",
    userName: userData.name || userData.fullName || "User",
    isActive: true,
    createdAt: serverTimestamp(),
    totalUsed: 0
  });

  push(`Created referral_codes document with code: ${actualCode}`, "success");

  return {
    code: actualCode,
    codeData: {
      code: actualCode,
      userId: userDoc.id,
      userRole: userData.role || "WORKER",
      userName: userData.name || userData.fullName || "User",
      isActive: true
    }
  };
}

function statusTone(status: string | undefined) {
  switch (status) {
    case "COMPLETED":
    case "ACCEPTED":
      return "success";
    case "FAILED":
    case "REJECTED":
      return "danger";
    case "PENDING":
      return "warning";
    default:
      return "neutral";
  }
}

function readError(error: unknown) {
  return error instanceof Error ? error.message : "Unknown error";
}
