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
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import one.next.player.core.ui.components.PreferenceItem
import one.next.player.core.ui.components.PreferenceSwitchWithDivider
import one.next.player.core.ui.components.TopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptPreferencesScreen(
    onNavigateUp: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("lua_script_prefs", Context.MODE_PRIVATE)
    
    // States to hold our UI data
    var selectedFolderUri by remember { mutableStateOf(prefs.getString("script_folder_uri", "Not selected")) }
    var enableLuaScripts by remember { mutableStateOf(prefs.getBoolean("enable_lua", false)) }

    // Android Storage Access Framework launcher to pick a folder
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            // Save the selected URI so we remember it
            prefs.edit().putString("script_folder_uri", it.toString()).apply()
            
            // Take persistable URI permission so we can read it later even after app restarts
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(it, takeFlags)
            
            selectedFolderUri = it.toString()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = "Advanced / Scripts",
                onNavigationIconClick = onNavigateUp
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            
            // 1. Pick Configuration Storage Location (Matches your screenshot)
            PreferenceItem(
                title = "Pick configuration storage location",
                subtitle = selectedFolderUri ?: "Tap to select folder",
                onClick = {
                    folderPickerLauncher.launch(null)
                }
            )

            // 2. Enable Lua Scripts Toggle
            PreferenceSwitchWithDivider(
                title = "Enable Lua Scripts",
                subtitle = "Load Lua scripts from configuration directory",
                isChecked = enableLuaScripts,
                onClick = { 
                    enableLuaScripts = !enableLuaScripts
                    prefs.edit().putBoolean("enable_lua", enableLuaScripts).apply()
                }
            )

            // 3. Manage Lua Scripts Button (We will link this to the next screen later)
            PreferenceItem(
                title = "Manage Lua Scripts",
                subtitle = "Tap to enable or disable specific scripts",
                onClick = {
                    // TODO: Navigate to the script checklist screen
                }
            )
            
            // 4. Custom Lua
            PreferenceItem(
                title = "Custom Lua",
                subtitle = "Create and manage custom Lua buttons",
                onClick = {
                    // TODO: Implement custom buttons later
                }
            )
        }
    }
}
