package com.xiaomi.toolbox.ui.hidden

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xiaomi.toolbox.R
import com.xiaomi.toolbox.data.HiddenIntents
import com.xiaomi.toolbox.ui.BackBar

@Composable
fun HiddenSettingsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        BackBar(stringResource(R.string.spoof_subtab_hidden), onBack)
        LazyColumn(Modifier.padding(16.dp)) {
            items(HiddenIntents.items) { item ->
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { HiddenIntents.launch(ctx, item) },
                ) {
                    Text(stringResource(item.titleRes), Modifier.padding(16.dp))
                }
            }
        }
    }
}
