package com.xiaomi.toolbox.ui.framework

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.xiaomi.toolbox.ui.BackBar
import com.xiaomi.toolbox.ui.BoolRow

@Composable
fun FrameworkScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val store = remember { SettingsStore(ctx) }
    var tick by remember { mutableStateOf(0) }
    fun r() { tick++ }
    tick
    Column(Modifier.fillMaxSize()) {
        BackBar(stringResource(R.string.spoof_subtab_framework), onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
            BoolRow(
                stringResource(R.string.secure_flag),
                stringResource(R.string.secure_flag_summary) +
                    if (store.getGlobal(Keys.SECURE_FLAG_WORK).trim() == "1") "  ·  live" else "",
                store.getGlobalBool(Keys.SECURE_FLAG, true),
            ) { store.putGlobalBool(Keys.SECURE_FLAG, it); r() }
            BoolRow(
                stringResource(R.string.spoof_gameprops),
                stringResource(R.string.spoof_gameprops_summary),
                store.getGlobalBool(Keys.SPOOF_GAMEPROPS, true),
            ) { store.putGlobalBool(Keys.SPOOF_GAMEPROPS, it); r() }
            BoolRow(
                stringResource(R.string.spoof_gms),
                stringResource(R.string.spoof_gms_summary),
                store.getGlobalBool(Keys.SPOOF_GMS, false),
            ) { store.putGlobalBool(Keys.SPOOF_GMS, it); r() }
            BoolRow(
                stringResource(R.string.spoof_vending),
                stringResource(R.string.spoof_vending_summary),
                store.getGlobalBool(Keys.SPOOF_VENDING, false),
            ) { store.putGlobalBool(Keys.SPOOF_VENDING, it); r() }
            BoolRow(
                stringResource(R.string.spoof_photos),
                stringResource(R.string.spoof_photos_summary),
                store.getGlobalBool(Keys.SPOOF_PHOTOS, true),
            ) {
                store.putGlobalBool(Keys.SPOOF_PHOTOS, it)
                com.xiaomi.toolbox.data.ForceStop.stop(ctx, "com.google.android.apps.photos")
                r()
            }
            BoolRow(
                stringResource(R.string.spoof_tft),
                stringResource(R.string.spoof_tft_summary),
                store.getGlobalBool(Keys.SPOOF_TFT, false),
            ) { store.putGlobalBool(Keys.SPOOF_TFT, it); r() }
        }
    }
}
