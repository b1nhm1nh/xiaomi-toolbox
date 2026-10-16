package com.xiaomi.toolbox.ui.hub

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xiaomi.toolbox.R
import com.xiaomi.toolbox.data.RomCompat
import com.xiaomi.toolbox.data.SettingsStore
import com.xiaomi.toolbox.ui.nav.HubRoute

@Composable
fun HubScreen(onOpen: (HubRoute) -> Unit) {
    val ctx = LocalContext.current
    val store = remember { SettingsStore(ctx) }
    val rom = remember { RomCompat.detect(store) }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.tab_spoof), style = MaterialTheme.typography.headlineMedium)
        if (!rom.hooks) {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
            ) {
                Text(
                    stringResource(R.string.rom_hooks_missing_banner),
                    Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        HubCard(R.string.spoof_subtab_pif, R.string.spoof_hub_pif_sub, rom.hooks) { onOpen(HubRoute.PlayIntegrity) }
        HubCard(R.string.spoof_subtab_apps, R.string.spoof_hub_apps_sub, rom.hooks) { onOpen(HubRoute.AppsSpoof) }
        HubCard(R.string.spoof_subtab_framework, R.string.spoof_hub_framework_sub, rom.hooks) { onOpen(HubRoute.Framework) }
        HubCard(R.string.spoof_subtab_zk, R.string.spoof_hub_zk_sub, rom.hooks) { onOpen(HubRoute.Rom) }
        HubCard(R.string.fcm_guard_title, R.string.fcm_hub_sub, true) { onOpen(HubRoute.Fcm) }
        HubCard(R.string.spoof_subtab_hidden, R.string.spoof_hub_hidden_sub, true) { onOpen(HubRoute.Hidden) }
    }
}

@Composable
private fun HubCard(title: Int, sub: Int, supported: Boolean, onClick: () -> Unit) {
    Card(
        Modifier
            .fillMaxWidth()
            .alpha(if (supported) 1f else 0.45f)
            .clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(sub), style = MaterialTheme.typography.bodySmall)
            if (!supported) {
                Text(
                    stringResource(R.string.rom_hooks_card_note),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
