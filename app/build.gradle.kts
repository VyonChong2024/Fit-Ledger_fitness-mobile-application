import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services") //Google Service Plugin (for Firebase)
    id("kotlin-kapt")                    //Kotlin annotation processor (for Room, Glide, etc.)
}

// Load sensitive keys (OpenAI API key, Google Web Client ID) from local.properties
val localProperties = Properties().apply {
    load(rootProject.file("local.properties").inputStream())
}

android {
    namespace = "com.example.fyp_fitledger"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.fyp_fitledger"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Load API keys securely from local.properties
        buildConfigField("String", "API_KEY", "\"${localProperties.getProperty("OPENAI_API_KEY")}\"")
        buildConfigField("String", "WEB_CLIENT_ID", "\"${localProperties.getProperty("GOOGLE_WEB_CLIENT_ID")}\"")
        manifestPlaceholders["WEB_CLIENT_ID"] = localProperties.getProperty("GOOGLE_WEB_CLIENT_ID")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    buildFeatures {
        viewBinding = true     // Enables View Binding
        compose = true         // Enables Jetpack Compose
        buildConfig = true     // Enables BuildConfig constants
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    signingConfigs {
        getByName("debug")
    }
}

dependencies {
    // --- Android Core and UI Libraries ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.fragment)
    implementation(libs.material)
    implementation(libs.androidx.gridlayout)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.cardview)

    // --- Navigation ---
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // --- Google Sign-In & Credential Manager ---
    implementation(libs.androidx.credentials)       //Credential Manager
    implementation(libs.androidx.credentials.play.services.auth)    // Google Sign-in
    implementation(libs.play.services.base)
    implementation(libs.googleid)
    implementation("com.google.android.gms:play-services-auth:21.3.0") {
        exclude(group = "some.conflicting.group", module = "conflicting-module")
    }

    // --- Google & Firebase Integration ---
    implementation(platform(libs.firebase.bom))  // Firebase Bill of Materials (version manager)
    implementation(libs.firebase.auth.ktx)       // Firebase Authentication
    implementation(libs.firebase.firestore.ktx)  // Firestore Database
    implementation(libs.firebase.functions.ktx)  // Firebase Cloud Functions
    implementation("com.google.firebase:firebase-database-ktx") // Firebase Realtime Database
    implementation(libs.firebase.analytics)      // Firebase Analytics

    // --- Room Database (Local SQLite) ---
    implementation(libs.androidx.room.runtime)
    kapt("androidx.room:room-compiler:2.7.0")

    // --- Lifecycle Components ---
    implementation(libs.androidx.lifecycle.runtime.ktx.v270)

    // --- Graphs and Charts ---
    implementation(libs.mpandroidchart)     // Line/Bar chart
    implementation(libs.eazegraph)          // Pie chart
    implementation(libs.library)            //Custom chart library
    implementation(libs.jjoe64.graphview) {
        exclude(group = "com.android.support")
    }
    implementation(libs.hellocharts.library) {
        exclude(group = "com.android.support")
    }

    // --- ChatGPT / API Integration ---
    implementation(libs.retrofit2.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.gson)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)
    implementation (libs.flexbox)     // flexbox layout

    // --- Image Loading ---
    implementation(libs.glide)
    kapt("com.github.bumptech.glide:compiler:4.16.0")

    // --- Testing Dependencies ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)


    //implementation(platform(libs.firebase.bom.v3280))
    //implementation(libs.google.firebase.analytics)
    //implementation(libs.firebase.auth)
    //implementation(libs.firebase.database.ktx)

}

configurations.all {
    /*resolutionStrategy {
        force("androidx.core:core-ktx:1.8.0")
        force("com.google.android.gms:play-services-auth:20.7.0")
    }*/
}