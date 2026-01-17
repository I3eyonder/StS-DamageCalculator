package dmgcalculator.renderer

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.megacrit.cardcrawl.cards.DamageInfo
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
import dmgcalculator.entities.DmgInfo
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
        var remainBlock = AbstractDungeon.player.currentBlock

        fun applyRelics(dmgInfo: DmgInfo) {
            AbstractDungeon.player.relics.forEach { relic ->
                when (relic.relicId) {
                    TungstenRod.ID ->
                        dmgInfo.amount = (dmgInfo.amount - 1).coerceAtLeast(0)

                    Torii.ID -> {
                        if (dmgInfo.type !in listOf(
                                DamageInfo.DamageType.HP_LOSS,
                                DamageInfo.DamageType.THORNS
                            ) && dmgInfo.amount in 2..5
                        ) {
                            dmgInfo.amount = 1
                        }
                    }
                }
            }
        }

        // --- Build incoming damage list with all effects applied ---
        val incomingDamages = getIncomingDamages().apply {

            // Apply Intangible
            if (AbstractDungeon.player.hasPower(IntangiblePlayerPower.POWER_ID)) {
                forEach { it.amount = it.amount.coerceAtMost(1) }
            }

            var bufferAmount = AbstractDungeon.player.getPower(BufferPower.POWER_ID)?.amount ?: 0

            forEach { dmgInfo ->
                if (dmgInfo.type != DamageInfo.DamageType.HP_LOSS) {
                    // Block
                    val blocked = Utils.getBlockedAmount(dmgInfo.amount, remainBlock)
                    val netDamage = Utils.getNetDamageAmount(dmgInfo.amount, remainBlock)
                    remainBlock -= blocked
                    dmgInfo.amount = netDamage
                }
                // Buffer
                if (dmgInfo.amount > 0 && bufferAmount > 0) {
                    bufferAmount--
                    dmgInfo.amount = 0
                }

                applyRelics(dmgInfo)
            }
        }

        // --- Final calculations ---
        val takenDamage = incomingDamages
            .filter { it.type != DamageInfo.DamageType.HP_LOSS }
            .sumOf { it.amount }

        val loseHP = incomingDamages
            .filter { it.type == DamageInfo.DamageType.HP_LOSS }
            .sumOf { it.amount }

        val blockedAmount = AbstractDungeon.player.currentBlock - remainBlock

        val remainHP = AbstractDungeon.player.currentHealth -
                incomingDamages.sumOf { it.amount }

        val dmgColor = when {
            takenDamage == 0 -> "#00FF00"
            remainHP > 0 -> "#FF4444"
            else -> "#FF0000"
        }

        // --- Build output message ---
        return buildString {
            append("Take %s damage".format(takenDamage.toString().colored(dmgColor)))

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
                    power.getPrivateField("hpLoss"),
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
