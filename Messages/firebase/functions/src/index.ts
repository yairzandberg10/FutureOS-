/**
 * Cloud Function for FutureOS chat (Messages app).
 *
 * The server never sees message content: every inbox document is encrypted on
 * the sending device for the recipient (see ChatCrypto.kt). This function only
 * wakes the recipient with a content-free, high-priority data push; the device
 * then pulls, verifies and decrypts its inbox itself.
 */
import { setGlobalOptions } from "firebase-functions/v2";
import { onDocumentCreated } from "firebase-functions/v2/firestore";
import * as logger from "firebase-functions/logger";
import * as admin from "firebase-admin";

admin.initializeApp();
setGlobalOptions({ region: "europe-west1", maxInstances: 10 });

export const pushOnMessage = onDocumentCreated("inbox/{uid}/messages/{id}", async (event) => {
  const data = event.data?.data();
  // Only real messages wake the device. Receipts and "typing" wait for the
  // next sync or arrive live while the app is open.
  if (!data || data.kind !== "m") return;

  const uid = event.params.uid;
  const privateDoc = admin.firestore().doc(`private/${uid}`);
  const token = (await privateDoc.get()).get("fcmToken") as string | undefined;
  if (!token) return;

  try {
    await admin.messaging().send({
      token,
      data: { sync: "1" },
      android: { priority: "high", ttl: 28 * 24 * 60 * 60 * 1000 },
    });
  } catch (err: unknown) {
    const code = (err as { code?: string }).code;
    if (code === "messaging/registration-token-not-registered" || code === "messaging/invalid-registration-token") {
      await privateDoc.update({ fcmToken: admin.firestore.FieldValue.delete() });
    } else {
      logger.error("FCM send failed", { uid, code });
    }
  }
});
