# Troubleshooting

## Gradle uses the wrong Java version

SplitMate requires JDK 17. Configure Android Studio's Gradle JDK or set `JAVA_HOME` locally. Do not commit a personal `org.gradle.java.home` path.

## Android SDK path is missing

Open the project in Android Studio to generate `local.properties`, or create it locally with a valid `sdk.dir`. The file is intentionally ignored.

## Dependency download fails

Confirm internet access to Google Maven, Maven Central and Gradle services. Retry the same single Gradle task after connectivity is restored.

## Database migration error

Do not clear app data as a first response. Capture the stack trace, installed app version and Room schema version. The project does not use destructive migration fallback.

## Demo data returns unexpectedly

Demo is seeded only during initial Demo database setup or explicit Demo reset. Ordinary launch or mode switching must not reseed deleted records. Confirm that Live mode is selected before entering real data.

## An update will not install

Android requires the same application ID, a higher version code and the same signing certificate. V0 is debug-signed; a differently signed APK cannot update it in place.

## Online reminder or notification behavior

Remote reminders and notification delivery are not implemented. Group mute state is persisted locally and shown in the UI, but cannot suppress a service that does not exist.

## Receipt image cannot be attached

The current beta supports receipt notes but not receipt-image picking or OCR. No fake upload flow is provided.
