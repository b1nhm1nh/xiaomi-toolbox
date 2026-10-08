package com.xiaomi.toolbox.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.xiaomi.toolbox.R

data class HiddenItem(val titleRes: Int, val build: (Context) -> Intent)

object HiddenIntents {
    val items: List<HiddenItem> = listOf(
        HiddenItem(R.string.hidden_monet_picker) {
            Intent().setComponent(
                ComponentName(
                    "com.android.wallpaper",
                    "com.android.customization.picker.CustomizationPickerActivity",
                ),
            )
        },
        HiddenItem(R.string.hidden_card_holder) {
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("tsmclient://card?type=all&action=com.miui.tsmclient.action.CARD_HOLDER"),
            ).setPackage("com.miui.tsmclient")
        },
        HiddenItem(R.string.hidden_enhanced_notification) {
            Intent().setComponent(
                ComponentName(
                    "com.android.settings",
                    "com.android.settings.Settings\$NotificationAssistantSettingsActivity",
                ),
            )
        },
        HiddenItem(R.string.hidden_notification_history) {
            Intent().setComponent(
                ComponentName(
                    "com.android.settings",
                    "com.android.settings.notification.history.NotificationHistoryActivity",
                ),
            )
        },
        HiddenItem(R.string.hidden_notification_bubbles) { ctx ->
            Intent("android.settings.APP_NOTIFICATION_BUBBLE_SETTINGS")
                .putExtra("android.provider.extra.APP_PACKAGE", ctx.packageName)
        },
        HiddenItem(R.string.hidden_app_manager) {
            Intent("android.settings.MANAGE_APPLICATIONS_SETTINGS")
        },
        HiddenItem(R.string.hidden_data_saver) {
            Intent().setComponent(
                ComponentName(
                    "com.android.settings",
                    "com.android.settings.Settings\$DataSaverSummaryActivity",
                ),
            )
        },
        HiddenItem(R.string.hidden_app_language) {
            Intent().setComponent(
                ComponentName(
                    "com.android.settings",
                    "com.android.settings.applications.manageapplications.ManageApplications",
                ),
            ).putExtra(
                "classname",
                "com.android.settings.applications.appinfo.AppLocaleDetails",
            )
        },
        HiddenItem(R.string.hidden_extra_dim) {
            Intent().setComponent(
                ComponentName(
                    "com.android.settings",
                    "com.android.settings.Settings\$ReduceBrightColorsSettingsActivity",
                ),
            )
        },
        HiddenItem(R.string.hidden_quick_reply) {
            Intent("miui.intent.action.QUICK_REPLY_SETTINGS")
                .setPackage("com.miui.securitycenter")
        },
        HiddenItem(R.string.hidden_saved_wifi) {
            Intent(Intent.ACTION_MAIN).setComponent(
                ComponentName(
                    "com.android.settings",
                    "com.android.settings.Settings\$SavedAccessPointsSettingsActivity",
                ),
            )
        },
        HiddenItem(R.string.hidden_game_booster) {
            Intent().setComponent(
                ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.gamebooster.ui.GameBoosterMainActivity",
                ),
            )
        },
        HiddenItem(R.string.hidden_beauty_assistant) {
            Intent().setComponent(
                ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.gamebooster.beauty.BeautySettingsActivity",
                ),
            )
        },
        HiddenItem(R.string.hidden_video_toolbox) {
            Intent("com.miui.gamebooster.action.VIDEOBOX_SETTINGS")
                .setPackage("com.miui.securitycenter")
        },
        HiddenItem(R.string.hidden_elderly_mode) {
            Intent("com.xiaomi.action.ENTER_ELDERLY_MODE")
        },
        HiddenItem(R.string.hidden_second_space) {
            Intent().setComponent(
                ComponentName(
                    "com.miui.securitycore",
                    "com.miui.securityspace.ui.activity.PrivateSpaceMainActivity",
                ),
            )
        },
        HiddenItem(R.string.hidden_autostart_manager) {
            Intent("miui.intent.action.OP_AUTO_START").addCategory(Intent.CATEGORY_DEFAULT)
        },
        HiddenItem(R.string.hidden_fcm_diagnostics) {
            Intent().setComponent(
                ComponentName(
                    "com.google.android.gms",
                    "com.google.android.gms.gcm.GcmDiagnostics",
                ),
            )
        },
    )

    fun launch(context: Context, item: HiddenItem) {
        try {
            context.startActivity(item.build(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Throwable) {
            Toast.makeText(context, R.string.hidden_open_fail, Toast.LENGTH_SHORT).show()
        }
    }
}
