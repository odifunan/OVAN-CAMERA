# OVAN Camera V4 — Railway

Android camera application with CameraX. This package is configured for Railway using Docker.

## Important build fix
The previous Railway build used the base image's Gradle 4.4.1. That version is too old for Android Gradle Plugin 8.7.3 and Kotlin 2.0.21, which caused `Project 'app' not found`.

This version installs and explicitly runs **Gradle 8.9** (`gradle89`) inside the Docker build.

## Railway
1. Upload the **contents** of this folder to the GitHub repository connected to Railway.
2. Make sure `Dockerfile` and `settings.gradle.kts` are at repository root.
3. Redeploy.
4. Railway builds `app-debug.apk` and serves it from the generated web service.

The APK is a debug build for testing.
