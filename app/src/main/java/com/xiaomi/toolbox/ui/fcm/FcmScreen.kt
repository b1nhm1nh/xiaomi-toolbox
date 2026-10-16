package com.xiaomi.toolbox.ui.fcm

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.xiaomi.toolbox.R
import com.xiaomi.toolbox.data.FcmApps
import com.xiaomi.toolbox.data.FcmGuard
import com.xiaomi.toolbox.data.SettingsStore
import com.xiaomi.toolbox.fcm.FcmGuardService
import com.xiaomi.toolbox.ui.BackBar
import com.xiaomi.toolbox.ui.BoolRow
import com.xiaomi.toolbox.ui.apps.MultiAppPickerDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class ScanRow(val entry: FcmApps.Entry, val autostart: FcmApps.Autostart)

@Composable
fun FcmScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val store = remember { SettingsStore(ctx) }
    val scope = rememberCoroutineScope()
    // bumped on resume so status/autostart refresh after returning from system pages
    var tick by remember { mutableStateOf(0) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) tick++ }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    val canWrite = remember(tick) { store.canWriteSystem() }
    val status = remember(tick) { FcmGuard.status(store) }
    val extras = remember(tick) { FcmGuard.extras(ctx) }
    val canNotify = remember(tick) { FcmGuardService.canNotify(ctx) }
    var auto by remember { mutableStateOf(FcmGuard.isAutoEnabled(ctx)) }
    var persistent by remember { mutableStateOf(FcmGuard.usePersistentNotification(ctx)) }
    var pickOpen by remember { mutableStateOf(false) }
    var scanned by remember { mutableStateOf<List<ScanRow>?>(null) }
    var scanning by remember { mutableStateOf(false) }

    fun rescan() {
        scanning = true
        scope.launch(Dispatchers.IO) {
            val rows = FcmApps.scan(ctx).map { ScanRow(it, FcmApps.autostart(ctx, it.pkg)) }
            withContext(Dispatchers.Main) {
                scanned = rows
                scanning = false
            }
        }
    }
    // re-read autostart after coming back from the HyperOS autostart pages
    LaunchedEffect(tick) { if (scanned != null && !scanning) rescan() }

    Column(Modifier.fillMaxSize()) {
        BackBar(stringResource(R.string.fcm_guard_title), onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!canWrite) {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.fcm_perm_missing), style = MaterialTheme.typography.bodyMedium)
                        Button(onClick = { FcmApps.openWriteSettings(ctx) }) {
                            Text(stringResource(R.string.fcm_grant))
                        }
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(if (status.present) R.string.fcm_protected else R.string.fcm_missing),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(stringResource(R.string.fcm_apps_in_list, status.count), style = MaterialTheme.typography.bodySmall)
                    if (extras.isNotEmpty()) {
                        Text(
                            stringResource(R.string.fcm_extra_apps, extras.sorted().joinToString(", ")),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            scope.launch(Dispatchers.IO) {
                                val repaired = FcmGuard.repair(ctx, store)
                                // manual repair always kicks, like FCMGuard "Wake FCM"
                                FcmGuard.kick(ctx)
                                val now = FcmGuard.status(store)
                                withContext(Dispatchers.Main) {
                                    val msg = when {
                                        !now.present -> R.string.fcm_repair_failed
                                        repaired -> R.string.fcm_repaired
                                        else -> R.string.fcm_already_ok
                                    }
                                    Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
                                    tick++
                                }
                            }
                        }) { Text(stringResource(R.string.fcm_repair)) }
                        OutlinedButton(onClick = { pickOpen = true }) {
                            Text(stringResource(R.string.fcm_protect_title))
                        }
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    BoolRow(
                        stringResource(R.string.fcm_auto),
                        stringResource(R.string.fcm_auto_sub),
                        auto,
                    ) { on ->
                        auto = on
                        FcmGuard.setAutoEnabled(ctx, on)
                        if (on) {
                            if (persistent && !FcmGuardService.canNotify(ctx)) FcmApps.openNotificationSettings(ctx)
                            FcmGuardService.startIfEnabled(ctx)
                        } else {
                            FcmGuardService.stop(ctx)
                        }
                    }
                    BoolRow(
                        stringResource(R.string.fcm_persistent),
                        stringResource(R.string.fcm_persistent_sub),
                        persistent,
                    ) { on ->
                        persistent = on
                        FcmGuard.setPersistentNotification(ctx, on)
                        if (auto) {
                            // FGS ↔ plain service needs a fresh start
                            FcmGuardService.stop(ctx)
                            if (on && !FcmGuardService.canNotify(ctx)) FcmApps.openNotificationSettings(ctx)
                            FcmGuardService.startIfEnabled(ctx)
                        }
                    }
                    if (auto && persistent && !canNotify) {
                        TextButton(onClick = { FcmApps.openNotificationSettings(ctx) }) {
                            Text(stringResource(R.string.fcm_notif_blocked))
                        }
                    }
                    Text(
                        stringResource(R.string.fcm_hyperos_hint),
                        Modifier.padding(vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { FcmApps.openAppPermissions(ctx, ctx.packageName) }) {
                            Text(stringResource(R.string.fcm_self_autostart))
                        }
                        OutlinedButton(onClick = { FcmApps.openDiagnostics(ctx) }) {
                            Text(stringResource(R.string.fcm_diag))
                        }
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.fcm_apps_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.fcm_apps_summary), style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { rescan() }, enabled = !scanning) {
                            Text(stringResource(if (scanning) R.string.fcm_scanning else R.string.fcm_scan))
                        }
                        OutlinedButton(onClick = { FcmApps.openAutostartManager(ctx) }) {
                            Text(stringResource(R.string.fcm_configure_all))
                        }
                    }
                    scanned?.let { rows -> ScanResults(rows, extras) { pkg -> FcmApps.openAppPermissions(ctx, pkg) } }
                    Text(stringResource(R.string.fcm_detection_note), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }

    if (pickOpen) {
        MultiAppPickerDialog(
            initial = extras,
            exclude = setOf(FcmGuard.REQUIRED, FcmGuard.GSF),
            onConfirm = { picked ->
                pickOpen = false
                scope.launch(Dispatchers.IO) {
                    FcmGuard.setExtras(ctx, picked)
                    FcmGuard.repairAndKick(ctx, store)
                    withContext(Dispatchers.Main) { tick++ }
                }
            },
            onDismiss = { pickOpen = false },
        )
    }
}

@Composable
private fun ScanResults(rows: List<ScanRow>, extras: Set<String>, onOpen: (String) -> Unit) {
    val enabled = rows.count { it.autostart == FcmApps.Autostart.ENABLED }
    val unknown = rows.count { it.autostart == FcmApps.Autostart.UNKNOWN }
    val attention = rows.size - enabled - unknown
    Text(
        if (rows.isNotEmpty() && unknown == rows.size) {
            stringResource(R.string.fcm_autostart_unavailable, rows.size)
        } else {
            stringResource(R.string.fcm_apps_status, rows.size, enabled, attention, unknown)
        },
        style = MaterialTheme.typography.bodyMedium,
    )
    for (r in rows) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(r.entry.label, style = MaterialTheme.typography.bodyMedium)
                Text(
                    r.entry.pkg + if (r.entry.pkg in extras) " · " + stringResource(R.string.fcm_in_list) else "",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            val (label, color) = when (r.autostart) {
                FcmApps.Autostart.ENABLED -> R.string.fcm_as_enabled to MaterialTheme.colorScheme.primary
                FcmApps.Autostart.PARTIAL -> R.string.fcm_as_partial to MaterialTheme.colorScheme.tertiary
                FcmApps.Autostart.DISABLED -> R.string.fcm_as_disabled to MaterialTheme.colorScheme.error
                FcmApps.Autostart.UNKNOWN -> R.string.fcm_as_unknown to MaterialTheme.colorScheme.outline
            }
            TextButton(onClick = { onOpen(r.entry.pkg) }) {
                Text(stringResource(label), color = color)
            }
        }
    }
}
