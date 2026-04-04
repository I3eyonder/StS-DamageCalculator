package dmgcalculator.util

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.megacrit.cardcrawl.helpers.FontHelper
import dmgcalculator.config.ModConfig
import dmgcalculator.entities.OutcomeResult
import dmgcalculator.entities.Range
import dmgcalculator.entities.sorted
import dmgcalculator.renderer.toSegments

/**
 * Created by HieuPT on 1/15/26.
 */

/**
 * Renders a single line of text that supports inline hex‑color markup.
 *
 *
 * The markup format allows segments of the text to be wrapped in tags of the form:
 * <pre>
 * &lt;#RRGGBB&gt;colored text&lt;/#&gt;
</pre> *
 * Each opening tag applies the specified color (in standard 6‑digit hex), and the closing
 * tag &lt;/#&gt; restores the color back to white. Any text outside of tags is rendered in white.
 *
 *
 * This method:
 *
 *  * Parses the markup into ordered text segments with associated colors
 *  * Measures the total width of all segments
 *  * Horizontally centers the entire line around the given (x, y) coordinate
 *  * Draws each segment left‑to‑right using its assigned color
 *
 *
 *
 * Only single‑line rendering is supported. For multi‑line text, split the input on newline
 * characters and call this method once per line.
 *
 * @param this@renderFormattedText     the SpriteBatch used for rendering
 * @param font   the BitmapFont used to draw the text
 * @param x      the horizontal center position of the full rendered line
 * @param y      the baseline Y coordinate for the text
 * @param markup the input string containing plain text and optional hex‑color tags
 */
fun SpriteBatch.renderFormattedText(font: BitmapFont, x: Float, y: Float, markup: String) {
    val segments = markup.toSegments()

    // 1) Measure total width
    var totalWidth = 0f
    segments.forEach { seg ->
        val gl = GlyphLayout(font, seg.text)
        totalWidth += gl.width
    }

    // 2) Compute starting x so the whole line is centered once
    var cx = x - totalWidth / 2f

    // 3) Draw each segment in order, left to right
    segments.forEach { seg ->
        font.color = seg.color
        val gl = GlyphLayout(font, seg.text)
        font.draw(this, gl, cx, y)

        cx += gl.width // move cursor to the right
    }

    font.color = Color.WHITE // reset
}

/**
 * Renders multi‑line text that supports inline hex‑color markup on each line.
 *
 *
 * The input string may contain newline characters (`\n`) to indicate
 * explicit line breaks. Each line is processed independently using the same
 * hex‑color markup rules as [.renderFormattedText], allowing segments
 * of text to be wrapped in tags of the form:
 * <pre>
 * &lt;#RRGGBB&gt;colored text&lt;/#&gt;
</pre> *
 * Each opening tag applies the specified color (in standard 6‑digit hex), and the
 * closing tag &lt;/#&gt; restores the color back to white. Any text outside of tags is
 * rendered in white.
 *
 *
 * This method:
 *
 *  * Splits the input into lines using `\n`
 *  * Computes the total vertical height of the block
 *  * Vertically centers the entire block around the given Y coordinate
 *  * Renders each line using [.renderFormattedText]
 *
 *
 *
 * This method does not perform automatic word‑wrapping; only explicit newline
 * characters create new lines.
 *
 * @param this@renderFormattedMultiline          the SpriteBatch used for rendering
 * @param font        the BitmapFont used to draw the text
 * @param x           the horizontal center position for each rendered line
 * @param y           the vertical center of the entire multi‑line block
 * @param markup      the input string containing plain text and optional hex‑color tags
 * @param lineSpacing the vertical spacing (in pixels) between consecutive lines
 */
fun SpriteBatch.renderFormattedMultiline(
    font: BitmapFont, x: Float, y: Float, markup: String, lineSpacing: Float,
) {
    // Split into lines by '\n'
    val lines = markup.split("\n").dropLastWhile { it.isEmpty() }

    // Starting Y so the whole block is vertically centered
    val totalHeight = lines.size * font.capHeight + (lines.size - 1) * lineSpacing
    var cy = y + totalHeight / 2f

    lines.forEach { line ->
        renderFormattedText(font, x, cy, line)
        cy -= font.capHeight + lineSpacing
    }
}

fun SpriteBatch.renderFixedSizeMessage(message: String, x: Float, y: Float) {
    val font = FontHelper.cardTitleFont
    val oldScale = font.getData().scaleX
    font.getData().setScale(1.0f) // lock size
    renderFormattedMultiline(font, x, y, message, 8f)
    font.getData().setScale(oldScale) // restore
}

fun String.colored(hexColor: String): String = if (hexColor.startsWith("#")) {
    "<$hexColor>$this</#>"
} else {
    "<#$hexColor>$this</#>"
}

fun Range.colored(color: String, reversed: Boolean = false): String {
    return if (isConstantsValue) {
        value.toString().colored(color)
    } else {
        val a = if (reversed) max else min
        val b = if (reversed) min else max
        "%s~%s".format(a, b).colored(color)
    }
}

fun buildOutcomeMessage(
    worstOutcomeResult: OutcomeResult,
    bestOutcomeResult: OutcomeResult,
): String = buildList {
    // --- Taken damage ---
    if (worstOutcomeResult.actionResult.takenDamage > 0 || bestOutcomeResult.actionResult.takenDamage > 0) {
        add(
            "Take %s damage".format(
                Range(worstOutcomeResult.actionResult.takenDamage, bestOutcomeResult.actionResult.takenDamage).sorted()
                    .colored("#FF0000")
            )
        )
    }

    // --- Blocked amount --- (only if enabled in config)
    if (ModConfig.showBlockInfo) {
        if (worstOutcomeResult.actionResult.blockedAmount == bestOutcomeResult.actionResult.blockedAmount) {
            if (bestOutcomeResult.actionResult.blockedAmount > 0) {
                add(
                    "(%s blocked)".format(
                        bestOutcomeResult.actionResult.blockedAmount.toString().colored("#00FF00"),
                    )
                )
                add(
                    "(%s blocks remaining)".format(
                        bestOutcomeResult.creatureInfo.remainBlock.toString().colored("#00FF00"),
                    )
                )
            }
        } else {
            add(
                "(%s blocked)".format(
                    Range(
                        worstOutcomeResult.actionResult.blockedAmount,
                        bestOutcomeResult.actionResult.blockedAmount,
                    ).sorted().colored("#00FF00"),
                )
            )
            add(
                "(%s blocks remaining)".format(
                    Range(
                        worstOutcomeResult.creatureInfo.remainBlock,
                        bestOutcomeResult.creatureInfo.remainBlock,
                    ).sorted().colored("#00FF00"),
                )
            )
        }
    }

    // --- Extra info ---
    if (worstOutcomeResult.actionResult.adjustHP == bestOutcomeResult.actionResult.adjustHP) {
        if (bestOutcomeResult.actionResult.adjustHP < 0) {
            add(
                "Lose extra %s HP".format(
                    bestOutcomeResult.actionResult.adjustHP.unaryMinus().toString().colored("#FF0000")
                )
            )
        } else if (bestOutcomeResult.actionResult.adjustHP > 0) {
            add(
                "Gain extra %s HP".format(
                    bestOutcomeResult.actionResult.adjustHP.toString().colored("#00FF00")
                )
            )
        }
    } else {
        when {
            worstOutcomeResult.actionResult.adjustHP < 0 && bestOutcomeResult.actionResult.adjustHP < 0 -> {
                add(
                    "Lose extra %s HP".format(
                        Range(
                            worstOutcomeResult.actionResult.adjustHP.unaryMinus(),
                            bestOutcomeResult.actionResult.adjustHP.unaryMinus(),
                        ).sorted().colored("#FF0000")
                    )
                )
            }

            worstOutcomeResult.actionResult.adjustHP > 0 && bestOutcomeResult.actionResult.adjustHP > 0 -> {
                add(
                    "Gain extra %s HP".format(
                        Range(
                            worstOutcomeResult.actionResult.adjustHP,
                            bestOutcomeResult.actionResult.adjustHP,
                        ).sorted().colored("#00FF00")
                    )
                )
            }

            else -> {
                if (worstOutcomeResult.actionResult.adjustHP < 0) {
                    add(
                        "Lose extra %s HP".format(
                            Range(
                                0,
                                worstOutcomeResult.actionResult.adjustHP.unaryMinus(),
                            ).sorted().colored("#FF0000")
                        )
                    )
                }
                if (bestOutcomeResult.actionResult.adjustHP > 0) {
                    add(
                        "Gain extra %s HP".format(
                            Range(
                                0,
                                bestOutcomeResult.actionResult.adjustHP,
                            ).sorted().colored("#00FF00")
                        )
                    )
                }
            }
        }
    }

    // --- Remaining HP ---
    if (worstOutcomeResult.creatureInfo.remainHP == bestOutcomeResult.creatureInfo.remainHP) {
        if (bestOutcomeResult.creatureInfo.isDead) {
            add("DEAD".colored("#FF0000"))
        } else {
            add(
                "%s HP remaining".format(
                    bestOutcomeResult.creatureInfo.remainHP.toString().colored("#00BFFF")
                )
            )
        }
    } else {
        add(
            "%s HP remaining".format(
                Range(
                    worstOutcomeResult.creatureInfo.remainHP,
                    bestOutcomeResult.creatureInfo.remainHP,
                ).sorted()
                    .colored("#00BFFF", reversed = true)
            )
        )
    }
}.joinToString("\n")