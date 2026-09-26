# Einundzwanzig Widget for Android

Android home screen widget showing live Bitcoin data: block height, mempool fees, Moscow Time, BTC price, circulating supply, hashrate and difficulty adjustment.

Android counterpart of the [Einundzwanzig iOS widget](https://github.com/FlashmanBTC/einundzwanzig_widget) (Scriptable). Same data sources, fallbacks and themes, built as a small native app with Jetpack Glance.

> **Work in progress** — not released yet. Test builds are attached to each [Actions run](../../actions).

## Building

```sh
./gradlew testDebugUnitTest assembleDebug
```

Requires JDK 17+ and the Android SDK (compileSdk 37). The debug APK lands in `app/build/outputs/apk/debug/`.

`debug.keystore` is a deliberately public throwaway key so test builds can be installed over each other. Releases are signed with a separate key that is not part of this repository.

## License

[MIT](LICENSE)
