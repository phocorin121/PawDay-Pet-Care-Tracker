plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "com.phocorin.pawday"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.phocorin.pawday"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0-debug"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.webkit:webkit:1.12.1")
    implementation("com.google.zxing:core:3.5.3")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}
