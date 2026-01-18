package dmgcalculator.renderer

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.megacrit.cardcrawl.cards.AbstractCard
import com.megacrit.cardcrawl.dungeons.AbstractDungeon
import com.megacrit.cardcrawl.helpers.FontHelper
import com.megacrit.cardcrawl.monsters.AbstractMonster
import com.megacrit.cardcrawl.powers.BufferPower
import com.megacrit.cardcrawl.powers.CombustPower
import com.megacrit.cardcrawl.powers.IntangiblePlayerPower
import com.megacrit.cardcrawl.powers.TheBombPower
import com.megacrit.cardcrawl.powers.watcher.OmegaPower
import com.megacrit.cardcrawl.relics.StoneCalendar
import dmgcalculator.entities.Range
import dmgcalculator.util.*

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
                    updateRemainHPAndBlock(cardNetDamageRange, blockedAmountRange)
                    buildDamageInfoMessage(msgBuilder, cardNetDamageRange)
                } else {
                    cardIntentDamageRange.set(0)
                    updateCardNetDamageAndBlocked()
                }
                if (remainHPRange.max > 0) {
                    if (AbstractDungeon.player.hasEndTurnDamage) {
                        endTurnNetDamageRange.set(0)
                        updateEndTurnNetDamageAndBlocked(monster)
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

    private fun Range.coloredRange(color: String, reversed: Boolean = false): String {
        return if (isConstantsValue) {
            value.toString().colored(color)
        } else {
            val a = if (reversed) max else min
            val b = if (reversed) min else max
            "%s~%s".format(a, b).colored(color)
        }
    }

    private fun buildDamageInfoMessage(
        msgBuilder: StringBuilder,
        damageRange: Range,
    ) {
        // --- Deal damage ---
        msgBuilder.append(
            "Deal %s damage".format(
                damageRange.coloredRange("#00FF00")
            )
        )

        // --- Blocked amount ---
        if (blockedAmountRange.isConstantsValue) {
            if (blockedAmountRange.value > 0) {
                msgBuilder.append("\n")
                    .append(
                        "(%s blocked)".format(
                            blockedAmountRange.value.toString().colored("#00FF00")
                        )
                    )
            }
        } else {
            msgBuilder
                .append("\n").append(
                    "(%s blocked)".format(
                        blockedAmountRange.coloredRange("#00FF00")
                    )
                )
        }

        // --- Remaining HP ---
        if (remainHPRange.isConstantsValue) {
            if (remainHPRange.value <= 0) {
                msgBuilder.append("\n")
                    .append("DEAD".colored("#FF0000"))
            } else {
                msgBuilder.append("\n")
                    .append(
                        "%s HP remains".format(
                            remainHPRange.value.toString().colored("#00BFFF")
                        )
                    )
            }
        } else {
            msgBuilder.append("\n")
                .append(
                    "%s HP remains".format(
                        remainHPRange.coloredRange("#00BFFF", reversed = true)
                    )
                )
        }
    }

    private fun getEndTurnDamageAmounts(): List<Int> = buildList {
        AbstractDungeon.player.run {
            // relics damage
            if (getRelic(StoneCalendar.ID)?.counter == 7) {
                add(52)
            }

            // powers damage
            powers.forEach { power ->
                when {
                    power.ID == CombustPower.POWER_ID ||
                            power.ID == OmegaPower.POWER_ID -> add(power.amount)

                    power.ID.contains(TheBombPower.POWER_ID) && power.amount == 1 ->
                        add(power.getPrivateField("damage"))
                }
            }
        }
    }

    private fun updateEndTurnNetDamageAndBlocked(monster: AbstractMonster) {
        val rawEndTurnDamgeAmounts = getEndTurnDamageAmounts()
        val endTurnDamageAmounts = if (monster.hasPower(IntangiblePlayerPower.POWER_ID)) {
            List(rawEndTurnDamgeAmounts.size) { 1 }
        } else {
            rawEndTurnDamgeAmounts
        }

        var bufferPowerAmount = monster.getPower(BufferPower.POWER_ID)?.amount ?: 0

        endTurnDamageAmounts.forEach { damageInstance ->
            val netDamageRange = Range(
                Utils.getNetDamageAmount(damageInstance, remainBlockAmountRange.max),
                Utils.getNetDamageAmount(damageInstance, remainBlockAmountRange.min)
            )
            val blockedAmountRange = Range(
                Utils.getBlockedAmount(damageInstance, remainBlockAmountRange.min),
                Utils.getBlockedAmount(damageInstance, remainBlockAmountRange.max)
            )
            if (netDamageRange.min > 0 && netDamageRange.max > 0 && bufferPowerAmount > 0) {
                bufferPowerAmount--
                netDamageRange.set(0)
                blockedAmountRange.set(0)
            }
            updateRemainHPAndBlock(netDamageRange, blockedAmountRange)
            endTurnNetDamageRange.set(
                endTurnNetDamageRange.min + netDamageRange.min,
                endTurnNetDamageRange.max + netDamageRange.max
            )
            this.blockedAmountRange.set(
                this.blockedAmountRange.min + blockedAmountRange.min,
                this.blockedAmountRange.max + blockedAmountRange.max
            )
        }
    }

    private fun updateCardNetDamageAndBlocked() {
        cardNetDamageRange.set(
            Utils.getNetDamageAmount(cardIntentDamageRange.min, remainBlockAmountRange.max),
            Utils.getNetDamageAmount(cardIntentDamageRange.max, remainBlockAmountRange.min)
        )
        blockedAmountRange.set(
            Utils.getBlockedAmount(cardIntentDamageRange.min, remainBlockAmountRange.min),
            Utils.getBlockedAmount(cardIntentDamageRange.max, remainBlockAmountRange.max)
        )
    }

    private fun updateRemainHPAndBlock(damageRange: Range, blockedAmountRange: Range) {
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
