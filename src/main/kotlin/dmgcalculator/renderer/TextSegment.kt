package dmgcalculator.renderer

import com.badlogic.gdx.graphics.Color

data class TextSegment(val text: String, val color: Color)

fun String.toSegments(): List<TextSegment> {
    val result = mutableListOf<TextSegment>()
    var currentColor = Color.WHITE

    var i = 0
    while (i < length) {
        // Detect closing tag: </#>

        if (this.startsWith("</#>", i)) {
            currentColor = Color.WHITE
            i += 4 // skip "</#>"
            continue
        }

        // Detect opening tag: <#RRGGBB>
        if (this.startsWith("<#", i)) {
            val end = indexOf('>', i)
            if (end != -1) {
                val hex = this.substring(i + 2, end).trim { it <= ' ' }

                currentColor = try {
                    Color.valueOf(hex)
                } catch (_: Exception) {
                    Color.WHITE
                }

                i = end + 1
                continue
            }
        }

        // Normal text until next tag
        var nextTag = indexOf("<", i)
        if (nextTag == -1) nextTag = length

        val text = this.substring(i, nextTag)
        if (!text.isEmpty()) {
            result.add(TextSegment(text, currentColor))
        }

        i = nextTag
    }

    return result
}
