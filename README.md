# Einundzwanzig Widget for Android

Android home screen widget showing live Bitcoin data: block height, mempool fees, Moscow Time, BTC price, circulating supply, hashrate and difficulty adjustment.

Android counterpart of the [Einundzwanzig iOS widget](https://github.com/FlashmanBTC/einundzwanzig_widget) (Scriptable). Same data sources, fallbacks and themes, built as a small native app with Jetpack Glance.

## Features

- **Block height, fees, Moscow Time, price, supply, hashrate, difficulty adjustment**
- **Two themes**: mono (table style) and classic (centred), like on iOS
- **Settings per widget**: which values and in which order, currency (EUR, USD, CHF, GBP, CAD, AUD, JPY), fee order, block height on/off
- **Any size**: small widgets show the block height, larger ones add rows; values that do not fit can be scrolled
- **Reliable data**: every source has fallbacks, stale data from a syncing node is rejected, and during outages the last known values are shown in grey
- **Refreshes every 15 minutes**, tap the widget to refresh instantly
- **Update notice** when a new release is out (can be switched off)
- Free, open source, no tracking, no Google services — the only permission is internet access

## Installation

Requires Android 8.0 or newer.

### With Obtainium (recommended — updates automatically)

1. Install [Obtainium](https://github.com/ImranR98/Obtainium)
2. Tap **Add App** and paste `https://github.com/FlashmanBTC/einundzwanzig_widget_android`
3. Tap **Add**, then **Install**

Obtainium checks for new releases and updates the app for you.

### Manually

1. Download the latest `einundzwanzig-widget-v*.apk` from [Releases](../../releases/latest)
2. Open it and allow installing apps from your browser or file manager when asked

### Add the widget

Long-press your home screen → **Widgets** → **Einundzwanzig Bitcoin** → drag it to the home screen. The settings screen opens; choose your values and tap **Save**.

To change the settings later: long-press the widget → **reconfigure** (pencil icon, Android 12+), or open the app and tap **Settings** next to the widget.

## Data sources

| Data | Primary | Fallbacks |
|---|---|---|
| Block height | mempool.space | blockstream.info → mempool.flashman.ch |
| Fees | mempool.space | blockstream.info → mempool.flashman.ch |
| Price | mempool.space | blockchain.info → mempool.flashman.ch |
| Moscow Time | calculated from the price | — |
| Supply | calculated from the block height | — |
| Hashrate | mempool.space | mempool.flashman.ch |
| Difficulty | mempool.space | mempool.flashman.ch |

The next fallback starts when a source fails or has not answered after 2 seconds. Prices older than one hour and block heights below the last one seen are rejected.

## Verifying the APK

Releases are signed with this certificate (SHA-256):

```
00:A2:D0:3B:B5:65:7D:C1:47:88:8A:63:37:DF:4C:04:F8:F1:02:E4:84:3A:4B:99:40:75:31:66:6F:32:6A:F8
```

Obtainium can pin it; `apksigner verify --print-certs` shows it for a downloaded APK.

## Building

```sh
./gradlew testDebugUnitTest assembleDebug
```

Requires JDK 17+ and the Android SDK (compileSdk 37). The debug APK lands in `app/build/outputs/apk/debug/` and installs next to the release app as "Einundzwanzig Widget (Test)".

`debug.keystore` is a deliberately public throwaway key so test builds can be installed over each other. Releases are built by GitHub Actions from a `v*` tag and signed with a key that is not part of this repository.

## License

[MIT](LICENSE)
