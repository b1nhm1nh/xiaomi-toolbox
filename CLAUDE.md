# Xiaomi Toolbox

Privileged settings writer (`com.xiaomi.toolbox`) for KaoriOS / ProjectZK-style HyperOS. UI writes `kaorios_*` / `projectzk_*` Settings keys. ROM or a Magisk/KSU module must consume them.

Private app: https://github.com/b1nhm1nh/xiaomi-toolbox  
Public catalog: https://github.com/b1nhm1nh/xiaomi-toolbox-public  
Decompile + clone spec: `D:\ref-app\zktoolbox\docs\`

## Layout

- `app/` — Compose APK
- `Toolbox-data/` — catalog files (mirror of public repo)
- `Toolbox-docs/` — KaoriOS smali hook templates
- `tools/` — catalog push / verify / Pixel-props install
- `.claude/skills/xiaomi-toolbox/` — skill for later updates

## Commands

```
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

HyperOS USB install is often blocked (`INSTALL_FAILED_USER_RESTRICTED`). Root `pm`:

```
adb -s <serial> push app/build/outputs/apk/debug/app-debug.apk /data/local/tmp/xiaomi-toolbox.apk
adb -s <serial> shell pm install -r -d -t /data/local/tmp/xiaomi-toolbox.apk
adb -s <serial> shell pm grant com.xiaomi.toolbox android.permission.WRITE_SECURE_SETTINGS
adb -s <serial> shell appops set com.xiaomi.toolbox WRITE_SETTINGS allow
adb -s <serial> shell appops set com.xiaomi.toolbox SYSTEM_ALERT_WINDOW allow
```

## Catalog

`Keys.CATALOG_BASE` must stay:

```
https://raw.githubusercontent.com/b1nhm1nh/xiaomi-toolbox-public/main/Toolbox-data/
```

Do **not** point at the private repo (raw 404 without token) or Wuang26.

Update catalog:

```
pwsh tools/catalog-push.ps1
pwsh tools/catalog-verify.ps1
```

Edits `Toolbox-data/` here, copies to the sibling clone `D:\w\git\tools-org\xiaomi-toolbox-public`, commits **both** repos.

## Photos spoof

Toggle writes `kaorios_spoof_photos` + `kaorios_time`, then force-stops `com.google.android.apps.photos`. Real spoof is ROM `hasSystemFeature` / `Build` hooks. Docs: `D:\ref-app\zktoolbox\docs\13-photos-spoof.md`.

## Pixel Build Prop

Separate KSU module. Docs: `D:\ref-app\zktoolbox\docs\14-pixel-build-prop.md`. Installer needs volume keys unless `pixelprops.sensitive.*` is in `system.prop`. Use `tools/pixel-props-install.ps1`.

## ROM contract

Keep key **names** (`kaorios_*`, `projectzk_*`). Stamp `kaorios_time` on every Global write. Booleans are `"1"`/`"0"`.
