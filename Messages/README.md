# Messages

`com.future.messages` — SMS messaging.

Real reads/writes against the `Telephony.Sms` provider (`SmsRepository`), real send via `SmsManager`, contact-name resolution via `ContactsContract.PhoneLookup`. Implements all four requirements to be a valid default SMS app: `SmsDeliverReceiver` (`SMS_DELIVER_ACTION`, since the OS won't auto-insert incoming messages once this app is default), `MmsReceiver` (`WAP_PUSH_DELIVER_ACTION`, eligibility stub — full MMS parsing not implemented), `HeadlessSmsSendService` (`RESPOND_VIA_MESSAGE` quick-reply), and a `SENDTO` intent filter. Prompts to become the default SMS app before showing any UI.
