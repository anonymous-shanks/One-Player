package one.next.player.feature.player.scripting

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LuaScriptMenuBottomSheet(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("lua_script_prefs", Context.MODE_PRIVATE) }
    
    val folderUriString = prefs.getString("script_folder_uri", "content://com.android.externalstorage.documents/tree/primary%3ANextPlayerScripts")
    
    var scriptFiles by remember { mutableStateOf<List<File>>(emptyList()) }

    LaunchedEffect(Unit) {
        var dir: File? = null
        if (folderUriString != null && folderUriString.contains("primary%3A")) {
            val parts = folderUriString.split("primary%3A")
            if (parts.size > 1) {
                val folderName = Uri.decode(parts[1])
                dir = File(Environment.getExternalStorageDirectory().absolutePath + "/" + folderName)
            }
        }
        if (dir != null && dir.exists()) {
            scriptFiles = dir.listFiles { _, name -> name.endsWith(".lua") }?.toList() ?: emptyList()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Manage Lua Scripts",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (scriptFiles.isEmpty()) {
                Text(
                    text = "No .lua files found in the scripts directory.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(48.dp))
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(scriptFiles) { file ->
                        var isEnabled by remember {
                            mutableStateOf(prefs.getBoolean("script_${file.name}", true))
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = file.name,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = if (isEnabled) "Enabled" else "Disabled",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isEnabled,
                                onCheckedChange = { checked ->
                                    isEnabled = checked
                                    // Save the new state in preferences
                                    prefs.edit().putBoolean("script_${file.name}", checked).apply()
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}
