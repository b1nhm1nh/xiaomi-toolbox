package com.xiaomi.toolbox.ui.rom

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
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
import com.xiaomi.toolbox.R
import com.xiaomi.toolbox.data.Keys
import com.xiaomi.toolbox.data.SettingsStore
import com.xiaomi.toolbox.data.ThermalProfile
import com.xiaomi.toolbox.ui.BackBar
import com.xiaomi.toolbox.ui.BoolRow

@Composable
fun RomSettingsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val store = remember { SettingsStore(ctx) }
    var tick by remember { mutableStateOf(0) }
    fun r() { tick++ }
    tick
    val clockOn = store.getSystemInt(Keys.CLOCK_ENABLED, 0) != 0
    val style = store.getSystemInt(Keys.CLOCK_STYLE, 0)
    val size = store.getSystemInt(Keys.CLOCK_SIZE, 14).coerceIn(11, 17)
    val weight = store.getSystemInt(Keys.CLOCK_WEIGHT, 700)
    val pattern = store.getSystemString(Keys.CLOCK_PATTERN, Keys.CLOCK_PATTERN_DEFAULT)
    val color = store.getSystemString(Keys.CLOCK_COLOR, Keys.CLOCK_COLOR_DEFAULT)
    val thermal = ThermalProfile.fromValue(store.getSystemInt(Keys.THERMAL_CONFIG, 0), false)
    val fod = store.sysProp(Keys.PROP_FOD) == "1"
    var styleMenu by remember { mutableStateOf(false) }
    var patternMenu by remember { mutableStateOf(false) }
    var thermalMenu by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        BackBar(stringResource(R.string.spoof_subtab_zk), onBack)
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.zk_clock_section), style = MaterialTheme.typography.titleMedium)
            BoolRow(stringResource(R.string.zk_clock_master_title), stringResource(R.string.zk_clock_master_sum), clockOn) { on ->
                if (on) {
                    store.putSystemInt(Keys.CLOCK_ENABLED, 1, silent = true)
                    store.putSystemInt(Keys.CLOCK_STYLE, 0, silent = true)
                    store.putSystemString(Keys.CLOCK_PATTERN, Keys.CLOCK_PATTERN_DEFAULT, silent = true)
                    store.putSystemInt(Keys.CLOCK_SIZE, 14, silent = true)
                    store.putSystemString(Keys.CLOCK_COLOR, Keys.CLOCK_COLOR_DEFAULT, silent = true)
                    store.putSystemInt(Keys.CLOCK_WEIGHT, 700, silent = true)
                } else {
                    // keep CLOCK_ENABLED (0) itself: deleting it makes the ROM fall back
                    // to enabled and the clock would stay visible after "off"
                    store.putSystemInt(Keys.CLOCK_ENABLED, 0, silent = true)
                    store.putSystemString(Keys.CLOCK_STYLE, null, silent = true)
                    store.putSystemString(Keys.CLOCK_PATTERN, null, silent = true)
                    store.putSystemString(Keys.CLOCK_SIZE, null, silent = true)
                    store.putSystemString(Keys.CLOCK_COLOR, null, silent = true)
                    store.putSystemString(Keys.CLOCK_WEIGHT, null, silent = true)
                }
                r()
            }
            if (clockOn) {
                OutlinedButton(onClick = { styleMenu = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("${stringResource(R.string.zk_clock_style_label)}: ${Keys.CLOCK_STYLES.getOrElse(style) { "?" }}")
                }
                DropdownMenu(expanded = styleMenu, onDismissRequest = { styleMenu = false }) {
                    Keys.CLOCK_STYLES.forEachIndexed { i, n ->
                        DropdownMenuItem(text = { Text(n) }, onClick = {
                            store.putSystemInt(Keys.CLOCK_STYLE, i); styleMenu = false; r()
                        })
                    }
                }
                OutlinedButton(onClick = { patternMenu = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("${stringResource(R.string.zk_clock_pattern_label)}: $pattern")
                }
                DropdownMenu(expanded = patternMenu, onDismissRequest = { patternMenu = false }) {
                    Keys.CLOCK_PATTERNS.forEach { (p, label) ->
                        DropdownMenuItem(text = { Text("$label  ($p)") }, onClick = {
                            store.putSystemString(Keys.CLOCK_PATTERN, p); patternMenu = false; r()
                        })
                    }
                }
                Text("${stringResource(R.string.zk_clock_size_label)}: $size")
                var sizeDraft by remember(tick) { mutableStateOf(size.toFloat()) }
                Slider(
                    value = sizeDraft,
                    onValueChange = { sizeDraft = it },
                    onValueChangeFinished = {
                        store.putSystemInt(Keys.CLOCK_SIZE, sizeDraft.toInt(), silent = true)
                        r()
                    },
                    valueRange = 11f..17f,
                    steps = 5,
                )
                Text("${stringResource(R.string.zk_clock_color_label)}: #$color")
                Text("${stringResource(R.string.zk_clock_weight_label)}: $weight")
                var weightDraft by remember(tick) { mutableStateOf(weight.toFloat()) }
                Slider(
                    value = weightDraft,
                    onValueChange = { weightDraft = it },
                    onValueChangeFinished = {
                        val w = ((weightDraft / 100).toInt().coerceIn(1, 9)) * 100
                        store.putSystemInt(Keys.CLOCK_WEIGHT, w, silent = true)
                        r()
                    },
                    valueRange = 100f..900f,
                    steps = 7,
                )
            }

            Text(stringResource(R.string.zk_charging_info), style = MaterialTheme.typography.titleMedium)
            BoolRow(stringResource(R.string.sys_extra_charging_info), null, store.getSystemInt(Keys.EXTRA_CHARGING_INFO, 1) != 0) {
                store.putSystemBool(Keys.EXTRA_CHARGING_INFO, it); r()
            }
            BoolRow(stringResource(R.string.sys_show_temp), null, store.getSystemInt(Keys.SHOW_TEMP, 1) != 0) {
                store.putSystemBool(Keys.SHOW_TEMP, it); r()
            }
            BoolRow("Hide original charging text", null, store.getSystemInt(Keys.HIDE_ORIGINAL_CHARGING_TEXT, 1) != 0) {
                store.putSystemBool(Keys.HIDE_ORIGINAL_CHARGING_TEXT, it); r()
            }
            BoolRow(stringResource(R.string.sys_show_ampere), null, store.getSystemInt(Keys.SHOW_AMPERE, 1) != 0) {
                store.putSystemBool(Keys.SHOW_AMPERE, it); r()
            }
            BoolRow(stringResource(R.string.sys_show_voltage), null, store.getSystemInt(Keys.SHOW_VOLTAGE, 1) != 0) {
                store.putSystemBool(Keys.SHOW_VOLTAGE, it); r()
            }
            BoolRow(stringResource(R.string.sys_show_power), null, store.getSystemInt(Keys.SHOW_POWER, 0) != 0) {
                store.putSystemBool(Keys.SHOW_POWER, it); r()
            }
            val csize = store.getSystemInt(Keys.CHARGING_INFO_TEXT_SIZE, 14)
            Text("${stringResource(R.string.sys_charging_text_size)}: $csize")
            var csizeDraft by remember(tick) { mutableStateOf(csize.toFloat()) }
            Slider(
                value = csizeDraft,
                onValueChange = { csizeDraft = it },
                onValueChangeFinished = {
                    store.putSystemInt(Keys.CHARGING_INFO_TEXT_SIZE, csizeDraft.toInt(), silent = true)
                    r()
                },
                valueRange = 10f..20f,
                steps = 9,
            )

            Text(stringResource(R.string.zk_advanced_settings), style = MaterialTheme.typography.titleMedium)
            BoolRow(stringResource(R.string.sys_disable_privacy_chip), stringResource(R.string.sys_disable_privacy_chip_sum), store.getSystemInt(Keys.DISABLE_PRIVACY_CHIP, 0) != 0) {
                store.putSystemBool(Keys.DISABLE_PRIVACY_CHIP, it); r()
            }
            BoolRow(stringResource(R.string.sys_zk_max_fps), stringResource(R.string.sys_zk_max_fps_sum), store.getSystemInt(Keys.ZK_MAX_FPS, 0) != 0) {
                store.putSystemBool(Keys.ZK_MAX_FPS, it); r()
            }
            val ncount = store.getSystemInt(Keys.NOTIFY_COUNT, 1)
            Text("${stringResource(R.string.sys_notif_icons_count)}: $ncount")
            var ncountDraft by remember(tick) { mutableStateOf(ncount.toFloat()) }
            Slider(
                value = ncountDraft,
                onValueChange = { ncountDraft = it },
                onValueChangeFinished = {
                    store.putSystemInt(Keys.NOTIFY_COUNT, ncountDraft.toInt().coerceIn(1, 8), silent = true)
                    r()
                },
                valueRange = 1f..8f,
                steps = 6,
            )
            BoolRow(
                stringResource(R.string.sys_show_rotation_suggestions),
                stringResource(R.string.sys_show_rotation_suggestions_sum),
                store.getSecureInt(Keys.SHOW_ROTATION, 1) != 0,
            ) { store.putSecureInt(Keys.SHOW_ROTATION, if (it) 1 else 0); r() }

            Text(stringResource(R.string.thermal_profiles_writer), style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = { thermalMenu = true }, modifier = Modifier.fillMaxWidth()) {
                Text(thermal.label)
            }
            DropdownMenu(expanded = thermalMenu, onDismissRequest = { thermalMenu = false }) {
                ThermalProfile.entries.forEach { p ->
                    DropdownMenuItem(text = { Text(p.label) }, onClick = {
                        thermalMenu = false
                        val v = p.value(false)
                        val ok = store.putSystemInt(Keys.THERMAL_CONFIG, v, silent = true)
                        if (ok) {
                            try {
                                val i = Intent(Keys.THERMAL_ACTION)
                                    .putExtra(Keys.THERMAL_EXTRA, v.toString())
                                    .setPackage(Keys.SYSTEMUI)
                                    .addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                                ctx.sendBroadcast(i)
                            } catch (_: Throwable) {
                            }
                        }
                        r()
                    })
                }
            }
            if (fod) {
                Text(stringResource(R.string.quick_open_fod), style = MaterialTheme.typography.titleMedium)
                Text(store.getSystemString(Keys.QUICK_APPS, "—"), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
