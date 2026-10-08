package com.xiaomi.toolbox.ui.apps

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xiaomi.toolbox.R

/** Multi-select app picker with search; returns the checked packages on confirm. */
@Composable
fun MultiAppPickerDialog(
    initial: Set<String>,
    exclude: Set<String>,
    onConfirm: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    val ctx = LocalContext.current
    var q by remember { mutableStateOf("") }
    val apps = remember {
        ctx.packageManager.getInstalledApplications(0)
            .sortedBy { it.loadLabel(ctx.packageManager).toString().lowercase() }
    }
    val checked = remember { mutableStateMapOf<String, Boolean>().apply { initial.forEach { put(it, true) } } }
    val filtered = apps.filter {
        val label = it.loadLabel(ctx.packageManager).toString()
        (q.isBlank() || label.contains(q, true) || it.packageName.contains(q, true)) && it.packageName !in exclude
    }.take(120)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.fcm_protect_title)) },
        text = {
            Column {
                OutlinedTextField(
                    q,
                    { q = it },
                    label = { Text(stringResource(R.string.search)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                LazyColumn {
                    items(filtered) { ai: ApplicationInfo ->
                        val sel = checked[ai.packageName] == true
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    checked[ai.packageName] = !sel
                                }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = sel, onCheckedChange = { checked[ai.packageName] = it })
                            Column {
                                Text(ai.loadLabel(ctx.packageManager).toString())
                                Text(
                                    ai.packageName,
                                    modifier = Modifier.padding(bottom = 4.dp),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(checked.filterValues { it }.keys.toSet()) }) {
                Text(stringResource(R.string.save_flags))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
