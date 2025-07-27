// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false

    // 在这里声明KSP插件和它的版本号
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false

}