package one.next.player.settings.screens.scripts

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import one.next.player.core.ui.components.PreferenceSwitchWithDivider
import one.next.player.core.ui.components.TopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageScriptsScreen(
    onNavigateUp: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("lua_script_prefs", Context.MODE_PRIVATE)
    val folderUriString = prefs.getString("script_folder_uri", null)
    
    // State to hold the list of scanned .lua files
    var scriptFiles by remember { mutableStateOf<List<DocumentFile>>(emptyList()) }

    // This block runs when the screen opens to safely scan the selected folder
    LaunchedEffect(folderUriString) {
        if (folderUriString != null) {
            try {
                val treeUri = Uri.parse(folderUriString)
                val documentFile = DocumentFile.fromTreeUri(context, treeUri)
                if (documentFile != null && documentFile.isDirectory) {
                    scriptFiles = documentFile.listFiles()
                        .filter { it.name?.endsWith(".lua") == true }
                        .sortedBy { it.name }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = "Manage Lua Scripts",
                onNavigationIconClick = onNavigateUp
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (scriptFiles.isEmpty()) {
                Text(
                    text = if (folderUriString == null) "No folder selected. Please select a folder first from the previous screen." else "No .lua scripts found in the selected folder.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn {
                    items(scriptFiles) { file ->
                        val fileName = file.name ?: "Unknown.lua"
                        // Create a unique key for each script to save its on/off state
                        val prefKey = "script_enabled_$fileName"
                        var isEnabled by remember { mutableStateOf(prefs.getBoolean(prefKey, false)) }

                        PreferenceSwitchWithDivider(
                            title = fileName,
                            subtitle = if (isEnabled) "Enabled" else "Disabled",
                            isChecked = isEnabled,
                            onClick = {
                                isEnabled = !isEnabled
                                prefs.edit().putBoolean(prefKey, isEnabled).apply()
                            }
                        )
                    }
                }
            }
        }
    }
}
