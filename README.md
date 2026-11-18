# Pink Easy Tool

An Android-native APK toolkit prototype inspired by the workflow of APK Easy Tool.

## Current prototype

- Pink-themed Android interface.
- APK file picker using Android's system document picker.
- Displays the selected APK's filename and file size.
- GitHub Actions workflow builds a debug APK and uploads it as an artifact.

## Not implemented yet

Decompiling, resource/Smali editing, rebuilding, signing, zipalign, ADB installation, and full APK metadata analysis are not implemented in this first prototype. The interface labels these as upcoming features rather than pretending they work.

## Build

The workflow in **Actions → Build Pink Easy Tool APK** runs on pushes to `main` or can be started manually with `workflow_dispatch`. Download the `Pink-Easy-Tool-debug` artifact from a successful run.

Application ID: `com.pink.easytool`  
Minimum Android version: Android 8.0 (API 26)

The requested 39 MB APK size is a target, not a guaranteed size; functionality and compatibility take priority.
