plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services") //Google Service Gradle Plugin
    id("kotlin-kapt")                       //food api plugin
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
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        //Newly Added
        debug {
            isDebuggable = true
        }
    }
    buildFeatures {  //NEWLY ADDED
        viewBinding = true
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
}

dependencies {

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
    //implementation(libs.androidx.legacy.support.v4)
    implementation(libs.androidx.gridlayout)
    implementation(libs.googleid)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)


    implementation(libs.play.services.base)
    //implementation(libs.play.services.auth)
    //implementation(libs.play.services.auth.v2070)
    //implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials) //Credential Manager
    implementation(libs.androidx.credentials.play.services.auth) // Google Sign-in

    implementation(platform(libs.firebase.bom))
    implementation(platform(libs.firebase.bom.v3280))
    //implementation("com.google.firebase:firebase-auth-ktx:23.2.0") // Firebase Authentication
    //implementation("com.google.firebase:firebase-analytics-ktx")
    //implementation("com.google.firebase:firebase-auth-ktx")
    implementation(libs.firebase.auth.ktx)
    implementation(libs.google.firebase.analytics)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)

    implementation(libs.androidx.appcompat) // Make sure this is compatible with other androidx libraries
    implementation(libs.androidx.constraintlayout) // Check for compatibility
    //implementation(libs.material.v1100) // (5) Check for compatibility

    //implementation(libs.jjoe64.graphview)   //Line graph //duplicate problem
    //implementation(libs.hellocharts.library)  //Pie chart //duplicate problem

    implementation(libs.androidx.cardview)  //Card View
    //implementation(libs.hellocharts.android) //implementation("com.github.lecho:hellocharts-android:v1.5.8")
    implementation(libs.mpandroidchart) //implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation(libs.eazegraph)          //Pie chart
    implementation(libs.library)

    //ChatGPT API
    implementation(libs.retrofit2.retrofit)             //implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation(libs.converter.gson)                 //implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation(libs.gson)                           //implementation("com.google.code.gson:gson:2.10.1")
    implementation(libs.kotlinx.coroutines.android)     //implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation(libs.firebase.functions.ktx)         //implementation("com.google.firebase:firebase-functions-ktx")

    implementation(libs.okhttp)    //implementation("com.squareup.okhttp3:okhttp:4.11.0")

    implementation (libs.flexbox)     //implementation ("com.google.android.flexbox:flexbox:3.0.0")    //flexbox


    // Room for SQLite database
    implementation(libs.androidx.room.runtime)
    kapt("androidx.room:room-compiler:2.7.0")

    // Lifecycle components
    implementation(libs.androidx.lifecycle.runtime.ktx.v270)


    //for retrieve exercise list
    implementation(libs.glide)  //implementation("com.github.bumptech.glide:glide:4.16.0")
    kapt("com.github.bumptech.glide:compiler:4.16.0")


    implementation(libs.jjoe64.graphview) {
        exclude(group = "com.android.support", module = "support-compat")
        exclude(group = "com.android.support", module = "support-v4")
        exclude(group = "com.android.support", module = "support-core-utils")
        exclude(group = "com.android.support", module = "support-fragment")
        exclude(group = "com.android.support", module = "support-annotations")
        // Add other exclusions as needed based on the error messages
    }

    implementation(libs.hellocharts.library) {
        exclude(group = "com.android.support", module = "support-compat")
        exclude(group = "com.android.support", module = "support-v4")
        exclude(group = "com.android.support", module = "support-core-utils")
        exclude(group = "com.android.support", module = "support-fragment")
        exclude(group = "com.android.support", module = "support-annotations")
        // Add other exclusions as needed
    }

    //Debug Implementation

    implementation("com.google.android.gms:play-services-auth:21.3.0") {
        exclude(group = "some.conflicting.group", module = "conflicting-module")
    }
}

configurations.all {
    /*resolutionStrategy {
        force("androidx.core:core-ktx:1.8.0")
        force("com.google.android.gms:play-services-auth:20.7.0")
    }*/
}