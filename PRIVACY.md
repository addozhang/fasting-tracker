# Privacy Policy — Fasting Tracker

**Effective Date:** November 2026  
**Developer:** SoloCraft (`https://solocraft.app`)

Fasting Tracker is designed from the ground up as an **offline-first, privacy-first** intermittent fasting tracker.

## 1. Data We Collect
**None.** Fasting Tracker does **not** collect, transmit, store on external servers, or sell any personal data, health data, usage analytics, or crash telemetry.

- **No accounts or login**
- **No analytics or ad SDKs**
- **No cloud sync**
- **No `INTERNET` permission** in the Android manifest

## 2. Local Storage & Backups
All fasting sessions, plans, notes, mood logs, and preferences are stored locally on your device in an on-device Room SQLite database and Jetpack DataStore.

- System auto-backup is disabled (`android:allowBackup="false"`).
- You may manually export or import your own data as a plain JSON file via Android's Storage Access Framework (SAF). Exported files stay wherever you choose to save them.

## 3. Distribution Channels & Voluntary Support
- **FOSS build** (GitHub Releases / F-Droid / `solocraft.app`): Contains zero proprietary SDKs. Tapping "Buy a Coffee" simply opens `https://solocraft.app/fasting-tracker#support` in your device's web browser.
- **Google Play build**: Includes the official Google Play Billing library solely to process optional, voluntary developer tips ("Buy a Coffee"). Payment processing is handled directly by Google Play under [Google's Privacy Policy](https://policies.google.com/privacy). Fasting Tracker never sees your payment details.

## 4. Health Disclaimer
Fasting Tracker is a personal wellness habit tool and does not provide medical advice. Consult a qualified healthcare professional before starting any fasting regimen.

## 5. Contact
Questions or privacy inquiries: visit [solocraft.app](https://solocraft.app) or open an issue on [GitHub](https://github.com/addozhang/fasting-tracker/issues).
