package one.next.player.feature.player.scripting

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun LuaScriptMenuSidePanel(
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
        } else if (folderUriString != null) {
             // Fallback
             dir = File(Uri.parse(folderUriString).path ?: "")
        }
        
        if (dir != null && dir.exists()) {
            scriptFiles = dir.listFiles { _, name -> name.endsWith(".lua") }?.toList() ?: emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header (Matching Native Next Player Style)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Manage Lua Scripts",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = onDismissRequest) {
                Icon(
                    imageVector = Icons.Rounded.Close, 
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (scriptFiles.isEmpty()) {
            Text(
                text = "No .lua files found in the directory.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isEnabled) "Enabled" else "Disabled",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { checked ->
                                isEnabled = checked
                                prefs.edit().putBoolean("script_${file.name}", checked).apply()
                                
                                // Broadcast event to execute script immediately at runtime
                                val intent = Intent("one.next.player.ACTION_LUA_TOGGLED").apply {
                                    putExtra("script_name", file.name)
                                    putExtra("is_enabled", checked)
                                    setPackage(context.packageName)
                                }
                                context.sendBroadcast(intent)
                                
                                // Warning for disabled state
                                if (!checked) {
                                    Toast.makeText(context, "Script disabled. (Restart video to undo runtime changes)", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
