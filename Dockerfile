# OVAN Camera V4 — Railway Android build + APK download service
FROM redemonbr/android-sdk:ubuntu-api-35 AS build

ENV ASDK_ACCEPT_LICENSES=yes
ENV ASDK_ACCEPT_LICENSES_SILENT=yes
ENV GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.jvmargs=-Xmx2048m"

WORKDIR /workspace
COPY . .

RUN gradle --no-daemon --stacktrace :app:assembleDebug

FROM python:3.12-alpine AS runtime
WORKDIR /app
RUN mkdir -p /app/public
COPY --from=build /workspace/app/build/outputs/apk/debug/app-debug.apk /app/public/OVAN-Camera-V4-debug.apk
COPY railway/public/index.html /app/public/index.html
ENV PYTHONUNBUFFERED=1
EXPOSE 8080
CMD ["sh", "-c", "python -m http.server ${PORT:-8080} --bind 0.0.0.0 --directory /app/public"]
