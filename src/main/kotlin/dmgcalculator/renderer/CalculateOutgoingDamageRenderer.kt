package dmgcalculator.renderer

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.megacrit.cardcrawl.cards.AbstractCard
import com.megacrit.cardcrawl.dungeons.AbstractDungeon
import com.megacrit.cardcrawl.helpers.FontHelper
import com.megacrit.cardcrawl.monsters.AbstractMonster
import com.megacrit.cardcrawl.powers.CombustPower
import com.megacrit.cardcrawl.powers.TheBombPower
import com.megacrit.cardcrawl.powers.watcher.OmegaPower
import com.megacrit.cardcrawl.relics.StoneCalendar
import dmgcalculator.entities.Range
import dmgcalculator.util.*
import dmgcalculator.util.Utils.getBlockedAmount
import dmgcalculator.util.Utils.getNetDamageAmount

object CalculateOutgoingDamageRenderer {
    private val cardIntentDamageRange = Range()
    private val cardNetDamageRange = Range()
    private val endTurnNetDamageRange = Range()
    private val blockedAmountRange = Range()
    private val remainBlockAmountRange = Range()
    private val remainHPRange = Range()

    fun render(sb: SpriteBatch, hoveredCard: AbstractCard?) {
        if (AbstractDungeon.isScreenUp) return
        if (AbstractDungeon.getCurrMapNode() == null) return
        var tmpHoveredCard: AbstractCard? = null
        if (hoveredCard?.canDealDamage == true) {
            tmpHoveredCard = hoveredCard.makeStatEquivalentCopy().apply {
                applyPowers()
            }
        }
        val monsters = AbstractDungeon.getMonsters().monsters
        val aliveMonsterNumber = monsters.aliveMonsterNumber
        monsters.forEachIndexed { index, monster ->
            if (!monster.isDeadOrEscaped) {
                initRemainHPAndBlock(monster)
                val msgBuilder = StringBuilder()
                if (tmpHoveredCard != null) {
                    tmpHoveredCard.calculateCardDamage(monster)
                    val damagePerHit = getDamagePerHit(tmpHoveredCard, index)
                    updateCardIntentDamageRange(tmpHoveredCard, aliveMonsterNumber, damagePerHit)
                    updateCardNetDamageAndBlocked()
                    updateRemainHPAndBlock(cardNetDamageRange)
                    buildDamageInfoMessage(msgBuilder, cardNetDamageRange)
                } else {
                    cardIntentDamageRange.set(0)
                    updateCardNetDamageAndBlocked()
                }
                if (remainHPRange.max > 0) {
                    if (AbstractDungeon.player.hasEndTurnDamage) {
                        updateEndTurnNetDamageAndBlocked()
                        updateRemainHPAndBlock(endTurnNetDamageRange)
                        if (tmpHoveredCard != null) {
                            msgBuilder.append("\n")
                        }
                        msgBuilder.append("--End Turn--\n")
                        buildDamageInfoMessage(msgBuilder, endTurnNetDamageRange)
                    }
                }
                val font = FontHelper.cardTitleFont
                val oldScale = font.getData().scaleX
                font.getData().setScale(1.0f) // lock size
                sb.renderFormattedMultiline(
                    font, monster.hb.cX, monster.hb.cY + monster.hb.height,
                    msgBuilder.toString(), 8f
                )
                font.getData().setScale(oldScale) // restore
            }
        }
    }

    private fun buildDamageInfoMessage(msgBuilder: StringBuilder, cardNetDamageRange: Range) {
        if (cardNetDamageRange.isConstantsValue) {
            msgBuilder.append(
                String.format(
                    "Deal %s damage",
                    cardNetDamageRange.value.toString().colored("#00FF00")
                )
            )
        } else {
            msgBuilder.append(
                String.format(
                    "Deal %s damage",
                    (cardNetDamageRange.min.toString() + "~" + cardNetDamageRange.max).colored("#00FF00")
                )
            )
        }
        if (blockedAmountRange.isConstantsValue) {
            if (blockedAmountRange.value > 0) {
                msgBuilder.append(" ")
                    .append(String.format("(%s blocked)", blockedAmountRange.value.toString().colored("#00FF00")))
            }
        } else {
            msgBuilder.append(" ")
                .append(
                    String.format(
                        "(%s blocked)",
                        (blockedAmountRange.min.toString() + "~" + blockedAmountRange.max).colored("#00FF00")
                    )
                )
        }
        if (remainHPRange.isConstantsValue) {
            if (remainHPRange.value <= 0) {
                msgBuilder.append("\n")
                    .append("DEAD".colored("#FF0000"))
            } else {
                msgBuilder.append("\n")
                    .append(
                        String.format(
                            "%s HP remains",
                            remainHPRange.value.toString().colored("#00BFFF")
                        )
                    )
            }
        } else {
            msgBuilder.append("\n")
                .append(
                    String.format(
                        "%s HP remains",
                        (remainHPRange.max.toString() + "~" + remainHPRange.min).colored("#00BFFF")
                    )
                )
        }
    }

    private val endTurnDamageAmount: Int
        get() {
            val powerDamage = getEndTurnPowerDamage()
            val relicDamage = getEndTurnRelicDamage()
            return powerDamage + relicDamage
        }

    private fun getEndTurnRelicDamage(): Int {
        var damage = 0;
        AbstractDungeon.player.run {
            if (getRelic(StoneCalendar.ID)?.counter == 7) {
                damage += 52
            }
        }
        return damage;
    }

    private fun getEndTurnPowerDamage(): Int = AbstractDungeon.player.powers.sumOf { power ->
        when {
            power.ID == CombustPower.POWER_ID ||
                    power.ID == OmegaPower.POWER_ID -> power.amount

            power.ID.contains(TheBombPower.POWER_ID) && power.amount == 1 ->
                power.getPrivateField("damage")

            else -> 0
        }
    }


    private fun updateEndTurnNetDamageAndBlocked() {
        endTurnDamageAmount.let {
            endTurnNetDamageRange.set(
                getNetDamageAmount(it, remainBlockAmountRange.max),
                getNetDamageAmount(it, remainBlockAmountRange.min)
            )
            blockedAmountRange.set(
                getBlockedAmount(it, remainBlockAmountRange.min),
                getBlockedAmount(it, remainBlockAmountRange.max)
            )
        }
    }

    private fun updateCardNetDamageAndBlocked() {
        cardNetDamageRange.set(
            getNetDamageAmount(cardIntentDamageRange.min, remainBlockAmountRange.max),
            getNetDamageAmount(cardIntentDamageRange.max, remainBlockAmountRange.min)
        )
        blockedAmountRange.set(
            getBlockedAmount(cardIntentDamageRange.min, remainBlockAmountRange.min),
            getBlockedAmount(cardIntentDamageRange.max, remainBlockAmountRange.max)
        )
    }

    private fun updateRemainHPAndBlock(damageRange: Range) {
        remainHPRange.set(
            (remainHPRange.min - damageRange.max).coerceAtLeast(0),
            (remainHPRange.max - damageRange.min).coerceAtLeast(0)
        )
        remainBlockAmountRange.set(
            (remainBlockAmountRange.max - blockedAmountRange.max).coerceAtLeast(0),
            (remainBlockAmountRange.max - blockedAmountRange.min).coerceAtLeast(0)
        )
    }

    private fun initRemainHPAndBlock(monster: AbstractMonster) {
        remainHPRange.set(monster.currentHealth)
        remainBlockAmountRange.set(monster.currentBlock)
    }

    private fun updateCardIntentDamageRange(tmpCard: AbstractCard, monsterNum: Int, damagePerHit: Int) {
        val hitCount = tmpCard.cardHitCount
        if (tmpCard.isRandomAttackCard && monsterNum > 1) {
            cardIntentDamageRange.set(0, damagePerHit * hitCount)
        } else {
            cardIntentDamageRange.set(damagePerHit * hitCount)
        }
    }

    private fun getDamagePerHit(tmpCard: AbstractCard, monsterIndex: Int): Int = if (tmpCard.multiDamage != null) {
        tmpCard.multiDamage[monsterIndex]
    } else {
        tmpCard.damage
    }
}
