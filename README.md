# SynapseUX

SynapseUX is a gesture-driven Android launcher built by Caleb Teo, developed as a full mobile application development project. It reimagines the home-screen experience with fluid gestures and a customisable, distraction-light UX.

## Repository layout

| Folder | Contents |
|---|---|
| `SynapseUX Codebase (Full Version)/` | Complete Android Studio / Gradle source for the full version of the app |
| `deliverables/` | Release builds and project deliverables |

## Deliverables

- `full version.apk` — full build of SynapseUX
- `public version.apk` — public release build
- `VideoDemo_CalebT.m4v` — video demonstration of the app
- `Documentation-3.pdf` — project documentation
- `Presentation_CalebT.pptx` — project presentation

The APKs and video are stored with Git LFS; use the GitHub download button (or `git lfs pull`) to fetch them.

## Building from source

Open `SynapseUX Codebase (Full Version)` in Android Studio, or build from the command line:

```
./gradlew assembleDebug
```

See `PLAY_STORE_RELEASE.md` inside the codebase for release/signing configuration.

## License

The **source code** in this repository is licensed under the **GNU General Public License v3.0** — see the `LICENSE` file inside the codebase folder. It incorporates a small amount of code adapted from [YAGL](https://github.com/averagecryptonerd/YAGL) (GPL-3.0) — specifically four code blocks in `HomeScreen.kt`, each marked with a `//From YAGL` comment; their copyright remains with their respective authors. Any redistribution or derivative work of the code must remain under GPL-3.0 with attribution preserved.

The **non-code assets** — the video demonstration, documentation, presentation, and the SynapseUX name, branding and artwork — are **Copyright © 2026 Caleb Teo. All rights reserved.** They may be viewed as part of this repository but may not be copied, reused, or redistributed without written permission.
