package com.xiaomi.toolbox.ui.apps

import android.content.pm.ApplicationInfo
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.xiaomi.toolbox.data.ForceStop
import com.xiaomi.toolbox.data.Formats
import com.xiaomi.toolbox.data.Keys
import com.xiaomi.toolbox.data.SettingsStore
import com.xiaomi.toolbox.ui.BackBar
import com.xiaomi.toolbox.ui.BoolRow
import com.xiaomi.toolbox.ui.integrity.readUri
import com.xiaomi.toolbox.ui.integrity.writeUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AppsSpoofScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val store = remember { SettingsStore(ctx) }
    val catalog = remember { Catalog(ctx) }
    val scope = rememberCoroutineScope()
    var tab by remember { mutableIntStateOf(0) }
    var tick by remember { mutableStateOf(0) }
    fun r() { tick++ }
    tick
    val games = Formats.parseGameProps(store.getGlobal(Keys.GAMEPROPS_JSON)).toMutableMap()
    val hide = Formats.parseHideDev(store.getGlobal(Keys.HIDE_DEVLIST)).toMutableMap()
    val master = store.getGlobalBool(Keys.SPOOF_GAMEPROPS, true)
    var pickPkg by remember { mutableStateOf<((String) -> Unit)?>(null) }
    var editPkg by remember { mutableStateOf<String?>(null) }

    val jsonImport = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val body = readUri(ctx, uri) ?: return@rememberLauncherForActivityResult
        val isGameProps = try {
            org.json.JSONObject(body).has("games")
        } catch (_: Throwable) {
            false
        }
        if (!isGameProps) {
            Toast.makeText(ctx, R.string.import_fail, Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        // re-serialize through the parser so junk fields can't reach the ROM
        store.putGlobal(Keys.GAMEPROPS_JSON, Formats.serializeGameProps(Formats.parseGameProps(body)))
        r()
    }
    val jsonExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        writeUri(ctx, uri, store.getGlobal(Keys.GAMEPROPS_JSON))
    }

    Column(Modifier.fillMaxSize()) {
        BackBar(stringResource(R.string.spoof_subtab_apps), onBack)
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.game_json_title)) })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.hide_dev_list)) })
        }
        if (tab == 0) {
            if (!master) {
                Text(stringResource(R.string.feature_off_gameprops), Modifier.padding(16.dp))
            }
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { pickPkg = { pkg ->
                    if (!games.containsKey(pkg)) {
                        games[pkg] = Formats.GameProp("", "", "", "", "", "", "")
                        store.putGlobal(Keys.GAMEPROPS_JSON, Formats.serializeGameProps(games), silent = true)
                        r()
                    }
                    editPkg = pkg
                } }) { Text(stringResource(R.string.game_add_app)) }
                OutlinedButton(onClick = {
                    scope.launch {
                        val cat = withContext(Dispatchers.IO) { catalog.refreshAppProps() }
                        val pm = ctx.packageManager
                        val installed = pm.getInstalledApplications(0).map { it.packageName }.toSet()
                        // re-read from the store: the captured `games` snapshot is stale
                        // once the user edited anything while the catalog fetch was running
                        val cur = Formats.parseGameProps(store.getGlobal(Keys.GAMEPROPS_JSON)).toMutableMap()
                        var n = 0
                        for ((pkg, prop) in cat) {
                            if (pkg in installed && pkg !in cur) {
                                cur[pkg] = prop
                                n++
                            }
                        }
                        if (n > 0) store.putGlobal(Keys.GAMEPROPS_JSON, Formats.serializeGameProps(cur), silent = true)
                        Toast.makeText(
                            ctx,
                            if (n == 0) ctx.getString(R.string.game_auto_import_no_matches)
                            else ctx.getString(R.string.game_auto_import_ok, n),
                            Toast.LENGTH_SHORT,
                        ).show()
                        r()
                    }
                }) { Text(stringResource(R.string.game_auto_import)) }
                OutlinedButton(onClick = { jsonImport.launch(arrayOf("application/json", "*/*")) }) {
                    Text(stringResource(R.string.game_import))
                }
                OutlinedButton(onClick = { jsonExport.launch("gameprops.json") }) {
                    Text(stringResource(R.string.game_export))
                }
            }
            LazyColumn(Modifier.padding(horizontal = 16.dp)) {
                items(games.entries.toList()) { (pkg, prop) ->
                    Card(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { editPkg = pkg },
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(pkg, style = MaterialTheme.typography.titleSmall)
                            Text(
                                prop.name.ifBlank { "${prop.brand} ${prop.model}" }.ifBlank { "—" },
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        } else {
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { pickPkg = { pkg ->
                    hide[pkg] = Formats.HideDev(hideDev = true, hideApp = false)
                    store.putGlobal(Keys.HIDE_DEVLIST, Formats.serializeHideDev(hide), silent = true)
                    r()
                } }) { Text(stringResource(R.string.add_app)) }
            }
            LazyColumn(Modifier.padding(horizontal = 16.dp)) {
                items(hide.entries.toList()) { (pkg, e) ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(pkg, style = MaterialTheme.typography.titleSmall)
                            BoolRow("hidedev", null, e.hideDev) { v ->
                                hide[pkg] = e.copy(hideDev = v)
                                store.putGlobal(Keys.HIDE_DEVLIST, Formats.serializeHideDev(hide), silent = true)
                                r()
                            }
                            BoolRow("hideapp", null, e.hideApp) { v ->
                                hide[pkg] = e.copy(hideApp = v)
                                store.putGlobal(Keys.HIDE_DEVLIST, Formats.serializeHideDev(hide), silent = true)
                                r()
                            }
                            TextButton(onClick = {
                                hide.remove(pkg)
                                store.putGlobal(Keys.HIDE_DEVLIST, Formats.serializeHideDev(hide), silent = true)
                                r()
                            }) { Text(stringResource(R.string.delete)) }
                        }
                    }
                }
            }
        }
    }

    val picker = pickPkg
    if (picker != null) {
        AppPickerDialog(
            onPick = { picker(it); pickPkg = null },
            onDismiss = { pickPkg = null },
        )
    }
    val ep = editPkg
    if (ep != null) {
        val cur = games[ep] ?: Formats.GameProp("", "", "", "", "", "")
        GameEditDialog(
            pkg = ep,
            initial = cur,
            onSave = { p ->
                games[ep] = p
                store.putGlobal(Keys.GAMEPROPS_JSON, Formats.serializeGameProps(games))
                ForceStop.stop(ctx, ep)
                editPkg = null
                r()
            },
            onDelete = {
                games.remove(ep)
                store.putGlobal(Keys.GAMEPROPS_JSON, Formats.serializeGameProps(games), silent = true)
                editPkg = null
                r()
            },
            onDismiss = { editPkg = null },
            catalog = catalog,
        )
    }
}

@Composable
private fun AppPickerDialog(onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var q by remember { mutableStateOf("") }
    val apps = remember {
        ctx.packageManager.getInstalledApplications(0)
            .sortedBy { it.loadLabel(ctx.packageManager).toString().lowercase() }
    }
    val filtered = apps.filter {
        val label = it.loadLabel(ctx.packageManager).toString()
        q.isBlank() || label.contains(q, true) || it.packageName.contains(q, true)
    }.take(80)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_app)) },
        text = {
            Column {
                OutlinedTextField(q, { q = it }, label = { Text(stringResource(R.string.search)) }, modifier = Modifier.fillMaxWidth())
                LazyColumn {
                    items(filtered) { ai: ApplicationInfo ->
                        Text(
                            "${ai.loadLabel(ctx.packageManager)}  (${ai.packageName})",
                            Modifier
                                .fillMaxWidth()
                                .clickable { onPick(ai.packageName) }
                                .padding(8.dp),
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun GameEditDialog(
    pkg: String,
    initial: Formats.GameProp,
    onSave: (Formats.GameProp) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    catalog: Catalog,
) {
    var brand by remember { mutableStateOf(initial.brand) }
    var device by remember { mutableStateOf(initial.device) }
    var manufacturer by remember { mutableStateOf(initial.manufacturer) }
    var model by remember { mutableStateOf(initial.model) }
    var fingerprint by remember { mutableStateOf(initial.fingerprint) }
    var product by remember { mutableStateOf(initial.product) }
    var name by remember { mutableStateOf(initial.name) }
    var models by remember { mutableStateOf<List<Formats.DeviceModel>>(emptyList()) }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(pkg) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedButton(onClick = {
                    scope.launch {
                        models = withContext(Dispatchers.IO) { catalog.refreshDeviceModels() }
                    }
                }) { Text(stringResource(R.string.select_device)) }
                if (models.isNotEmpty()) {
                    LazyColumn(Modifier.fillMaxWidth()) {
                        items(models.take(40)) { m ->
                            Text(
                                "${m.name} · ${m.model}",
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        brand = m.brand
                                        device = m.device
                                        manufacturer = m.manufacturer
                                        model = m.model
                                        fingerprint = m.fingerprint.orEmpty()
                                        product = m.product
                                        name = m.name
                                        models = emptyList()
                                    }
                                    .padding(6.dp),
                            )
                        }
                    }
                }
                Field("Brand", brand) { brand = it }
                Field("Device", device) { device = it }
                Field("Manufacturer", manufacturer) { manufacturer = it }
                Field("Model", model) { model = it }
                Field("Fingerprint", fingerprint) { fingerprint = it }
                Field("Product", product) { product = it }
                Field("Name", name) { name = it }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(Formats.GameProp(brand, device, manufacturer, model, fingerprint, product, name))
            }) { Text(stringResource(R.string.save_flags)) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text(stringResource(R.string.delete)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}

@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value, onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
}
