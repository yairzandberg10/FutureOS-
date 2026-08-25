# Dialer (טלפון)

`com.future.dialer` — Phone dialer.

Real `ContactsContract`/`CallLog` backed dial suggestions (T9 matcher), call history, and contacts — no mock data. Requests the `RoleManager.ROLE_DIALER` role and implements a real `InCallService` (`telecom/CallService.kt`), so once granted the system binds it for every call (incoming or outgoing) and drives the in-call screen (answer/reject/mute/speaker/hangup) off the actual `android.telecom.Call`, not a fake timer.
