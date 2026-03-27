package one.next.player.feature.player.scripting

import android.content.Context
import android.widget.Toast
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

        // Lua function: np.set_speed(speed)
        npTable.set("set_speed", object : OneArgFunction() {
            override fun call(speed: LuaValue): LuaValue {
                CoroutineScope(Dispatchers.Main).launch {
                    player.playbackParameters = PlaybackParameters(speed.checkfloat().toFloat())
                }
                return LuaValue.NIL
            }
        })

        // Lua function: np.osd_message("message")
        npTable.set("osd_message", object : OneArgFunction() {
            override fun call(message: LuaValue): LuaValue {
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, message.checkjstring(), Toast.LENGTH_SHORT).show()
                }
                return LuaValue.NIL
            }
        })

        globals.set("np", npTable)
    }

    fun loadScripts() {
        if (scriptDir == null) {
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(context, "Lua Debug: Folder path NULL hai!", Toast.LENGTH_SHORT).show()
            }
            return
        }

        if (!scriptDir.exists()) {
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(context, "Lua Debug: Folder exist nahi karta ya storage permission denied hai!\nPath: ${scriptDir.absolutePath}", Toast.LENGTH_LONG).show()
            }
            return
        }

        val files = scriptDir.listFiles { _, name -> name.endsWith(".lua") }

        if (files == null || files.isEmpty()) {
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(context, "Lua Debug: Folder mil gaya par andar koi .lua file nahi mili!", Toast.LENGTH_SHORT).show()
            }
            return
        }

        files.forEach { file ->
            try {
                Logger.info(TAG, "Loading script: ${file.name}")
                val chunk = globals.loadfile(file.absolutePath)
                chunk.call()
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "Lua Success: ${file.name} load ho gayi! 🎉", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Logger.error(TAG, "Error executing script ${file.name}", e)
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "Lua Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
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
