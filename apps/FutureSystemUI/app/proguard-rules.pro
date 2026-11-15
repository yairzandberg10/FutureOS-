# The framework calls these through binder/reflection from classes R8 cannot see (compileOnly stubs).
-keep class com.future.systemui.** { *; }
-dontwarn com.android.internal.**
-dontwarn android.hardware.biometrics.**
-dontwarn android.hardware.fingerprint.**
-dontwarn android.app.ITransientNotificationCallback
