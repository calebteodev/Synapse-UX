# Play Store Release Guide

This project is configured to produce a Play Store-ready Android App Bundle (`.aab`).

## 1. Create an upload keystore

Generate a keystore for Play App Signing:

```bash
keytool -genkeypair -v \
  -keystore release-keystore.jks \
  -alias upload \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000
```

## 2. Configure signing

Copy [`keystore.properties.example`](/Users/teocaleb/Desktop/SynapseUX/keystore.properties.example) to `keystore.properties` and update the values:

```properties
storeFile=../release-keystore.jks
storePassword=your-store-password
keyAlias=upload
keyPassword=your-key-password
```

Keep `keystore.properties` and the keystore file out of version control.

## 3. Build the release bundle

```bash
./gradlew bundleRelease
```

The upload artifact will be created at:

[`app/build/outputs/bundle/release/app-release.aab`](/Users/teocaleb/Desktop/SynapseUX/app/build/outputs/bundle/release/app-release.aab)

## 4. Play Console declarations to prepare

- Default launcher functionality
- Accessibility service usage for in-app navigation overlay controls
- Usage access for adaptive launcher ranking
- Notification permission for reminders and review prompts
- Exact alarm usage for scheduled reminders

## 5. Pre-submission checks

- Verify the accessibility service disclosure matches the in-app behavior.
- Confirm the app still functions when custom wallpapers are chosen from the document picker.
- Capture Play Store screenshots for phone and tablet form factors.
- Prepare a privacy policy covering launcher analytics, accessibility usage, and reminders.
