package dmgcalculator.renderer

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.megacrit.cardcrawl.dungeons.AbstractDungeon
import com.megacrit.cardcrawl.helpers.FontHelper
import dmgcalculator.util.*
import kotlin.math.max

object CalculateIncomingDamageRenderer {

    private var cachedMsg: String? = null

    fun render(sb: SpriteBatch) {
        if (AbstractDungeon.isScreenUp) return
        if (AbstractDungeon.getCurrMapNode() == null) return
        if (AbstractDungeon.getMonsters() == null) return
        if (AbstractDungeon.player == null) return

        val msg = if (Utils.isPlayerTurn) {
            buildIncomingDamageMessage().also {
                cachedMsg = it
            }
        } else {
            cachedMsg.orEmpty()
        }

        if (msg.isNotEmpty()) {
            val font = FontHelper.cardTitleFont
            val oldScale = font.getData().scaleX
            font.getData().setScale(1.0f) // lock size
            sb.renderFormattedMultiline(
                font,
                AbstractDungeon.player.hb.cX,
                AbstractDungeon.player.hb.cY + AbstractDungeon.player.hb.height,
                msg,
                8f
            )
            font.getData().setScale(oldScale) // restore
        }
    }

    private fun buildIncomingDamageMessage(): String {
        val currentHealth = AbstractDungeon.player.currentHealth
        val blockedDamage = IntArray(1)
        val netIncomingDamage = getTotalNetIncomingDamage(blockedDamage)
        val remainHP = max(0, currentHealth - netIncomingDamage)
        val dmgColor = when {
            netIncomingDamage == 0 -> "#00FF00"
            remainHP > 0 -> "#FF4444"
            else -> "#FF0000"
        }

        return buildString {
            append(String.format("Take %s damage", netIncomingDamage.toString().colored(dmgColor)))
            if (blockedDamage[0] > 0) {
                append(" ")
                    .append(String.format("(%s blocked)", blockedDamage[0].toString().colored("#00FF00")))
            }
            if (remainHP > 0) {
                append("\n")
                    .append(String.format("%s HP remains", remainHP.toString().colored("#00BFFF")))
            } else {
                append("\n")
                    .append(String.format("%s", "DEAD".colored("#FF0000")))
            }
        }
    }

    private fun getTotalNetIncomingDamage(blockedDamage: IntArray): Int {
        var netIncomingDamage: Int = totalRawIncomingDamage
        blockedDamage[0] = calculateBlock()
        netIncomingDamage -= blockedDamage[0]
        return netIncomingDamage.coerceAtLeast(0)
    }

    private fun calculateBlock(): Int {
        val currentBlock = AbstractDungeon.player.currentBlock
        return currentBlock
    }

    private val totalRawIncomingDamage: Int
        get() = AbstractDungeon.getMonsters().monsters
            .sumOf { m -> if (!m.isDeadOrEscaped && m.intent.isAttackingIntent) m.totalIntentDamage else 0 }

}
