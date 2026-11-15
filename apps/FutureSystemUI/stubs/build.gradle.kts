// compileOnly framework stubs generated from the device framework.jar (aosp/rom-repack/gen_stubs.py).
// Never packaged: at runtime the device's own classes are used.
plugins { alias(libs.plugins.android.library) }
android {
    namespace = "com.future.systemui.stubs"
    compileSdk = 37
    defaultConfig { minSdk = 31 }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_11; targetCompatibility = JavaVersion.VERSION_11 }
}
