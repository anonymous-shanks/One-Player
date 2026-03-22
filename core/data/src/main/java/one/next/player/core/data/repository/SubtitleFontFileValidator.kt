package one.next.player.core.data.repository

import android.graphics.fonts.Font
import java.io.File
import javax.inject.Inject

fun interface SubtitleFontFileValidator {
    fun validate(file: File)
}

class AndroidSubtitleFontFileValidator @Inject constructor() : SubtitleFontFileValidator {
    override fun validate(file: File) {
        require(file.exists()) { "Font file does not exist" }
        require(file.length() > 0L) { "Font file is empty" }
        Font.Builder(file).build()
    }
}
