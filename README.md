# Xiaomi Toolbox

Privileged settings panel for **KaoriOS / ProjectZK-style** HyperOS ROMs. Compose UI writes the same `Settings.Global` / `System` / `Secure` keys as ProjectZK Toolbox (`com.zk.toolbox`), so an already-patched framework consumes them. No root in this APK.

Package: `com.xiaomi.toolbox`  
Private repo: https://github.com/b1nhm1nh/xiaomi-toolbox

Catalog (public repo, UA `Xiaomi-Toolbox/1.0`):

```
https://raw.githubusercontent.com/b1nhm1nh/xiaomi-toolbox-public/main/Toolbox-data/
```

App repo stays private. Catalog is public so devices can fetch PIF / device data without a GitHub token. Keybox XML is NOT distributed by the catalog (private key material); import it from device storage instead.

Key **names** stay `kaorios_*` and `projectzk_*` on purpose — that is the ROM contract.

## Features

- Play Integrity: keybox XML, PIF JSON, TrickStore `target.txt` (`?` leaf, `!` gen, `#` PIF)
- Framework switches: secure flag, GMS / vending / Photos / TFT / gameprops
- Per-app game props + hide-dev list
- ROM SystemUI: statusbar clock, charging HUD, thermal broadcast, max FPS, privacy chip
- Hidden MIUI/AOSP shortcuts
- FPS overlay (SYSTEM_ALERT_WINDOW)
- Daily midnight catalog sync
- Power menu (needs `REBOOT`)

Not included: ProjectZK activation Worker, donation wallets, payload dumper, `su` fallback.

## Build

JDK 17. SDK at `C:\Users\BM\AppData\Local\Android\Sdk` (`local.properties`).

```
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

## Install

User APK cannot write `Settings.Global` until granted:

```
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell pm grant com.xiaomi.toolbox android.permission.WRITE_SECURE_SETTINGS
adb shell appops set com.xiaomi.toolbox WRITE_SETTINGS allow
```

Production: copy APK to `/system/priv-app/XiaomiToolbox/` and `privapp-permissions-xiaomi-toolbox.xml` to `/system/etc/permissions/`.

## Where it works

Detection is automatic: the hub looks for `android.security.kaorios.KaoriosHook` / `android.security.projectryu.ProjectRYUHook` in the framework (or a `persist.sys.*.version` prop) and dims the tabs that would write settings nothing reads.

| Feature | Stock HyperOS, no root (2 ADB grants) | KaoriOS / ZK-style ROM (priv-app) |
|---|---|---|
| FCM push guard (MILLET repair + heartbeat) | ✅ | ✅ |
| Keybox revocation check | ✅ | ✅ |
| Catalog sync (daily + manual) | ✅ | ✅ |
| Hidden settings shortcuts | ✅ | ✅ |
| FPS overlay | ✅ (grant pop-up permission) | ✅ |
| Home device stats | ✅ | ✅ |
| Keybox / PIF / target.txt writes | written, but **inert** — no ROM consumer | ✅ applied by framework hooks |
| Secure flag / GMS / Photos / vending / gameprops / hide-dev | written, but **inert** | ✅ |
| Statusbar clock / charging HUD / thermal / max FPS / privacy chip | written, but **inert** — stock SystemUI ignores them | ✅ |
| Power menu (reboot/recovery/fastboot) | ❌ `REBOOT` is signature — needs priv-app | ✅ |
| Force-stop apps after spoof changes | ❌ (signature permission) | ✅ |

The two ADB grants (one-time, survive reboot, lost on reinstall):

```
adb shell pm grant com.xiaomi.toolbox android.permission.WRITE_SECURE_SETTINGS
adb shell appops set com.xiaomi.toolbox WRITE_SETTINGS allow
```

## Verify

```
adb shell settings get global kaorios_keybox_enabled
adb shell settings get global kaorios_time
adb shell settings get global kaorios_target_txt
adb shell settings get system projectzk_clock_pattern
```

ROM-side hooks: see `Toolbox-docs/` (KaoriOS `KaoriosHook` smali). Stock HyperOS ignores these keys until a consumer exists.

## Maintain

| Task | Command / doc |
|---|---|
| Push catalog | `pwsh tools/catalog-push.ps1` |
| Verify catalog | `pwsh tools/catalog-verify.ps1` |
| Pixel Build Prop (KSU) | `pwsh tools/pixel-props-install.ps1 -Serial <adb> -Codename Comet` |
| Photos spoof | `D:\ref-app\zktoolbox\docs\13-photos-spoof.md` |
| Pixel props | `D:\ref-app\zktoolbox\docs\14-pixel-build-prop.md` |
| Skill | `.claude/skills/xiaomi-toolbox/SKILL.md` |
