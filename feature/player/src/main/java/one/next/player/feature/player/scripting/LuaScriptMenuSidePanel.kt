package one.next.player.feature.player.scripting

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    
    var isMasterEnabled by remember { mutableStateOf(prefs.getBoolean("enable_lua", true)) }

    LaunchedEffect(Unit) {
        var dir: File? = null
        if (folderUriString != null && folderUriString.contains("primary%3A")) {
            val parts = folderUriString.split("primary%3A")
            if (parts.size > 1) {
                val folderName = Uri.decode(parts[1])
                dir = File(Environment.getExternalStorageDirectory().absolutePath + "/" + folderName)
            }
        } else if (folderUriString != null) {
             dir = File(Uri.parse(folderUriString).path ?: "")
        }
        
        if (dir != null && dir.exists()) {
            scriptFiles = dir.listFiles { _, name -> name.endsWith(".lua") }?.toList() ?: emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Drag handle (Top center pill)
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                .align(Alignment.CenterHorizontally)
        )
        
        Spacer(modifier = Modifier.height(20.dp))

        // Header Title & Close Icon
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Lua Scripts",
                style = MaterialTheme.typography.titleLarge,
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

        Spacer(modifier = Modifier.height(16.dp))

        // Master Toggle Card (Blue Tint)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Lua Scripts", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("Lua runtime is on. Tap a script below to arm it for playback.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                }
                Switch(
                    checked = isMasterEnabled,
                    onCheckedChange = {
                        isMasterEnabled = it
                        prefs.edit().putBoolean("enable_lua", it).apply()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Scripts List Cards (Grey Tint)
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(scriptFiles) { file ->
                var isEnabled by remember { mutableStateOf(prefs.getBoolean("script_${file.name}", true)) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Code, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(file.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            Text(if (isEnabled) "Enabled" else "Disabled", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { checked ->
                                isEnabled = checked
                                prefs.edit().putBoolean("script_${file.name}", checked).apply()
                                
                                // Broadcast event for instant runtime execution
                                if (isMasterEnabled && checked) {
                                    val intent = Intent("one.next.player.ACTION_LUA_TOGGLED").apply {
                                        putExtra("script_name", file.name)
                                        putExtra("is_enabled", true)
                                        setPackage(context.packageName)
                                    }
                                    context.sendBroadcast(intent)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
