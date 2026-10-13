const admin = require("firebase-admin");
const path = require("path");
const fs = require("fs");

const saPath = path.resolve(__dirname, "../serviceAccountKey.json.json");
const sa = JSON.parse(fs.readFileSync(saPath, "utf-8"));

admin.initializeApp({
  credential: admin.credential.cert(sa),
  projectId: "dutype-860ac"
});

const db = admin.firestore();

async function check() {
  const jobsSnap = await db.collection("jobmetadata").count().get();
  console.log("Total jobs in jobmetadata:", jobsSnap.data().count);

  const workersSnap = await db.collection("worker_profiles").limit(1000).get();
  console.log("Loaded workers:", workersSnap.size);
  const states = {};
  const districts = {};
  workersSnap.forEach(d => {
    const data = d.data();
    const st = data.state || data.stateId || (data.address && data.address.includes("Telangana") ? "Telangana" : (data.address && data.address.includes("Andhra Pradesh") ? "Andhra Pradesh" : "Unknown"));
    const dist = data.district || data.districtId || data.city || data.area || "Unknown";
    states[st] = (states[st] || 0) + 1;
    districts[dist] = (districts[dist] || 0) + 1;
  });

  const usersSnap = await db.collection("users").limit(1000).get();
  console.log("Loaded users:", usersSnap.size);
  usersSnap.forEach(d => {
    const data = d.data();
    if (data.role === "WORKER" || data.role === "worker") {
      const st = data.state || (data.city ? "Telangana" : "Unknown");
      states[st] = (states[st] || 0) + 1;
    }
  });

  console.log("=== WORKER STATES ===");
  console.log(JSON.stringify(states, null, 2));
  console.log("=== TOP DISTRICTS / LOCALITIES ===");
  const sortedDistricts = Object.entries(districts).sort((a,b) => b[1] - a[1]).slice(0, 20);
  console.log(JSON.stringify(Object.fromEntries(sortedDistricts), null, 2));
}

check().catch(console.error);
