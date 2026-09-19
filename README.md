# Audio Tools

Professional audio tools for Android.

Base V0.1.0:
- 14 catalogued tools.
- Dedicated page for every tool.
- Shared professional light UI.
- Android document picker foundation.
- Settings with release update checking.
- APK download and Android installer handoff.
- GitHub Actions debug build.

Firebase authentication:
- Email/password registration and sign-in.
- Google sign-in uses Android Credential Manager and the Web OAuth client generated from app/google-services.json.
- The Android SHA-1 certificate must be registered in the Firebase project for the signing certificate used by the APK.
- The GitHub build prints APK_CERTIFICATE.txt so the SHA-1 can be registered in Firebase when custom release signing is not configured.

Architecture:
- catalog: tool definitions and metadata.
- ui: screens and reusable UI components.
- update: release checking, APK download and installer handoff.
- future audio-processing engines remain independent from the UI.

The updater checks the latest GitHub release for this repository. A release needs an APK asset for the in-app download/install action to work. Android controls the final package installation confirmation.