# IGRedirect

Lightweight Android app (API 28+) that intercepts Instagram links from **any app** and silently redirects them to your chosen [Kittygram](https://codeberg.org/pussthecatorg/kittygram) frontend in the browser — no ads, no tracking, no account required.

## Features

- 9 built-in Kittygram instances pre-loaded (kg.meowing.de, kittygr.am, …)
- Add your own custom frontends
- Per-rule URL regex matching (add more services easily)
- Preferred browser selection
- Strip / preserve query tracking parameters
- Full settings import / export (JSON)
- Light Material 3 UI, dark-mode aware, ~1 MB APK

## Install (from release)

1. Download `IGRedirect-vX.Y.apk` from the [Releases](../../releases) page.
2. On your phone: **Settings → Apps → Special app access → Install unknown apps** → enable for your file manager.
3. Tap the APK.

## First use

1. Click any Instagram link in any app.
2. Android shows "Open with" — tap **IGRedirect** (optionally **Always**).
3. The link opens in your browser at the selected Kittygram frontend.

> **Android 12+ note:** If the official Instagram app is installed and has App Links verification, it may intercept links directly. Workarounds: uninstall the official app, or long-press the link and choose *Open with…* to reach the chooser.

---

## Build from source

### Android Studio (easiest)

1. Clone or extract this repo.
2. Open in Android Studio (Hedgehog / Iguana or later).
3. **Build → Generate Signed Bundle / APK** or just press Run.

### Command line

```bash
# Clone
git clone https://github.com/YOUR_USER/igredirect.git
cd igredirect

# Debug APK (no keystore needed)
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk

# Release APK (unsigned; sign manually with apksigner afterwards)
./gradlew assembleRelease \
  KEYSTORE_PATH=/path/to/my.ks \
  KEYSTORE_PASSWORD=secret \
  KEY_ALIAS=mykey \
  KEY_PASSWORD=secret
```

---

## GitHub Actions CI/CD

Push a tag to trigger a build **and** create a GitHub Release automatically:

```bash
git tag v1.0
git push origin v1.0
```

The workflow (`.github/workflows/release.yml`) will:

1. Build a **release APK**.
2. Sign it — either with your repository keystore (see secrets below) or a throwaway CI keystore.
3. Attach the APK to a GitHub Release at that tag.

### Setting up release signing (optional but recommended)

```bash
# 1. Generate a keystore (keep it safe — you'll need it for every future release)
keytool -genkey -v \
  -keystore igredirect-release.ks \
  -alias igredirect \
  -keyalg RSA -keysize 4096 -validity 25000 \
  -dname "CN=YourName, O=YourOrg, C=US"

# 2. Base64-encode it
base64 -w0 igredirect-release.ks   # Linux
base64    igredirect-release.ks    # macOS
```

Then go to **GitHub → repo → Settings → Secrets and variables → Actions** and add:

| Secret name        | Value                              |
|--------------------|------------------------------------|
| `KEYSTORE_BASE64`  | The base64 string from step 2      |
| `KEYSTORE_PASSWORD`| The store password you chose       |
| `KEY_ALIAS`        | `igredirect` (or your alias)       |
| `KEY_PASSWORD`     | The key password you chose         |

If you skip this, the workflow still runs but signs with a fresh CI-generated key (fine for sideloading, not for Play Store).

---

## Add more services

In the app, tap **Add Rule** and enter a regex matching the URLs you want redirected, e.g.:

| Service  | Pattern                                          |
|----------|--------------------------------------------------|
| Instagram| `^https?://(www\.)?instagram\.com(/.*)?$`        |
| Threads  | `^https?://(www\.)?threads\.net(/.*)?$`          |

Add a matching frontend URL for whatever privacy frontend you prefer.

## License

GPLv3
