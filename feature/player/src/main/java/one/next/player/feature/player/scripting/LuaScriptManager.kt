package one.next.player.feature.player.scripting

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.next.player.core.common.Logger
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.jse.JsePlatform
import java.io.File

class LuaScriptManager(
    private val context: Context,
    private val player: ExoPlayer,
    private val scriptDir: File?
) {
    private val TAG = "LuaScriptManager"
    private var globals: Globals = JsePlatform.standardGlobals()
    private var receiver: BroadcastReceiver? = null

    init {
        setupNextPlayerLuaApi()
        registerReceiver()
    }

    private fun registerReceiver() {
        receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == "one.next.player.ACTION_LUA_TOGGLED") {
                    val scriptName = intent.getStringExtra("script_name")
                    val isEnabled = intent.getBooleanExtra("is_enabled", false)
                    
                    if (isEnabled && scriptName != null) {
                        Logger.info(TAG, "Runtime execution requested for: $scriptName")
                        executeScript(scriptName)
                    }
                }
            }
        }
        val filter = IntentFilter("one.next.player.ACTION_LUA_TOGGLED")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
    }

    private fun executeScript(scriptName: String) {
        if (scriptDir == null || !scriptDir.exists()) return
        
        val prefs = context.getSharedPreferences("lua_script_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("enable_lua", true)) return // Stop if master is off

        val file = File(scriptDir, scriptName)
        if (file.exists()) {
            try {
                val chunk = globals.loadfile(file.absolutePath)
                chunk.call()
            } catch (e: Exception) {
                Logger.error(TAG, "Error executing script ${file.name}", e)
            }
        }
    }

    private fun setupNextPlayerLuaApi() {
        val npTable = LuaValue.tableOf()

        npTable.set("set_speed", object : OneArgFunction() {
            override fun call(speed: LuaValue): LuaValue {
                CoroutineScope(Dispatchers.Main).launch {
                    player.playbackParameters = PlaybackParameters(speed.checkdouble().toFloat())
                }
                return LuaValue.NIL
            }
        })

        npTable.set("osd_message", object : OneArgFunction() {
            override fun call(message: LuaValue): LuaValue {
                Logger.info(TAG, "OSD Message: ${message.checkjstring()}")
                return LuaValue.NIL
            }
        })

        globals.set("np", npTable)
    }

    fun loadScripts() {
        if (scriptDir == null || !scriptDir.exists()) return
        val prefs = context.getSharedPreferences("lua_script_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("enable_lua", true)) return

        val files = scriptDir.listFiles { _, name -> name.endsWith(".lua") }
        if (files == null || files.isEmpty()) return

        files.forEach { file ->
            val isScriptEnabled = prefs.getBoolean("script_${file.name}", true)
            if (isScriptEnabled) {
                executeScript(file.name)
            }
        }
    }

    fun onFileLoaded() {
        try {
            val onFileLoadedFunc = globals.get("on_file_loaded")
            if (onFileLoadedFunc.isfunction()) {
                onFileLoadedFunc.call()
            }
        } catch (e: Exception) {
            Logger.error(TAG, "Error calling on_file_loaded in Lua", e)
        }
    }
    
    fun release() {
        try {
            receiver?.let { context.unregisterReceiver(it) }
        } catch (e: Exception) {
            Logger.error(TAG, "Error unregistering receiver", e)
        }
    }
}
