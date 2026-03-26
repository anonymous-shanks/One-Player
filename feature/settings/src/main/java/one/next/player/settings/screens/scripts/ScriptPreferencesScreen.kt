package one.next.player.settings.screens.scripts

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import one.next.player.core.ui.components.ClickablePreferenceItem
import one.next.player.core.ui.components.PreferenceSwitchWithDivider
import one.next.player.core.ui.components.NextTopAppBar
import one.next.player.core.ui.designsystem.NextIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptPreferencesScreen(
    onNavigateUp: () -> Unit,
    onManageScriptsClick: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("lua_script_prefs", Context.MODE_PRIVATE)
    
    var selectedFolderUri by remember { mutableStateOf(prefs.getString("script_folder_uri", "Not selected")) }
    var enableLuaScripts by remember { mutableStateOf(prefs.getBoolean("enable_lua", false)) }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            prefs.edit().putString("script_folder_uri", it.toString()).apply()
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(it, takeFlags)
            selectedFolderUri = it.toString()
        }
    }

    Scaffold(
        topBar = {
            NextTopAppBar(
                title = "Advanced / Scripts",
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(imageVector = NextIcons.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            ClickablePreferenceItem(
                title = "Pick configuration storage location",
                description = selectedFolderUri ?: "Tap to select folder",
                onClick = { folderPickerLauncher.launch(null) }
            )

            PreferenceSwitchWithDivider(
                title = "Enable Lua Scripts",
                description = "Load Lua scripts from configuration directory",
                isChecked = enableLuaScripts,
                onClick = { 
                    enableLuaScripts = !enableLuaScripts
                    prefs.edit().putBoolean("enable_lua", enableLuaScripts).apply()
                },
                onChecked = { 
                    enableLuaScripts = !enableLuaScripts
                    prefs.edit().putBoolean("enable_lua", enableLuaScripts).apply()
                }
            )

            ClickablePreferenceItem(
                title = "Manage Lua Scripts",
                description = "Tap to enable or disable specific scripts",
                onClick = onManageScriptsClick
            )
        }
    }
}
