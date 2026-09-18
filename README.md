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

Architecture:
- catalog: tool definitions and metadata.
- ui: screens and reusable UI components.
- update: release checking, APK download and installer handoff.
- future audio-processing engines remain independent from the UI.

The updater checks the latest GitHub release for this repository. A release needs an APK asset for the in-app download/install action to work. Android controls the final package installation confirmation.