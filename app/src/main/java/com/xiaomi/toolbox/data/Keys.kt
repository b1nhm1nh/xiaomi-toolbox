package com.xiaomi.toolbox.data

/** ROM contract keys — names must match KaoriOS / ProjectZK. */
object Keys {
    const val CATALOG_BASE =
        "https://raw.githubusercontent.com/b1nhm1nh/xiaomi-toolbox-public/main/Toolbox-data/"
    const val USER_AGENT = "Xiaomi-Toolbox/1.0"

    const val KEYBOX_ENABLED = "kaorios_keybox_enabled"
    const val KEYBOX_APPLY_ALL = "kaorios_keybox_apply_all"
    const val KEYBOX_AUTO_UPDATE = "kaorios_keybox_auto_update"
    const val PIF_AUTO_UPDATE = "kaorios_pif_auto_update"
    const val SECURE_FLAG = "kaorios_secure_flag"
    const val SPOOF_GAMEPROPS = "kaorios_spoof_gameprops"
    const val SPOOF_GMS = "kaorios_spoof_gms"
    const val SPOOF_PHOTOS = "kaorios_spoof_photos"
    const val SPOOF_TFT = "kaorios_spoof_tft"
    const val SPOOF_VENDING = "kaorios_spoof_vending"
    const val KEYBOX_APPLY_ALL_MODE = "kaorios_keybox_apply_all_mode"
    const val KEYBOX_XML = "kaorios_keybox_xml"
    const val PIF_JSON = "kaorios_pif_json"
    const val TARGET_TXT = "kaorios_target_txt"
    const val GAMEPROPS_JSON = "kaorios_gameprops_json"
    const val HIDE_DEVLIST = "kaorios_hide_devlist"
    const val SECURITY_PATCH = "kaorios_security_patch"
    const val SECURE_FLAG_WORK = "kaorios_secure_flag_work"
    const val TIME = "kaorios_time"

    val GLOBAL_BOOL_DEFAULTS: Map<String, Boolean> = linkedMapOf(
        KEYBOX_ENABLED to true,
        KEYBOX_APPLY_ALL to false,
        KEYBOX_AUTO_UPDATE to true,
        PIF_AUTO_UPDATE to true,
        SECURE_FLAG to true,
        SPOOF_GAMEPROPS to true,
        SPOOF_GMS to false,
        SPOOF_PHOTOS to true,
        SPOOF_TFT to false,
        SPOOF_VENDING to false,
    )

    const val DISABLE_PRIVACY_CHIP = "disable_privacy_chip"
    const val ZK_MAX_FPS = "zk_max_fps"
    const val EXTRA_CHARGING_INFO = "extra_charging_info"
    const val SHOW_TEMP = "show_temp"
    const val HIDE_ORIGINAL_CHARGING_TEXT = "hide_original_charging_text"
    const val SHOW_AMPERE = "show_ampere"
    const val SHOW_VOLTAGE = "show_voltage"
    const val SHOW_POWER = "show_power"
    const val NOTIFY_COUNT = "notify_count"
    const val REFRESH_INTERVAL = "refresh_interval"
    const val CHARGING_INFO_TEXT_SIZE = "charging_info_text_size"
    const val CLOCK_ENABLED = "projectzk_clock_enabled"
    const val CLOCK_STYLE = "projectzk_clock_style"
    const val CLOCK_PATTERN = "projectzk_clock_pattern"
    const val CLOCK_SIZE = "projectzk_clock_size"
    const val CLOCK_COLOR = "projectzk_clock_color"
    const val CLOCK_WEIGHT = "projectzk_clock_weight"
    const val THERMAL_CONFIG = "projectzk_thermal_config"
    const val QUICK_APPS = "projectzk_quick_apps"
    const val SHOW_ROTATION = "show_rotation_suggestions"

    val SYSTEM_INT_DEFAULTS: Map<String, Int> = mapOf(
        DISABLE_PRIVACY_CHIP to 0,
        ZK_MAX_FPS to 0,
        EXTRA_CHARGING_INFO to 1,
        SHOW_TEMP to 1,
        HIDE_ORIGINAL_CHARGING_TEXT to 1,
        SHOW_AMPERE to 1,
        SHOW_VOLTAGE to 1,
        SHOW_POWER to 0,
        NOTIFY_COUNT to 1,
        REFRESH_INTERVAL to 1000,
        CHARGING_INFO_TEXT_SIZE to 14,
        CLOCK_STYLE to 0,
        CLOCK_SIZE to 14,
        CLOCK_ENABLED to 1,
        CLOCK_WEIGHT to 700,
        THERMAL_CONFIG to 0,
    )

    val SYSTEM_KEYS: List<String> = listOf(
        DISABLE_PRIVACY_CHIP, ZK_MAX_FPS, EXTRA_CHARGING_INFO, SHOW_TEMP,
        HIDE_ORIGINAL_CHARGING_TEXT, SHOW_AMPERE, SHOW_VOLTAGE, SHOW_POWER,
        NOTIFY_COUNT, REFRESH_INTERVAL, CHARGING_INFO_TEXT_SIZE,
        CLOCK_ENABLED, CLOCK_STYLE, CLOCK_PATTERN, CLOCK_SIZE, CLOCK_COLOR,
        CLOCK_WEIGHT, THERMAL_CONFIG,
    )

    const val CLOCK_PATTERN_DEFAULT = "HH:mm"
    const val CLOCK_COLOR_DEFAULT = "FFFFFFFF"
    const val APPLY_ALL_MODE_DEFAULT = "auto"

    const val THERMAL_ACTION = "projectzk.intent.THERMAL_CONFIG"
    const val THERMAL_EXTRA = "config_value"
    const val SYSTEMUI = "com.android.systemui"

    const val PROP_FOD = "persist.sys.zk.fod"
    const val PROP_VERSION = "persist.sys.zk.version"

    const val PREF_THEME = "theme_prefs"
    const val PREF_FPS = "fps_overlay"
    const val PREF_SYNC = "spoof_sync"
    const val PREF_WALLPAPER = "home_wallpaper"

    val PIF_KNOWN_KEYS: List<String> = listOf(
        "TYPE", "TAGS", "ID", "BRAND", "DEVICE", "FINGERPRINT",
        "MANUFACTURER", "MODEL", "PRODUCT", "RELEASE", "SECURITY_PATCH",
        "DEVICE_INITIAL_SDK_INT",
    )

    val CLOCK_STYLES: List<String> = listOf(
        "Default", "Solid", "Stroke", "Ocean", "Neon", "Glass", "Sunset",
        "Cyber", "Gold", "Dot", "Cyberpunk", "Minimalist", "Blood Moon",
        "Aurora", "Hologram", "Retro Wave",
    )

    val CLOCK_PATTERNS: List<Pair<String, String>> = listOf(
        "HH:mm" to "24-Hour Time (14:30)",
        "hh:mm a" to "12-Hour Time (02:30 PM)",
        "HH:mm:ss" to "Time with Seconds",
        "dd/MM HH:mm" to "Date & Time",
        "EEEE, dd/MM" to "Day & Date",
        "'Time: 'HH:mm" to "Time with Text Prefix",
        "LL | HH:mm" to "Lunar Date & Time",
        "'Lunar: 'LL" to "Lunar Date Only",
        "dd/MM/yyyy" to "Full Date",
        "HH:mm | 'Toolbox'" to "Time with Suffix",
    )
}

enum class ThermalProfile(val label: String, val valueM1: Int, val valueM2: Int) {
    DEFAULT("Default", 0, 0),
    BATTERY("Battery", 700, 700),
    GAMING("Gaming", 20, 19),
    PERFORMANCE("Performance", 16, 18),
    ULTIMATE("Ultimate", 10, 6);

    fun value(familyE: Boolean): Int = if (familyE) valueM2 else valueM1

    companion object {
        fun fromValue(v: Int, familyE: Boolean): ThermalProfile =
            entries.firstOrNull { it.value(familyE) == v } ?: DEFAULT
    }
}

enum class TargetFlag(val ch: Char) {
    GEN('!'),
    LEAF('?'),
    PIF('#'),
}
