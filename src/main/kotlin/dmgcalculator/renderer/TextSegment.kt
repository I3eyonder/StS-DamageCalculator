package dmgcalculator.renderer

import com.badlogic.gdx.graphics.Color

data class TextSegment(val text: String, val color: Color)

fun String.toSegments(): List<TextSegment> {
    val segments = mutableListOf<TextSegment>()
    val colorStack = ArrayDeque<Color>()
    colorStack.add(Color.WHITE.cpy()) // base color

    var i = 0
    val sb = StringBuilder()

    fun flushText() {
        if (sb.isNotEmpty()) {
            segments += TextSegment(sb.toString(), colorStack.last().cpy())
            sb.clear()
        }
    }

    while (i < length) {
        if (this.startsWith("</#>", i)) {
            flushText()
            if (colorStack.size > 1) colorStack.removeLast()
            i += 4
            continue
        }

        if (this.startsWith("<#", i)) {
            val end = indexOf('>', i)
            if (end != -1) {
                val hex = substring(i + 2, end)
                flushText()
                colorStack.add(Color.valueOf(hex))
                i = end + 1
                continue
            }
        }

        sb.append(this[i])
        i++
    }

    flushText()
    return segments
}

