package dmgcalculator.renderer

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.megacrit.cardcrawl.cards.curses.Decay
import com.megacrit.cardcrawl.cards.curses.Regret
import com.megacrit.cardcrawl.cards.status.Burn
import com.megacrit.cardcrawl.dungeons.AbstractDungeon
import com.megacrit.cardcrawl.helpers.FontHelper
import com.megacrit.cardcrawl.powers.BufferPower
import com.megacrit.cardcrawl.powers.CombustPower
import com.megacrit.cardcrawl.powers.ConstrictedPower
import com.megacrit.cardcrawl.powers.IntangiblePlayerPower
import com.megacrit.cardcrawl.relics.Torii
import com.megacrit.cardcrawl.relics.TungstenRod
import dmgcalculator.DmgCalculatorMod
import dmgcalculator.util.*

object CalculateIncomingDamageRenderer {

    private var cachedMsg: String? = null

    fun render(sb: SpriteBatch) {
        if (AbstractDungeon.isScreenUp) return
        if (AbstractDungeon.getCurrMapNode() == null) return
        if (AbstractDungeon.getMonsters() == null) return
        if (AbstractDungeon.player == null) return

        val msg = if (Utils.isPlayerTurn) {
            buildDamageInfoMessage().also {
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

    private fun buildDamageInfoMessage(): String {
        var remainHP = AbstractDungeon.player.currentHealth
        var remainBlock = AbstractDungeon.player.currentBlock
        var blockedAmount = 0

        val loseHP = getLoseHP()
        remainHP -= loseHP

        // Resolve incoming damages
        val incomingDamages = getIncomingDamages().let { damages ->
            // Applying Intangible if needed
            if (AbstractDungeon.player.hasPower(IntangiblePlayerPower.POWER_ID)) {
                damages.map { it.coerceAtMost(1) }
            } else {
                damages
            }
        }

        var bufferPowerAmount = AbstractDungeon.player.getPower(BufferPower.POWER_ID)?.amount ?: 0

        // Compute net damages and update HP/block state
        val netIncomingDamages = incomingDamages.map { rawDamage ->
            val blocked = Utils.getBlockedAmount(rawDamage, remainBlock)
            val rawNet = Utils.getNetDamageAmount(rawDamage, remainBlock)

            // Apply Buffer first
            var netDamage = if (bufferPowerAmount > 0) {
                bufferPowerAmount--
                0
            } else {
                rawNet
            }
            // Apply relics
            netDamage = AbstractDungeon.player.relics.fold(netDamage) { acc, relic ->
                when (relic.relicId) {
                    TungstenRod.ID -> (acc - 1).coerceAtLeast(0)
                    Torii.ID -> if (acc in 2..5) 1 else acc
                    else -> acc
                }
            }.coerceAtLeast(0)

            blockedAmount += blocked
            remainBlock -= blocked
            remainHP -= netDamage

            netDamage
        }

        val netIncomingDamage = netIncomingDamages.sum()
        val dmgColor = when {
            netIncomingDamage == 0 -> "#00FF00"
            remainHP > 0 -> "#FF4444"
            else -> "#FF0000"
        }

        return buildString {
            append("Take %s damage".format(netIncomingDamage.toString().colored(dmgColor)))

            if (blockedAmount > 0) {
                append("\n")
                append("(%s blocked)".format(blockedAmount.toString().colored("#00FF00")))
            }

            if (loseHP > 0) {
                append("\n")
                append("Lose extra %s HP".format(loseHP.toString().colored("#FF0000")))
            }

            if (remainHP > 0) {
                append("\n")
                append("%s HP remains".format(remainHP.toString().colored("#00BFFF")))
            } else {
                append("\n")
                append("%s".format("DEAD".colored("#FF0000")))
            }
        }

    }

    private fun getIncomingDamages(): List<Int> {
        // Resolve power intent damages
        val powerDamages = AbstractDungeon.player.powers.mapNotNull { power ->
            when (power.ID) {
                ConstrictedPower.POWER_ID -> power.amount
                else -> null
            }
        }

        // Resolve hand intent damages
        val handDamages = AbstractDungeon.player.hand.group.mapNotNull { card ->
            when (card.cardID) {
                Burn.ID -> card.magicNumber
                Decay.ID -> 2
                else -> null
            }
        }

        // Resolve monster intent damages
        val monsterIntentDamages = AbstractDungeon.getMonsters().aliveMonsters.getIntentDamages()

        return powerDamages + handDamages + monsterIntentDamages
    }

    private fun getLoseHP(): Int {
        var loseHP = 0
        // Check Combust
        val combustPower = AbstractDungeon.player.getPower(CombustPower.POWER_ID)
        if (combustPower != null) {
            loseHP += combustPower.getPrivateField<Int>("hpLoss")
        }
        // Check hands
        loseHP += AbstractDungeon.player.hand.group.fold(0) { acc, card ->
            if (card.cardID == Regret.ID) {
                acc + AbstractDungeon.player.hand.size()
            } else {
                acc
            }
        }
        return loseHP
    }
}
