package com.xiaomi.toolbox.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xiaomi.toolbox.BuildConfig
import com.xiaomi.toolbox.R
import com.xiaomi.toolbox.data.Keys
import com.xiaomi.toolbox.overlay.FpsOverlayService
import com.xiaomi.toolbox.ui.BoolRow

@Composable
fun SettingsScreen() {
    val ctx = LocalContext.current
    val fpsPrefs = remember { ctx.getSharedPreferences(Keys.PREF_FPS, android.content.Context.MODE_PRIVATE) }
    var fps by remember { mutableStateOf(fpsPrefs.getBoolean("enabled", false)) }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(stringResource(R.string.tab_settings), style = MaterialTheme.typography.headlineMedium)
        BoolRow(
            stringResource(R.string.fps_overlay_title),
            stringResource(R.string.fps_overlay_summary),
            fps,
        ) { on ->
            if (on && !Settings.canDrawOverlays(ctx)) {
                ctx.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${ctx.packageName}"),
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
                return@BoolRow
            }
            fpsPrefs.edit().putBoolean("enabled", on).apply()
            fps = on
            if (on) {
                ctx.startForegroundService(Intent(ctx, FpsOverlayService::class.java))
            } else {
                ctx.stopService(Intent(ctx, FpsOverlayService::class.java))
            }
        }
        Text("Xiaomi Toolbox ${BuildConfig.VERSION_NAME}", Modifier.padding(top = 24.dp), style = MaterialTheme.typography.bodyMedium)
        Text("https://github.com/b1nhm1nh/xiaomi-toolbox", style = MaterialTheme.typography.bodySmall)
    }
}
