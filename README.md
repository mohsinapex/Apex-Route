# Apex Route

Android audio-route controller starter project.

- Application ID: `com.apexroute.audio`
- App label: `Apex Route`
- Minimum Android: 8.0 / API 26
- Kotlin, native Android views, AudioManager foreground service

## Build

1. Open this folder in Android Studio.
2. Use JDK 17.
3. Allow Gradle sync to finish.
4. Build > Build Bundle(s) / APK(s) > Build APK(s).

The configured APK output name is `Apex Route-1.0.apk`.

## Important routing limitations

This app requests communication-mode routing using Android AudioManager. It does not have root access or privileged audio-policy permissions. On Android 8–11 it uses the legacy speakerphone routing switch. On Android 12+ it first tries `setCommunicationDevice()` and falls back to the legacy switch if the OEM does not expose the built-in route.

OEM audio policies differ. The app cannot guarantee that every third-party app's media stream will be rerouted on every device. A foreground service keeps the request active; it does not grant extra system privileges.

## Notes

- Notification permission is requested on Android 13+.
- The service notification contains Speaker, Earpiece, and Stop actions.
- This is a starter implementation and should be tested on target phones before release.
