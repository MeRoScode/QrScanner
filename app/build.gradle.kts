@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.androidApplication)
}

android {
    namespace = "ir.meros.qrscanner"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "ir.meros.qrscanner"
        minSdk = 21
        targetSdk = 36
        versionCode = 4
        versionName = "2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Same applicationId for both stores; the flavor only records which market
    // the build was published to, so store-specific links (rating, updates) can
    // be built from BuildConfig.STORE.
    flavorDimensions += "store"
    productFlavors {
        create("bazaar") {
            dimension = "store"
            buildConfigField("String", "STORE", "\"bazaar\"")
        }
        create("myket") {
            dimension = "store"
            buildConfigField("String", "STORE", "\"myket\"")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.zxing.android.embedded)
    implementation(libs.tapsell.plus.sdk.android)
    implementation(libs.gson)}