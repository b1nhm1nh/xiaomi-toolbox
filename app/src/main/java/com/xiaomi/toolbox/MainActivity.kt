package com.xiaomi.toolbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.xiaomi.toolbox.ui.apps.AppsSpoofScreen
import com.xiaomi.toolbox.ui.fcm.FcmScreen
import com.xiaomi.toolbox.ui.framework.FrameworkScreen
import com.xiaomi.toolbox.ui.hidden.HiddenSettingsScreen
import com.xiaomi.toolbox.ui.home.HomeScreen
import com.xiaomi.toolbox.ui.hub.HubScreen
import com.xiaomi.toolbox.ui.integrity.IntegrityScreen
import com.xiaomi.toolbox.ui.nav.HubRoute
import com.xiaomi.toolbox.ui.nav.Tab
import com.xiaomi.toolbox.ui.rom.RomSettingsScreen
import com.xiaomi.toolbox.ui.settings.SettingsScreen
import com.xiaomi.toolbox.ui.theme.ToolboxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToolboxTheme {
                var tab by rememberSaveable { mutableStateOf(Tab.Home) }
                var hub by rememberSaveable { mutableStateOf(HubRoute.Hub) }
                // back pops the hub sub-screen first, then leaves the app
                BackHandler(enabled = tab == Tab.Toolbox && hub != HubRoute.Hub) {
                    hub = HubRoute.Hub
                }
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = tab == Tab.Home,
                                onClick = { tab = Tab.Home },
                                icon = { Icon(Icons.Default.Home, contentDescription = null) },
                                label = { Text(stringResource(R.string.tab_home)) },
                            )
                            NavigationBarItem(
                                selected = tab == Tab.Toolbox,
                                onClick = { tab = Tab.Toolbox; hub = HubRoute.Hub },
                                icon = { Icon(Icons.Default.Build, contentDescription = null) },
                                label = { Text(stringResource(R.string.tab_spoof)) },
                            )
                            NavigationBarItem(
                                selected = tab == Tab.Settings,
                                onClick = { tab = Tab.Settings },
                                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                label = { Text(stringResource(R.string.tab_settings)) },
                            )
                        }
                    },
                ) { pad ->
                    androidx.compose.foundation.layout.Box(Modifier.padding(pad)) {
                        when (tab) {
                            Tab.Home -> HomeScreen { tab = Tab.Toolbox; hub = HubRoute.Fcm }
                            Tab.Settings -> SettingsScreen()
                            Tab.Toolbox -> when (hub) {
                                HubRoute.Hub -> HubScreen { hub = it }
                                HubRoute.PlayIntegrity -> IntegrityScreen { hub = HubRoute.Hub }
                                HubRoute.AppsSpoof -> AppsSpoofScreen { hub = HubRoute.Hub }
                                HubRoute.Framework -> FrameworkScreen { hub = HubRoute.Hub }
                                HubRoute.Rom -> RomSettingsScreen { hub = HubRoute.Hub }
                                HubRoute.Hidden -> HiddenSettingsScreen { hub = HubRoute.Hub }
                                HubRoute.Fcm -> FcmScreen { hub = HubRoute.Hub }
                            }
                        }
                    }
                }
            }
        }
    }
}
