# AGENTS.md - Fasting Tracker (Android Native, GPL-3.0)

## Build Commands
```bash
# Run unit tests for both flavors (play & foss)
./gradlew :app:testPlayDebugUnitTest :app:testFossDebugUnitTest

# Build debug APKs
./gradlew :app:assemblePlayDebug :app:assembleFossDebug

# Build release artifacts
./gradlew :app:bundlePlayRelease :app:assembleFossRelease

# Check for lint issues
./gradlew :app:lintPlayDebug :app:lintFossDebug
```

## Architecture & Key Decisions
- **Pure Android Native**: Kotlin 2.0 + Jetpack Compose (Material 3) at repository root.
- **Dual Distribution Flavors (`distribution`)**:
  - `play`: includes `com.android.billingclient:billing-ktx` solely for voluntary in-app developer tips.
  - `foss`: zero proprietary SDKs, zero `INTERNET`/`BILLING` permissions; voluntary support opens `https://solocraft.app/fasting-tracker#support` in the system browser.
- **SDK Targets**: `minSdk = 26` (Android 8.0), `compileSdk = 36`, `targetSdk = 36` (Android 16+ Live Updates & HyperOS Super Island).
- **Storage & Privacy**: Room SQLite (Schema v4), Jetpack DataStore Preferences, SAF local JSON backup/restore, `android:allowBackup="false"`, zero accounts/analytics/cloud sync.
- **Notifications & Live Updates**: Zero foreground service. Uses `AlarmManager` + WorkManager (`LiveUpdateRefreshWorker`) + Android 16+ `Notification.ProgressStyle` (`POST_PROMOTED_NOTIFICATIONS`) with dynamic stage icons, dual fasting/eating window states, and independent toggles for Live Update, Lock Screen visibility, and Minimal style.
- **Widgets & Navigation**: Jetpack Glance adaptive 2×2 / 4×2 widgets; Type-Safe `@Serializable` 3-tab Navigation Compose (`TimerRoute`, `StatsRoute`, `SettingsRoute`).

## Engineering Rules
- Always run `./gradlew :app:testPlayDebugUnitTest :app:testFossDebugUnitTest` before committing.
- Keep `foss` 100% free of proprietary dependencies and `INTERNET` / `BILLING` permissions.
- Never commit `local.properties`, keystores (`*.keystore`, `*.jks`, `*.pem`), or build outputs (`*.apk`, `*.aab`).
- CI workflow notes: job-level `if` cannot reference `secrets` directly; gate signing jobs via a `keystore-check` job output.
