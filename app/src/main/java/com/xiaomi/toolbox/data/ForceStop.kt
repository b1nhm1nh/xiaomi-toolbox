package com.xiaomi.toolbox.data

import android.app.ActivityManager
import android.content.Context
import android.os.Process
import android.widget.Toast
import com.xiaomi.toolbox.R

object ForceStop {
    fun stop(context: Context, pkg: String): Boolean {
        if (pkg.isBlank() || pkg == context.packageName) return false
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        try {
            ActivityManager::class.java.getMethod("forceStopPackage", String::class.java)
                .invoke(am, pkg)
            Toast.makeText(
                context,
                context.getString(R.string.app_force_stopped, pkg),
                Toast.LENGTH_SHORT,
            ).show()
            return true
        } catch (_: Throwable) {
        }
        return try {
            val svc = try {
                Class.forName("android.app.ActivityManager")
                    .getMethod("getService").invoke(null)
            } catch (_: Throwable) {
                Class.forName("android.app.ActivityManagerNative")
                    .getMethod("getDefault").invoke(null)
            } ?: return fail(context, pkg)
            val userId = Process.myUid() / 100000
            val methods = svc.javaClass.methods
            val m = methods.firstOrNull {
                it.name == "forceStopPackage" && it.parameterTypes.size in 1..2
            } ?: return fail(context, pkg)
            if (m.parameterTypes.size == 1) m.invoke(svc, pkg)
            else m.invoke(svc, pkg, userId)
            Toast.makeText(
                context,
                context.getString(R.string.app_force_stopped, pkg),
                Toast.LENGTH_SHORT,
            ).show()
            true
        } catch (_: Throwable) {
            fail(context, pkg)
        }
    }

    private fun fail(context: Context, pkg: String): Boolean {
        Toast.makeText(
            context,
            context.getString(R.string.app_force_stop_fail, pkg),
            Toast.LENGTH_SHORT,
        ).show()
        return false
    }
}
