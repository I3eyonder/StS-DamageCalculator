package dmgcalculator.util

import com.megacrit.cardcrawl.cards.AbstractCard
import com.megacrit.cardcrawl.cards.green.Eviscerate
import com.megacrit.cardcrawl.cards.green.RiddleWithHoles
import com.megacrit.cardcrawl.cards.green.Skewer
import com.megacrit.cardcrawl.cards.purple.Tantrum
import com.megacrit.cardcrawl.cards.red.*
import com.megacrit.cardcrawl.cards.tempCards.Expunger
import com.megacrit.cardcrawl.dungeons.AbstractDungeon
import com.megacrit.cardcrawl.monsters.AbstractMonster
import com.megacrit.cardcrawl.relics.ChemicalX
import com.megacrit.cardcrawl.ui.panels.EnergyPanel
import dmgcalculator.entities.Action

private val randomAttackCards = listOf(
    SwordBoomerang.ID
)

val AbstractCard.isRandomAttackCard: Boolean
    get() = randomAttackCards.contains(cardID)

val AbstractCard.cardHitCount: Int
    get() = when (cardID) {
        TwinStrike.ID -> 2
        RiddleWithHoles.ID -> 5
        Pummel.ID, Tantrum.ID, SwordBoomerang.ID -> magicNumber
        Whirlwind.ID, Skewer.ID, Expunger.ID -> {
            var hits = EnergyPanel.totalCount
            if (AbstractDungeon.player.hasRelic(ChemicalX.ID)) {
                hits += 2
            }
            hits
        }

        Eviscerate.ID -> 3
        FiendFire.ID -> AbstractDungeon.player.hand.size() - 1
        else -> 1
    }


val AbstractCard.canDealDamage: Boolean
    get() = baseDamage >= 0


fun AbstractCard.getDamagePerHit(monsterIndex: Int): Int = if (multiDamage != null) {
    multiDamage[monsterIndex]
} else {
    damage
}

fun AbstractCard.getAttackIntentActions(
    monster: AbstractMonster,
    monsterIndex: Int,
    aliveMonsterCount: Int,
): List<Action> {
    return if (type == AbstractCard.CardType.ATTACK) {
        calculateCardDamage(monster)
        val damagePerHit = getDamagePerHit(monsterIndex)
        if (isRandomAttackCard && aliveMonsterCount > 1) {
            List(cardHitCount) {
                Action.DamageNormal(0, damagePerHit)
            }
        } else {
            List(cardHitCount) {
                Action.DamageNormal(damagePerHit)
            }
        }
    } else {
        emptyList()
    }
}