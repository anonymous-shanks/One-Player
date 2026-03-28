package one.next.player.feature.player.scripting

import android.content.Context
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

    init {
        setupNextPlayerLuaApi()
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
        if (scriptDir == null || !scriptDir.exists()) {
            Logger.info(TAG, "Lua scripts directory is null or does not exist.")
            return
        }

        val files = scriptDir.listFiles { _, name -> name.endsWith(".lua") }

        if (files == null || files.isEmpty()) {
            Logger.info(TAG, "No .lua files found.")
            return
        }

        // SharedPreferences access karenge scripts ka ON/OFF status check karne ke liye
        val prefs = context.getSharedPreferences("lua_script_prefs", Context.MODE_PRIVATE)

        files.forEach { file ->
            // Har script ki apni key hogi, jaise: "script_auto_speed_boost.lua"
            // By default hum true rakhenge taaki pehli baar sab run hon.
            val isScriptEnabled = prefs.getBoolean("script_${file.name}", true)
            
            if (isScriptEnabled) {
                try {
                    Logger.info(TAG, "Loading script: ${file.name}")
                    val chunk = globals.loadfile(file.absolutePath)
                    chunk.call()
                } catch (e: Exception) {
                    Logger.error(TAG, "Error executing script ${file.name}", e)
                }
            } else {
                Logger.info(TAG, "Skipping disabled script: ${file.name}")
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
}
