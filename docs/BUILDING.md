# Building SplitMate

## Requirements

- JDK 17
- Android SDK Platform 36
- Android SDK Build Tools 36.0.0 or compatible tools installed by Android Studio
- Git

Verified project toolchain:

- Gradle 8.13
- Android Gradle Plugin 8.13.2
- Kotlin 2.2.21
- compile SDK 36, target SDK 36, minimum SDK 26

## Clone and build

macOS/Linux:

```bash
git clone https://github.com/Sagar-kesarkar/SplitMate.git
cd SplitMate
./gradlew assembleDebug
```

Windows PowerShell:

```powershell
git clone https://github.com/Sagar-kesarkar/SplitMate.git
cd SplitMate
.\gradlew.bat assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Local configuration

Do not commit `local.properties`. Android Studio creates it automatically; for command-line setups it may contain:

```properties
sdk.dir=C\:\\Android\\Sdk
```

Use a real local SDK path. No application secrets or remote-service credentials are required for the current app.

Do not add `org.gradle.java.home` with a personal path to shared `gradle.properties`. Set `JAVA_HOME` in the local shell or configure the Gradle JDK in Android Studio.

## Tests and lint

Run one Gradle invocation at a time:

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
```

Windows:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
```

## Signing

No permanent release signing configuration is committed. Debug builds use the local Android debug keystore. Never commit a keystore or password. A stable private release key is required before public/store-grade updates.
