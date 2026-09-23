// Seeds the Firestore "wallpapers" collection with the app's 100 built-in wallpapers.
//
// Usage:
//   node seed-firestore.js <projectId> <accessToken>
// accessToken: `gcloud auth print-access-token` for an account with Firestore write access.
//
// After seeding, add or replace documents in the collection (fields: title, category,
// url, thumb, order) to change the catalog on every device without an app update.
// Images can live in Firebase Storage - use each file's download URL as `url`/`thumb`.
const fs = require('fs');
const path = require('path');

const [projectId, token] = process.argv.slice(2);
if (!projectId || !token) {
  console.error('usage: node seed-firestore.js <projectId> <accessToken>');
  process.exit(1);
}

const src = fs.readFileSync(path.join(__dirname, '../app/src/main/java/com/future/wallpapers/WallpaperCatalog.kt'), 'utf8');
const ids = [...src.split('BUILT_IN_IDS: List')[1].matchAll(/(\d+) to "([^"]+)"/g)].map(m => [m[1], m[2]]);

(async () => {
  let order = 0;
  for (const [pid, category] of ids) {
    const doc = {
      fields: {
        title: { stringValue: `${category} ${order + 1}` },
        category: { stringValue: category },
        url: { stringValue: `https://picsum.photos/id/${pid}/640/960` },
        thumb: { stringValue: `https://picsum.photos/id/${pid}/200/300` },
        order: { integerValue: String(order) },
      },
    };
    const url = `https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents/wallpapers?documentId=picsum-${pid}`;
    const res = await fetch(url, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
      body: JSON.stringify(doc),
    });
    console.log(`picsum-${pid}: ${res.status}`);
    order++;
  }
})();
