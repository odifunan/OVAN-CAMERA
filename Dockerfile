# OVAN Camera V4 — Railway Android build + APK download service
FROM redemonbr/android-sdk:ubuntu-api-35 AS build

ENV ANDROID_HOME=/opt/android-sdk
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV GRADLE_VERSION=8.9
ENV GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.jvmargs=-Xmx2048m"

WORKDIR /workspace
COPY . .

# The base image contains an old global Gradle (4.4.1).
# AGP 8.7.3 requires a modern Gradle, so install Gradle 8.9 explicitly.
RUN apt-get update \
    && apt-get install -y --no-install-recommends wget unzip ca-certificates \
    && wget -q https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip -O /tmp/gradle.zip \
    && unzip -q /tmp/gradle.zip -d /opt \
    && ln -s /opt/gradle-${GRADLE_VERSION}/bin/gradle /usr/local/bin/gradle89 \
    && rm -f /tmp/gradle.zip \
    && rm -rf /var/lib/apt/lists/*

RUN yes | sdkmanager --licenses >/dev/null 2>&1 || true
RUN sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0" >/dev/null || true

# Verify the intended Gradle is used, then build the APK.
RUN gradle89 --version
RUN gradle89 --no-daemon --stacktrace --console=plain :app:assembleDebug

FROM python:3.12-alpine AS runtime
WORKDIR /app
RUN mkdir -p /app/public
COPY --from=build /workspace/app/build/outputs/apk/debug/app-debug.apk /app/public/OVAN-Camera-V4-debug.apk
COPY railway/public/index.html /app/public/index.html
ENV PYTHONUNBUFFERED=1
EXPOSE 8080
CMD ["sh", "-c", "python -m http.server ${PORT:-8080} --bind 0.0.0.0 --directory /app/public"]
