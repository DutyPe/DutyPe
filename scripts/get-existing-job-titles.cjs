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

async function run() {
  const snap = await db.collection("jobmetadata").get();
  console.log("Total jobs in jobmetadata:", snap.size);
  const titles = new Set();
  const districts = {};
  snap.forEach(d => {
    const data = d.data();
    if (data.title) titles.add(data.title.toLowerCase().trim());
    const dist = data.district || "Unknown";
    districts[dist] = (districts[dist] || 0) + 1;
  });
  console.log("Districts distribution:", districts);
  fs.writeFileSync(path.resolve(__dirname, "existing-titles.json"), JSON.stringify(Array.from(titles), null, 2));
}

run().catch(console.error);
