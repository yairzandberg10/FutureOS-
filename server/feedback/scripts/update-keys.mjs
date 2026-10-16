// יוצר זוג מפתחות ECDSA P-256 לחתימת ה-manifest של העדכונים. רץ פעם אחת, במחשב שלך.
// שימוש:  node scripts/update-keys.mjs
//
// הפרטי -> סוד ב-Worker:   npx wrangler secret put UPDATE_SIGNING_KEY   (להדביק כשמתבקש)
//          ולפיתוח מקומי: UPDATE_SIGNING_KEY=... ב-.dev.vars
// הציבורי -> בבניית Settings:  future.update.pubkey=...  ב-Settings/local.properties
//
// זה לא מפתח החתימה של ה-APK. ה-APK ממשיך להיחתם במפתח ה-release שלך, והטלפון בודק אותו בנפרד.
// אם המפתח הפרטי דלף: ליצור זוג חדש, לבנות Settings עם הציבורי החדש, ורק אז להחליף את הסוד ב-Worker.

const pair = await crypto.subtle.generateKey({ name: "ECDSA", namedCurve: "P-256" }, true, ["sign", "verify"]);
const b64 = (buf) => Buffer.from(buf).toString("base64");
const priv = b64(await crypto.subtle.exportKey("pkcs8", pair.privateKey));
const pub = b64(await crypto.subtle.exportKey("spki", pair.publicKey));

console.log("UPDATE_SIGNING_KEY (פרטי - לא לגיט, לא לצ'אט):");
console.log(priv);
console.log("");
console.log("future.update.pubkey (ציבורי - ל-Settings/local.properties):");
console.log(pub);
