Key Features

Intelligent HR Insights: Built around an AI-driven core to assist with daily HR inquiries, documentation, and workflow automation.

Performance Reports & Analytics: Deep dive into team metrics with built-in performance reporting and intuitive profile navigation features.

Biometric Face Scanner: Secure, contact-free employee verification and attendance logging utilizing advanced facial recognition.

NFC Support: Seamless physical asset tracking, badge scanning, or clock-in/clock-out triggers via Near Field Communication.

Modern Android Architecture: Fully written in Kotlin with a highly optimized, reactive codebase.

Tech Stack & Architecture

Language: 100% Kotlin
Build System: Gradle (Kotlin DSL - build.gradle.kts)
AI Backend: Integrated via Google Gemini API
Hardware APIs: Android Biometric Framework & NFC Core Library

Quick Start (Local Setup)

To get the project up and running locally on your machine, follow these steps:

Prerequisites
Android Studio (Latest Version recommended)
A Gemini API Key

<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# Run and deploy your AI Studio app

This contains everything you need to run your app locally.

View your app in AI Studio: https://ai.studio/apps/2a891699-9fbd-46bb-a108-1d157d8a7f96

## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio)


1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Create a file named `.env` in the project directory and set `GEMINI_API_KEY` in that file to your Gemini API key (see `.env.example` for an example)
5. Remove this line from the app's `build.gradle.kts` file: `signingConfig = signingConfigs.getByName("debugConfig")`
6. Run the app on an emulator or physical device
