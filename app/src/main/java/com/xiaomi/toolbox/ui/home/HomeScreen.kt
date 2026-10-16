package com.xiaomi.toolbox.ui.home

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xiaomi.toolbox.R
import com.xiaomi.toolbox.data.FcmGuard
import com.xiaomi.toolbox.data.Keys
import com.xiaomi.toolbox.data.Power
import com.xiaomi.toolbox.data.SettingsStore
import java.io.File

@Composable
fun HomeScreen(onOpenFcm: () -> Unit) {
    val ctx = LocalContext.current
    val store = remember { SettingsStore(ctx) }
    var powerOpen by remember { mutableStateOf(false) }
    val stats = remember { collectStats(ctx, store) }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
        Text("${Build.MANUFACTURER} ${Build.MODEL}", style = MaterialTheme.typography.bodyMedium)
        Text("Android ${Build.VERSION.RELEASE}  ·  ${stats.version}", style = MaterialTheme.typography.bodySmall)
        StatRow("CPU", stats.cpu)
        StatRow(stringResource(R.string.battery), stats.battery)
        StatRow(stringResource(R.string.ram), stats.ram)
        StatRow(stringResource(R.string.storage), stats.storage)
        FcmCard(store, onOpenFcm)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { powerOpen = true }, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.power))
            }
        }
        if (!store.canWriteGlobal()) {
            Card(Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.write_global_fail),
                    Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
    if (powerOpen) {
        AlertDialog(
            onDismissRequest = { powerOpen = false },
            title = { Text(stringResource(R.string.power_select)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { Power.reboot(ctx, null) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.reboot))
                    }
                    OutlinedButton(onClick = { Power.reboot(ctx, "recovery") }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.recovery))
                    }
                    OutlinedButton(onClick = { Power.reboot(ctx, "bootloader") }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.fastboot))
                    }
                    OutlinedButton(onClick = { Power.reboot(ctx, "shutdown") }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.power_off))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { powerOpen = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun FcmCard(store: SettingsStore, onOpen: () -> Unit) {
    val status = remember { FcmGuard.status(store) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.fcm_title), style = MaterialTheme.typography.labelMedium)
            Text(
                stringResource(
                    if (status.present) R.string.fcm_protected else R.string.fcm_missing,
                ),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                stringResource(R.string.fcm_apps_in_list, status.count),
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedButton(onClick = onOpen) { Text(stringResource(R.string.fcm_open)) }
        }
    }
}

private data class Stats(val cpu: String, val battery: String, val ram: String, val storage: String, val version: String)

private fun collectStats(ctx: Context, store: SettingsStore): Stats {
    val cpu = try {
        File("/proc/cpuinfo").readLines().firstOrNull { it.startsWith("Hardware") || it.contains("model name", true) }
            ?.substringAfter(":")?.trim()
            ?: store.sysProp("ro.soc.model").ifBlank { Build.HARDWARE }
    } catch (_: Throwable) {
        Build.HARDWARE
    }
    val bat = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val pct = if (bat != null) {
        val l = bat.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val s = bat.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        if (s > 0) (l * 100 / s) else -1
    } else -1
    val temp = bat?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)?.div(10f) ?: 0f
    val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val mi = ActivityManager.MemoryInfo()
    am.getMemoryInfo(mi)
    val ramUsed = (mi.totalMem - mi.availMem) / 1e9
    val ramTot = mi.totalMem / 1e9
    val st = StatFs(Environment.getDataDirectory().path)
    val used = (st.totalBytes - st.availableBytes) / 1e9
    val tot = st.totalBytes / 1e9
    val ver = store.sysProp(Keys.PROP_VERSION).ifBlank { "—" }
    return Stats(
        cpu = cpu,
        battery = if (pct >= 0) "$pct% · ${"%.1f".format(temp)}°C" else "—",
        ram = "${"%.1f".format(ramUsed)} / ${"%.1f".format(ramTot)} GB",
        storage = "${"%.1f".format(used)} / ${"%.0f".format(tot)} GB",
        version = ver,
    )
}
