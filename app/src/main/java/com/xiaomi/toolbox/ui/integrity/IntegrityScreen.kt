package com.xiaomi.toolbox.ui.integrity

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xiaomi.toolbox.R
import com.xiaomi.toolbox.data.Catalog
import com.xiaomi.toolbox.data.Formats
import com.xiaomi.toolbox.data.Keys
import com.xiaomi.toolbox.data.Revocation
import com.xiaomi.toolbox.data.SettingsStore
import com.xiaomi.toolbox.data.TargetFlag
import com.xiaomi.toolbox.ui.BackBar
import com.xiaomi.toolbox.ui.BoolRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun IntegrityScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val store = remember { SettingsStore(ctx) }
    val catalog = remember { Catalog(ctx) }
    val scope = rememberCoroutineScope()
    var tick by remember { mutableStateOf(0) }
    fun refresh() { tick++ }
    tick
    val enabled = store.getGlobalBool(Keys.KEYBOX_ENABLED, true)
    val applyAll = store.getGlobalBool(Keys.KEYBOX_APPLY_ALL, false)
    val kbAuto = store.getGlobalBool(Keys.KEYBOX_AUTO_UPDATE, true)
    val pifAuto = store.getGlobalBool(Keys.PIF_AUTO_UPDATE, true)
    var mode by remember { mutableStateOf(store.getGlobal(Keys.KEYBOX_APPLY_ALL_MODE, "auto")) }
    val pif = Formats.parsePif(store.getGlobal(Keys.PIF_JSON))
    val targets = Formats.parseTargetTxt(store.getGlobal(Keys.TARGET_TXT))
    val xml = store.getGlobal(Keys.KEYBOX_XML)
    val xmlImport = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val body = readUri(ctx, uri) ?: return@rememberLauncherForActivityResult
        if (Revocation.parseSerials(body).isEmpty()) {
            Toast.makeText(ctx, R.string.import_fail, Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        store.putGlobal(Keys.KEYBOX_XML, body)
        catalog.markKeyboxImported()   // user-owned keybox: catalog must never overwrite
        refresh()
    }
    val xmlExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/xml")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        writeUri(ctx, uri, store.getGlobal(Keys.KEYBOX_XML))
    }
    val jsonImport = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val body = readUri(ctx, uri) ?: return@rememberLauncherForActivityResult
        if (!catalog.applyPif(store, body)) {
            Toast.makeText(ctx, R.string.import_fail, Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        refresh()
    }
    val jsonExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        writeUri(ctx, uri, store.getGlobal(Keys.PIF_JSON))
    }
    val txtImport = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val body = readUri(ctx, uri) ?: return@rememberLauncherForActivityResult
        val map = Formats.parseTargetTxt(body)
        // blank clears the list; anything else that parses to nothing is garbage
        if (body.isNotBlank() && map.isEmpty()) {
            Toast.makeText(ctx, R.string.import_fail, Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        store.putGlobal(Keys.TARGET_TXT, Formats.serializeTargetTxt(map))
        refresh()
    }
    val txtExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        writeUri(ctx, uri, store.getGlobal(Keys.TARGET_TXT))
    }

    Column(Modifier.fillMaxSize()) {
        BackBar(stringResource(R.string.spoof_subtab_pif), onBack)
        LazyColumn(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                BoolRow(stringResource(R.string.keybox_enabled), stringResource(R.string.keybox_enabled_desc), enabled) {
                    store.putGlobalBool(Keys.KEYBOX_ENABLED, it); refresh()
                }
                BoolRow(stringResource(R.string.target_apply_for_all_apps), null, applyAll) {
                    store.putGlobalBool(Keys.KEYBOX_APPLY_ALL, it); refresh()
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("auto", "leaf", "gen").forEach { m ->
                        val label = when (m) {
                            "leaf" -> stringResource(R.string.flag_leaf)
                            "gen" -> stringResource(R.string.flag_gen)
                            else -> stringResource(R.string.flag_auto)
                        }
                        if (mode == m) Button(onClick = {}) { Text(label) }
                        else OutlinedButton(onClick = {
                            mode = m
                            store.putGlobal(Keys.KEYBOX_APPLY_ALL_MODE, m)
                            refresh()
                        }) { Text(label) }
                    }
                }
                BoolRow(stringResource(R.string.auto_update_keybox), null, kbAuto) {
                    store.putGlobalBool(Keys.KEYBOX_AUTO_UPDATE, it); refresh()
                }
                BoolRow(stringResource(R.string.auto_update_pif), null, pifAuto) {
                    store.putGlobalBool(Keys.PIF_AUTO_UPDATE, it); refresh()
                }
                Button(onClick = {
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) {
                            val a = catalog.refreshKeybox(store)
                            val b = catalog.refreshPif(store)
                            catalog.refreshDeviceModels()
                            if (a && store.getGlobalBool(Keys.KEYBOX_ENABLED, true)) {
                                Revocation.check(ctx, store.getGlobal(Keys.KEYBOX_XML))
                            }
                            catalog.refreshDate()
                            a || b
                        }
                        Toast.makeText(
                            ctx,
                            ctx.getString(if (ok) R.string.refresh_ok else R.string.refresh_fail),
                            Toast.LENGTH_SHORT,
                        ).show()
                        refresh()
                    }
                }) { Text(stringResource(R.string.spoof_refresh_row)) }
                catalog.catalogDate()?.let {
                    Text(
                        stringResource(R.string.catalog_updated, it),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.keybox_title), style = MaterialTheme.typography.titleMedium)
                        Text(if (xml.isBlank()) "—" else "${xml.length} chars", style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { xmlImport.launch(arrayOf("text/xml", "*/*")) }) {
                                Text(stringResource(R.string.game_import))
                            }
                            OutlinedButton(onClick = { xmlExport.launch("keybox.xml") }) {
                                Text(stringResource(R.string.game_export))
                            }
                        }
                        RevocationRow(xml, tick) { refresh() }
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.pif_title), style = MaterialTheme.typography.titleMedium)
                        Text(
                            listOf("MODEL", "DEVICE", "SECURITY_PATCH")
                                .map { "${it}: ${pif[it] ?: "—"}" }
                                .joinToString("\n"),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { jsonImport.launch(arrayOf("application/json", "*/*")) }) {
                                Text(stringResource(R.string.game_import))
                            }
                            OutlinedButton(onClick = { jsonExport.launch("pif.json") }) {
                                Text(stringResource(R.string.game_export))
                            }
                        }
                    }
                }
            }
            if (!applyAll) {
                item {
                    Text(stringResource(R.string.target_list_title), style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { txtImport.launch(arrayOf("text/plain", "*/*")) }) {
                            Text(stringResource(R.string.target_import))
                        }
                        OutlinedButton(onClick = { txtExport.launch("target.txt") }) {
                            Text(stringResource(R.string.target_export))
                        }
                    }
                }
                items(targets.entries.toList()) { (pkg, flags) ->
                    TargetRow(pkg, flags) { new ->
                        val m = targets.toMutableMap()
                        m[pkg] = new
                        store.putGlobal(Keys.TARGET_TXT, Formats.serializeTargetTxt(m), silent = true)
                        refresh()
                    }
                }
            }
        }
    }
}

@Composable
private fun RevocationRow(keyboxXml: String, version: Int, onChanged: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val cached = remember(version) { Revocation.cached(ctx) }
    var state by remember(version) { mutableStateOf(cached?.state) }
    var checkedAt by remember(version) { mutableStateOf(cached?.checkedAt ?: 0L) }
    var affected by remember(version) { mutableStateOf(cached?.affectedSerials.orEmpty()) }
    var running by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.revocation_title), style = MaterialTheme.typography.titleSmall)
        val stateRes = when (state) {
            null -> R.string.revocation_none
            Revocation.STATE_EMPTY -> R.string.revocation_state_EMPTY
            Revocation.STATE_VALID -> R.string.revocation_state_VALID
            Revocation.STATE_SUSPENDED -> R.string.revocation_state_SUSPENDED
            Revocation.STATE_REVOKED -> R.string.revocation_state_REVOKED
            else -> R.string.revocation_state_OFFLINE
        }
        Text(
            stringResource(stateRes) + if (checkedAt > 0L) {
                " · " + stringResource(
                    R.string.revocation_checked_at,
                    java.text.DateFormat.getDateTimeInstance().format(java.util.Date(checkedAt)),
                )
            } else "",
            style = MaterialTheme.typography.bodySmall,
        )
        if (state == Revocation.STATE_REVOKED || state == Revocation.STATE_SUSPENDED) {
            if (affected.isNotEmpty()) {
                Text(
                    stringResource(R.string.revocation_affected, affected.joinToString(", ")),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        OutlinedButton(enabled = !running, onClick = {
            running = true
            scope.launch {
                val r = withContext(Dispatchers.IO) { Revocation.check(ctx, keyboxXml) }
                state = r.state
                checkedAt = r.checkedAt
                affected = r.affectedSerials
                running = false
                onChanged()
            }
        }) { Text(stringResource(R.string.revocation_check)) }
    }
}

@Composable
private fun TargetRow(pkg: String, flags: Set<TargetFlag>, onChange: (Set<TargetFlag>) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(pkg, style = MaterialTheme.typography.bodyMedium)
                Text(
                    flags.joinToString(" ") { it.name },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            TextButton(onClick = { menu = true }) { Text(stringResource(R.string.target_mode)) }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Auto") }, onClick = { onChange(emptySet()); menu = false })
                DropdownMenuItem(text = { Text("Leaf") }, onClick = { onChange(setOf(TargetFlag.LEAF)); menu = false })
                DropdownMenuItem(text = { Text("Gen") }, onClick = { onChange(setOf(TargetFlag.GEN)); menu = false })
                DropdownMenuItem(text = { Text("Leaf+PIF") }, onClick = {
                    onChange(setOf(TargetFlag.LEAF, TargetFlag.PIF)); menu = false
                })
                DropdownMenuItem(text = { Text("Gen+PIF") }, onClick = {
                    onChange(setOf(TargetFlag.GEN, TargetFlag.PIF)); menu = false
                })
            }
        }
    }
}

fun readUri(ctx: android.content.Context, uri: Uri): String? =
    try {
        ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
    } catch (_: Throwable) {
        null
    }

fun writeUri(ctx: android.content.Context, uri: Uri, body: String) {
    try {
        ctx.contentResolver.openOutputStream(uri)?.use { it.write(body.toByteArray()) }
    } catch (_: Throwable) {
    }
}
