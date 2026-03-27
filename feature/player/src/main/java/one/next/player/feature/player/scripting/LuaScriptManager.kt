package one.next.player.feature.player.scripting

import android.content.Context
import android.widget.Toast
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.next.player.core.common.Logger
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
import org.luaj.vm2.lib.jse.JsePlatform
import java.io.File

class LuaScriptManager(
    private val context: Context,
    private val player: Player,
    private val scriptDir: File?
) {
    private val globals: Globals = JsePlatform.standardGlobals()
    private val TAG = "LuaScriptManager"

    init {
        setupNextPlayerApi()
    }

    private fun setupNextPlayerApi() {
        val npTable = LuaValue.tableOf()

        npTable.set("play", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                CoroutineScope(Dispatchers.Main).launch { player.play() }
                return LuaValue.NIL
            }
        })

        npTable.set("pause", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                CoroutineScope(Dispatchers.Main).launch { player.pause() }
                return LuaValue.NIL
            }
        })

        npTable.set("set_speed", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val speed = arg.checkdouble().toFloat()
                CoroutineScope(Dispatchers.Main).launch { player.setPlaybackSpeed(speed) }
                return LuaValue.NIL
            }
        })

        npTable.set("osd_message", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val message = arg.checkjstring()
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
                return LuaValue.NIL
            }
        })

        globals.set("np", npTable)
    }

    fun loadScripts() {
        if (scriptDir == null || !scriptDir.exists()) return
        
        val files = scriptDir.listFiles { _, name -> name.endsWith(".lua") } ?: return
        files.forEach { file ->
            try {
                Logger.info(TAG, "Loading script: ${file.name}")
                val chunk = globals.loadfile(file.absolutePath)
                chunk.call()
            } catch (e: Exception) {
                Logger.error(TAG, "Error executing script ${file.name}", e)
            }
        }
    }

    fun onFileLoaded() {
        try {
            val func = globals.get("on_file_loaded")
            if (func.isfunction()) {
                func.call()
            }
        } catch (e: Exception) {
            Logger.error(TAG, "Error executing on_file_loaded in Lua", e)
        }
    }

    fun runScript(fileName: String) {
        if (scriptDir == null) return
        val file = File(scriptDir, fileName)
        if (file.exists()) {
            try {
                val chunk = globals.loadfile(file.absolutePath)
                chunk.call()
                Toast.makeText(context, "Ran: $fileName", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Logger.error(TAG, "Error running $fileName", e)
            }
        }
    }
}
