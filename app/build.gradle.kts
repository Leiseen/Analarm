plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)

    // 加上这个，才能使用ksp
    id("com.google.devtools.ksp")

}

android {
    namespace = "com.analarm"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.analarm"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Room数据库的核心组件
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    annotationProcessor("androidx.room:room-compiler:$room_version")
    // 要使用Kotlin的协程功能来操作数据库
    implementation("androidx.room:room-ktx:$room_version")
    // 要使用KSP，必须加上这个
    ksp("androidx.room:room-compiler:$room_version")

    // 引入强大的ZXing扫码库
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("androidx.core:core-ktx:1.13.1") // 确保有这个核心库

    // 引入ZXing的核心库，用于生成二维码
    implementation("com.google.zxing:core:3.5.3")

}