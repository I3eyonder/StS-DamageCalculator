package dmgcalculator.renderer

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.megacrit.cardcrawl.cards.DamageInfo
import com.megacrit.cardcrawl.cards.curses.Decay
import com.megacrit.cardcrawl.cards.curses.Regret
import com.megacrit.cardcrawl.cards.status.Burn
import com.megacrit.cardcrawl.dungeons.AbstractDungeon
import com.megacrit.cardcrawl.helpers.FontHelper
import com.megacrit.cardcrawl.powers.CombustPower
import com.megacrit.cardcrawl.powers.ConstrictedPower
import dmgcalculator.entities.DmgInfo
import dmgcalculator.util.*
import dmgcalculator.util.Utils.calculateDamage

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
        val calculatedOutcome = getIncomingDamages().calculateDamage(
            AbstractDungeon.player
        )

        val dmgColor = if (calculatedOutcome.damageAmount.value == 0) {
            "#00FF00"
        } else {
            "#FF0000"
        }

        // --- Build output message ---
        return buildString {
            append("Take %s damage".format(calculatedOutcome.damageAmount.toString().colored(dmgColor)))

            if (calculatedOutcome.blockedAmount.value > 0) {
                append("\n")
                append("(%s blocked)".format(calculatedOutcome.blockedAmount.toString().colored("#00FF00")))
            }

            if (calculatedOutcome.adjustHPAmount.value < 0) {
                append("\n")
                append(
                    "Lose extra %s HP".format(
                        calculatedOutcome.adjustHPAmount.value.unaryMinus().toString().colored("#FF0000")
                    )
                )
            } else if (calculatedOutcome.adjustHPAmount.value > 0) {
                append("\n")
                append(
                    "Gain extra %s HP".format(
                        calculatedOutcome.adjustHPAmount.value.toString().colored("#FF0000")
                    )
                )
            }

            if (calculatedOutcome.remainHPAmount.value > 0) {
                append("\n")
                append("%s HP remains".format(calculatedOutcome.remainHPAmount.value.toString().colored("#00BFFF")))
            } else {
                append("\n")
                append("%s".format("DEAD".colored("#FF0000")))
            }
        }
    }

    private fun getIncomingDamages(): List<DmgInfo> {
        // Resolve hand intent damages
        val handDamages = AbstractDungeon.player.hand.group.mapNotNull { card ->
            when (card.cardID) {
                Burn.ID -> DmgInfo(card.magicNumber, DamageInfo.DamageType.THORNS)
                Decay.ID -> DmgInfo(2, DamageInfo.DamageType.THORNS)
                Regret.ID -> DmgInfo(AbstractDungeon.player.hand.size(), DamageInfo.DamageType.HP_LOSS)
                else -> null
            }
        }

        // Resolve power intent damages
        val powerDamages = AbstractDungeon.player.powers.mapNotNull { power ->
            when (power.ID) {
                ConstrictedPower.POWER_ID -> DmgInfo(
                    power.amount,
                    DamageInfo.DamageType.THORNS
                )

                CombustPower.POWER_ID -> DmgInfo(
                    power.getPrivateField<Int>("hpLoss"),
                    DamageInfo.DamageType.HP_LOSS
                )

                else -> null
            }
        }

        // Resolve monster intent damages
        val monsterIntentDamages = AbstractDungeon.getMonsters().aliveMonsters.getIntentDamages()

        return handDamages + powerDamages + monsterIntentDamages
    }
}
