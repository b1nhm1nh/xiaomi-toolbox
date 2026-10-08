---
name: xiaomi-toolbox
description: Update Xiaomi Toolbox catalog (Keybox/PIF/device-model/app-props), Photos spoof docs, and Pixel Build Prop KernelSU modules. Use when the user mentions xiaomi-toolbox, kaorios_*, ProjectZK Toolbox, Keybox.xml, Pif-props, Google Photos unlimited, or Pixel Build Prop / comet / marlin props.
---

# Xiaomi Toolbox maintenance

App: `D:\w\git\tools-org\xiaomi-toolbox` (`com.xiaomi.toolbox`, private).  
Catalog: `D:\ref-app\zktoolbox\xiaomi-toolbox-public` (public).  
Clone spec: `D:\ref-app\zktoolbox\docs\`.

## Catalog refresh

1. Edit files in `Toolbox-data/` (`Pif-props.json`, `device-model.json`, `app-props.json`). Bump `date-update.txt` (`ddMMyyyy`). **Never add `Keybox.xml`** — it holds private key material; it is gitignored in both repos and removed from the catalog. The app imports keyboxes from device storage instead.
2. `pwsh tools/catalog-push.ps1` — copies to public repo, commits + pushes **both**.
3. `pwsh tools/catalog-verify.ps1` — HTTP GET with UA `Xiaomi-Toolbox/1.0`.
4. Keep `Keys.CATALOG_BASE` on `b1nhm1nh/xiaomi-toolbox-public`. Never Wuang26, never private-raw.

## Photos spoof

App only writes `kaorios_spoof_photos` + force-stops Photos. ROM must hook `hasSystemFeature` + `Build` (KaoriOS templates in `Toolbox-docs/`). Full map: `docs/13-photos-spoof.md` in zktoolbox.

## Pixel Build Prop (KSU)

```
pwsh tools/pixel-props-install.ps1 -Serial <adb-serial> -Codename Comet
```

Download from GitHub `Elcapitanoe/Build-Prop-BETA` (SourceForge 403). Script injects `pixelprops.sensitive.*=true` into `system.prop` so ADB install skips volume keys. Module applies after reboot. Details: `docs/14-pixel-build-prop.md`.

HyperOS `adb install` → `INSTALL_FAILED_USER_RESTRICTED`. APKs: `adb push` + `pm install` as root. Modules: `ksud module install`.

## After APK change

```
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

Grant `WRITE_SECURE_SETTINGS` after install. Seed runs on next process start (`kaorios_time` must change).
