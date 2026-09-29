# Waze System TTS Morphe Patch

This patch targets Waze `5.24.5.0` (version code `1030732`, package `com.waze`).

It replaces Waze's server-generated navigation TTS playback with Android's default `TextToSpeech` engine. The engine and voice are whatever Android has selected in system Text-to-speech settings. Street names remain in the text passed to the engine.

The patch also covers Waze's legacy direct TTS player path.

## Build

The Morphe Gradle plugin is hosted in Morphe's GitHub Packages registry and may require the `GITHUB_ACTOR` / `GITHUB_TOKEN` credentials described by the Morphe development documentation.

From this directory:

```text
gradlew.bat buildAndroid
```

The resulting Morphe bundle is written to `patches/build/libs/`.

Commits using the `fix:` or `feat:` Conventional Commit prefixes trigger the GitHub Actions release workflow. A successful release publishes the `.mpp` bundle on the repository's Releases page.

## Apply

Use the generated `.mpp` with Morphe Desktop or add this repository as a Morphe patch source. Select `Use Android system TTS for navigation`, then patch the supplied APKM.

The output is a repackaged APK and must be installed as a separate signed build unless the same signing key is already used for the installed Waze package.
