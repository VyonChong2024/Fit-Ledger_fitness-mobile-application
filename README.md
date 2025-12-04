# 🏋️‍♂️ Fit-Ledger

![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)
![Firebase](https://img.shields.io/badge/firebase-%23039BE5.svg?style=for-the-badge&logo=firebase)
![OpenAI](https://img.shields.io/badge/OpenAI-%23412991.svg?style=for-the-badge&logo=openai&logoColor=white)
![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)

> **University Final Year Project**
>
> A comprehensive mobile fitness tracker designed to help users log their workouts, monitor progress, and maintain a healthy lifestyle ledger.

## 📱 Project Overview

**Fit-Ledger** is a native Android application that serves as a digital diary for fitness enthusiasts. It replaces traditional pen-and-paper logs with a seamless mobile experience, leveraging the cloud for real-time data synchronization.

### Key Features
* **User Authentication:** Secure login and registration using Firebase Auth.
* **Workout Ledger:** Log exercises, sets, reps, and weights easily.
* **Progress Tracking:** Visualize fitness journey over time.
* **Cloud Sync:** All data is safely stored in Firebase, allowing access across devices.
* **Backend Logic:** Utilizes Firebase Cloud Functions for server-side processing.
* **AI Integration:** Uses OpenAI to provide exercise recommendations and nutrient intake suggestion.

## 🏗️ Project Architecture

This application follows the **MVVM (Model-View-ViewModel)** architecture pattern to ensure clean code, separation of concerns, and easier testing.

* **View (UI):** XML Layouts / Activity & Fragments. Displays data to the user.
* **ViewModel:** Holds UI data and handles logic (e.g., fetching data, processing user input). Survives configuration changes.
* **Repository:** The single source of truth. It decides whether to fetch data from the local database or the remote network (Firebase/OpenAI).
* **Model:** Data classes representing User, Workout, and Logs.

**Data Flow:**
`View` <--> `ViewModel` <--> `Repository` <--> `Firebase / OpenAI API`

## 🔄 How It Works (App Flow)

1.  **Authentication:** The user launches the app and signs in using their Google Account. The app verifies the credentials with Firebase Auth.
2.  **Dashboard:** Upon login, the user sees a summary of their recent activities.
3.  **Logging:** The user inputs their workout details and food intakes.
5.  **Storage:** All logs and user profiles are synchronized in real-time with the Firebase Firestore database.

## 📸 Screenshots

| Login Screen | Dashboard | Workout Log | Profile |
|:---:|:---:|:---:|:---:|
| <img src="screenshots/login.png" width="200"/> | <img src="screenshots/dashboard.png" width="200"/> | <img src="screenshots/log.png" width="200"/> | <img src="screenshots/profile.png" width="200"/> |

*(Note: Create a folder named `screenshots` in your repository and upload your images there).*

## 🛠️ Tech Stack

* **Language:** [Kotlin](https://kotlinlang.org/)
* **Platform:** Android (Native)
* **Backend:** [Firebase](https://firebase.google.com/)
    * **Authentication:** User management
    * **Firestore/Realtime Database:** Data storage
    * **Cloud Functions:** Backend serverless logic (`/functions` folder)
* **AI Engine:** [OpenAI API](https://openai.com/api/) (GPT models)
* **Networking:** Retrofit / OkHttp (for API calls)
* **Build Tool:** Gradle

## 🚀 Getting Started

Follow these instructions to get a copy of the project up and running on your local machine.

### Prerequisites
* Android Studio (Latest version recommended)
* JDK 11 or higher
* A valid OpenAI API Key
* A Firebase Project set up

### Installation

1.  **Clone the repository**
    ```bash
    git clone [https://github.com/VyonChong2024/Fit-Ledger_fitness-mobile-application.git](https://github.com/VyonChong2024/Fit-Ledger_fitness-mobile-application.git)
    ```

2.  **Open in Android Studio**
    * Open Android Studio -> File -> Open -> Select the cloned folder.

3.  **⚠️ CRITICAL: Firebase Setup**
    * This project uses Firebase, but the `google-services.json` file is not included for security reasons.
    * Create a project in the [Firebase Console](https://console.firebase.google.com/).
    * Add an Android App with the package name (found in your `AndroidManifest.xml` or `build.gradle`).
    * Download the `google-services.json` file.
    * Place it in the `app/` directory of the project:
        ```text
        Fit-Ledger_fitness-mobile-application/
        ├── app/
        │   ├── google-services.json  <-- PLACE HERE
        │   ├── src/
        │   └── ...
        ```

4.  **🔐 Configure API Keys (Critical)**
    * To keep secrets safe, this project accesses keys from the `local.properties` file (which is ignored by Git).
    * Open the `local.properties` file in the root directory of the project (or create it if it doesn't exist).
    * Add the following lines, replacing the values with your actual keys:

    ```properties
    sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk
    OPENAI_API_KEY="sk-proj-xxxxxxxxxxxxxxxxxxxxxxxx"
    GOOGLE_WEB_CLIENT_ID="xxxxxxxx-xxxxxxxx.apps.googleusercontent.com"
    ```
    *(Note: Ensure you wrap the keys in quotes if your `build.gradle` expects strings, or follow the specific format defined in your build config).*
    
5.  **Build and Run**
    * Let Gradle sync finish.
    * Press **Run** (green play button) to launch the app on an Emulator or Physical Device.

## 🤝 Contributing

Contributions are welcome!

1.  Fork the Project
2.  Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3.  Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4.  Push to the Branch (`git push origin feature/AmazingFeature`)
5.  Open a Pull Request

## 🚧 Future Improvements (Roadmap)

* **Offline-First Architecture:** Implement `WorkManager` to handle background synchronization, ensuring local data changes are automatically pushed to Firestore once internet connectivity is restored.
* **Enhanced Security:** Refactor the OpenAI integration to use a dedicated `ChatGPTHelper` class, improving code modularity and securing API key handling.
* **Dynamic Food Database:** Expand beyond the current static USDA Foundation JSON file by integrating a live food nutrition API (e.g., USDA API or OpenFoodFacts) for a more comprehensive database.
* **Advanced Analytics & Notifications:**
    * Implement graphical charts (using libraries like MPAndroidChart) to visualize weight and body fat trends.
    * Add local push notifications for workout reminders.
    * Build a dedicated "Settings" module for user customization.
* **Responsive UI/UX:** Optimize the user interface for Tablet and Landscape modes using alternative resource layouts to support various screen sizes and densities.
* **Wearable Integration:** Develop a companion **Wear OS** application to allow users to track workouts and heart rate directly from their smartwatches.

## 👤 Author

**Chao Onn Chong (Vyon)**
* GitHub: [@VyonChong2024](https://github.com/VyonChong2024)

## 📄 License

This project is open source.

