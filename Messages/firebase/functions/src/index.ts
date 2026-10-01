/**
 * Cloud Functions for FutureOS chat (Messages app) and the Music app's jam
 * (shared listening), which lives in the same Firebase project.
 *
 * The server never sees message content: every inbox document is encrypted on
 * the sending device for the recipient (see ChatCrypto.kt). This function only
 * wakes the recipient with a content-free, high-priority data push; the device
 * then pulls, verifies and decrypts its inbox itself.
 */
import { setGlobalOptions } from "firebase-functions/v2";
import { onDocumentCreated, onDocumentDeleted } from "firebase-functions/v2/firestore";
import { onSchedule } from "firebase-functions/v2/scheduler";
import * as logger from "firebase-functions/logger";
import { initializeApp } from "firebase-admin/app";
import { getFirestore, FieldValue, Timestamp } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";
import { getStorage } from "firebase-admin/storage";

initializeApp();
setGlobalOptions({ region: "europe-west1", maxInstances: 10 });

export const pushOnMessage = onDocumentCreated("inbox/{uid}/messages/{id}", async (event) => {
  const data = event.data?.data();
  // Only real messages wake the device. Receipts and "typing" wait for the
  // next sync or arrive live while the app is open.
  if (!data || data.kind !== "m") return;

  const uid = event.params.uid;
  const privateDoc = getFirestore().doc(`private/${uid}`);
  const token = (await privateDoc.get()).get("fcmToken") as string | undefined;
  if (!token) return;

  try {
    await getMessaging().send({
      token,
      data: { sync: "1" },
      android: { priority: "high", ttl: 28 * 24 * 60 * 60 * 1000 },
    });
  } catch (err: unknown) {
    const code = (err as { code?: string }).code;
    if (code === "messaging/registration-token-not-registered" || code === "messaging/invalid-registration-token") {
      await privateDoc.update({ fcmToken: FieldValue.delete() });
    } else {
      logger.error("FCM send failed", { uid, code });
    }
  }
});

/**
 * Jam (Music app): a jam document is gone - the host ended it, or jamExpiry
 * below removed it. Delete everything under it: members, the queue, the
 * uploaded songs, and its join code (unless the code already points to a new
 * jam). Firestore does not delete subcollections with their parent.
 */
export const jamCleanup = onDocumentDeleted("jams/{jamId}", async (event) => {
  const jamId = event.params.jamId;
  const code = event.data?.get("code") as string | undefined;
  const db = getFirestore();

  await db.recursiveDelete(db.collection(`jams/${jamId}/members`));
  await db.recursiveDelete(db.collection(`jams/${jamId}/queue`));
  if (code) {
    const codeRef = db.doc(`jamCodes/${code}`);
    await db.runTransaction(async (tx) => {
      const snap = await tx.get(codeRef);
      if (snap.exists && snap.get("jam") === jamId) tx.delete(codeRef);
    });
  }
  await getStorage().bucket().deleteFiles({ prefix: `jams/${jamId}/` });
  logger.info("Jam cleaned up", { jamId });
});

/** A jam lives at most 12 hours (JamSession.LIFETIME_MS); stale codes go with it. */
export const jamExpiry = onSchedule("every 60 minutes", async () => {
  const db = getFirestore();
  const now = Timestamp.now();
  const jams = await db.collection("jams").where("expireAt", "<", now).limit(200).get();
  await Promise.all(jams.docs.map((doc) => doc.ref.delete()));
  const codes = await db.collection("jamCodes").where("expireAt", "<", now).limit(200).get();
  await Promise.all(codes.docs.map((doc) => doc.ref.delete()));
  if (!jams.empty) logger.info("Expired jams removed", { count: jams.size });
});
