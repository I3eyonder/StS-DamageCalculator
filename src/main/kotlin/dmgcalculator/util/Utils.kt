package dmgcalculator.util

import com.megacrit.cardcrawl.cards.AbstractCard
import com.megacrit.cardcrawl.cards.AbstractCard.CardType
import com.megacrit.cardcrawl.dungeons.AbstractDungeon
import com.megacrit.cardcrawl.monsters.AbstractMonster.Intent
import com.megacrit.cardcrawl.orbs.AbstractOrb
import com.megacrit.cardcrawl.powers.BurstPower
import com.megacrit.cardcrawl.powers.DoubleTapPower
import com.megacrit.cardcrawl.powers.DuplicationPower
import com.megacrit.cardcrawl.powers.EchoPower
import com.megacrit.cardcrawl.relics.Necronomicon
import dmgcalculator.entities.Action
import dmgcalculator.entities.SimpleCardInfo
import kotlin.math.min

object Utils {

    fun getNetDamageAmount(rawDamageAmount: Int, blockAmount: Int): Int =
        (rawDamageAmount - blockAmount).coerceAtLeast(0)

    fun getBlockedAmount(damageAmount: Int, blockAmount: Int): Int =
        min(damageAmount, blockAmount)

    val Intent.isAttackingIntent: Boolean
        get() = this in listOf(
            Intent.ATTACK,
            Intent.ATTACK_BUFF,
            Intent.ATTACK_DEBUFF,
            Intent.ATTACK_DEFEND
        )

    fun <T> MutableList<T>.addToTop(element: T) {
        this.add(0, element)
    }

    fun <T> MutableList<T>.addToBottom(element: T) {
        this.add(element)
    }

    fun <T> MutableList<T>.replacesWith(listBuilder: (List<T>) -> List<T>) {
        listBuilder(this@replacesWith).also {
            clear()
            addAll(it)
        }
    }

    fun <T> MutableList<T>.replacesWith(list: List<T>) {
        clear()
        addAll(list)
    }

    fun List<AbstractCard>.toSimpleCardInfoList() = map {
        SimpleCardInfo(
            cardId = it.cardID,
            type = it.type,
            isHovered = it == AbstractDungeon.player.hoveredCard,
        )
    }

    fun MutableList<Action>.addDuplicationCardActionIfNeeded(
        baseCard: AbstractCard,
        cardActionBuilder: AbstractCard.() -> Action,
    ) {
        val duplicationCard = baseCard.makeSameInstanceOf().apply {
            purgeOnUse = true
        }
        // Apply Duplication power if needed
        AbstractDungeon.player.powers.forEach { power ->
            when (power.ID) {
                DoubleTapPower.POWER_ID -> {
                    if (duplicationCard.type == CardType.ATTACK && power.amount > 0) {
                        add(duplicationCard.cardActionBuilder())
                    }
                }

                DuplicationPower.POWER_ID -> {
                    if (power.amount > 0) {
                        add(duplicationCard.cardActionBuilder())
                    }
                }

                BurstPower.POWER_ID -> {
                    if (duplicationCard.type == CardType.SKILL && power.amount > 0) {
                        add(duplicationCard.cardActionBuilder())
                    }
                }

                EchoPower.POWER_ID -> {
                    val cardsDoubledThisTurn = power.getPrivateField<Int>("cardsDoubledThisTurn") ?: 0
                    if (power.amount > 0 &&
                        AbstractDungeon.actionManager.cardsPlayedThisTurn.size + 1 - cardsDoubledThisTurn <= power.amount
                    ) {
                        add(duplicationCard.cardActionBuilder())
                    }
                }
            }
        }

        // Apply Necronomicon relic if needed
        AbstractDungeon.player.getRelic(Necronomicon.ID)?.let { necronomiconRelic ->
            if (duplicationCard.type == CardType.ATTACK &&
                (duplicationCard.costForTurn >= 2 && !duplicationCard.freeToPlayOnce || duplicationCard.cost == -1 && duplicationCard.energyOnUse >= 2) &&
                necronomiconRelic.checkTrigger()
            ) {
                add(duplicationCard.cardActionBuilder())
            }
        }
    }

    fun List<AbstractOrb>.getEvokedOrbsWhenExceededMax() = dropLast(AbstractDungeon.player.maxOrbs)

    fun List<AbstractOrb>.getRetainedOrbsWhenExceededMax() = takeLast(AbstractDungeon.player.maxOrbs)
}
