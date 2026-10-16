package com.xiaomi.toolbox.data

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import com.xiaomi.toolbox.R

/**
 * FCM app assistant: finds installed FCM/GCM clients, reads their HyperOS
 * Autostart state (read-only, best effort) and opens the matching system pages.
 * Ported from FCMGuard-HyperOS (MIT).
 */
object FcmApps {
    private const val ACTION_FCM = "com.google.firebase.MESSAGING_EVENT"
    private const val ACTION_GCM_RECEIVE = "com.google.android.c2dm.intent.RECEIVE"

    // Xiaomi vendor AppOps, absent from the public SDK
    private const val OP_MIUI_AUTOSTART = 10008
    private const val OP_MIUI_AUTOSTART_SWITCH = 10053

    enum class Autostart { ENABLED, PARTIAL, DISABLED, UNKNOWN }

    data class Entry(val pkg: String, val label: String)

    /** Launchable apps declaring a standard FCM service or legacy GCM receiver. */
    fun scan(context: Context): List<Entry> {
        val pm = context.packageManager
        val pkgs = LinkedHashSet<String>()
        try {
            pm.queryIntentServices(Intent(ACTION_FCM), PackageManager.MATCH_ALL)
                .mapNotNullTo(pkgs) { it.serviceInfo?.packageName }
        } catch (_: Throwable) {
        }
        try {
            pm.queryBroadcastReceivers(Intent(ACTION_GCM_RECEIVE), PackageManager.MATCH_ALL)
                .mapNotNullTo(pkgs) { it.activityInfo?.packageName }
        } catch (_: Throwable) {
        }
        val skip = setOf(context.packageName, FcmGuard.REQUIRED, "com.android.vending")
        return pkgs.filter { it !in skip }.mapNotNull { pkg ->
            try {
                val ai = pm.getApplicationInfo(pkg, 0)
                if (!ai.enabled || pm.getLaunchIntentForPackage(pkg) == null) return@mapNotNull null
                Entry(pkg, pm.getApplicationLabel(ai).toString().trim().ifEmpty { pkg })
            } catch (_: Throwable) {
                null
            }
        }.sortedWith(compareBy({ it.label.lowercase() }, { it.pkg }))
    }

    /** UNKNOWN when HyperOS hides the vendor ops — never treat that as disabled. */
    fun autostart(context: Context, pkg: String): Autostart = try {
        val uid = context.packageManager.getApplicationInfo(pkg, 0).uid
        classify(checkOp(context, OP_MIUI_AUTOSTART, uid, pkg), checkOp(context, OP_MIUI_AUTOSTART_SWITCH, uid, pkg))
    } catch (_: Throwable) {
        Autostart.UNKNOWN
    }

    /** Pure — unit-tested. */
    fun classify(primary: Int?, switchOp: Int?): Autostart {
        val a = AppOpsManager.MODE_ALLOWED
        val i = AppOpsManager.MODE_IGNORED
        return when {
            primary == a && switchOp == a -> Autostart.ENABLED
            primary == i && switchOp == i -> Autostart.DISABLED
            (primary == a && switchOp == i) || (primary == i && switchOp == a) -> Autostart.PARTIAL
            else -> Autostart.UNKNOWN
        }
    }

    private fun checkOp(context: Context, op: Int, uid: Int, pkg: String): Int? {
        val mgr = context.getSystemService(AppOpsManager::class.java) ?: return null
        return try {
            val m = AppOpsManager::class.java.getDeclaredMethod(
                "checkOpNoThrow",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                String::class.java,
            )
            m.isAccessible = true
            m.invoke(mgr, op, uid, pkg) as? Int
        } catch (_: Throwable) {
            null
        }
    }

    fun openAutostartManager(context: Context) {
        val ok = launch(context, Intent("miui.intent.action.OP_AUTO_START").addCategory(Intent.CATEGORY_DEFAULT)) ||
            launch(
                context,
                Intent().setComponent(
                    ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity",
                    ),
                ),
            ) ||
            launch(context, Intent(Settings.ACTION_APPLICATION_SETTINGS))
        if (!ok) Toast.makeText(context, R.string.hidden_open_fail, Toast.LENGTH_SHORT).show()
    }

    /** MIUI per-app permission editor (Autostart lives there), else app details. */
    fun openAppPermissions(context: Context, pkg: String) {
        for (cls in listOf(
            "com.miui.permcenter.permissions.PermissionsEditorActivity",
            "com.miui.permcenter.permissions.AppPermissionsEditorActivity",
        )) {
            val i = Intent().setComponent(ComponentName("com.miui.securitycenter", cls))
                .putExtra("extra_pkgname", pkg)
                .putExtra("package_name", pkg)
            if (launch(context, i)) return
        }
        if (!launch(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg")))) {
            Toast.makeText(context, R.string.hidden_open_fail, Toast.LENGTH_SHORT).show()
        }
    }

    /** GMS GCM/GTalk diagnostics; class name varies across Play services builds. */
    fun openDiagnostics(context: Context) {
        val known = listOf(
            "com.google.android.gms.gcm.GcmDiagnostics",
            "com.google.android.gms.gtalkservice.diagnostics.GTalkServiceDiagnostics",
        )
        val discovered = try {
            context.packageManager.getPackageInfo(FcmGuard.REQUIRED, PackageManager.GET_ACTIVITIES)
                .activities.orEmpty().map { it.name }.filter {
                    val l = it.lowercase()
                    ("gcm" in l || "fcm" in l || "gtalk" in l) && "diagnostic" in l
                }
        } catch (_: Throwable) {
            emptyList()
        }
        for (cls in (known + discovered).distinct()) {
            if (launch(context, Intent().setClassName(FcmGuard.REQUIRED, cls))) return
        }
        Toast.makeText(context, R.string.fcm_diag_unavailable, Toast.LENGTH_SHORT).show()
    }

    fun openWriteSettings(context: Context) {
        launch(context, Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}")))
    }

    fun openNotificationSettings(context: Context) {
        launch(
            context,
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
        )
    }

    private fun launch(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: Throwable) {
        false
    }
}
