package dmgcalculator.util

import com.megacrit.cardcrawl.cards.AbstractCard
import com.megacrit.cardcrawl.cards.blue.BeamCell
import com.megacrit.cardcrawl.cards.green.Eviscerate
import com.megacrit.cardcrawl.cards.green.RiddleWithHoles
import com.megacrit.cardcrawl.cards.green.Skewer
import com.megacrit.cardcrawl.cards.green.Terror
import com.megacrit.cardcrawl.cards.purple.CrushJoints
import com.megacrit.cardcrawl.cards.purple.Indignation
import com.megacrit.cardcrawl.cards.purple.Tantrum
import com.megacrit.cardcrawl.cards.red.*
import com.megacrit.cardcrawl.cards.tempCards.Expunger
import com.megacrit.cardcrawl.dungeons.AbstractDungeon
import com.megacrit.cardcrawl.monsters.AbstractMonster
import com.megacrit.cardcrawl.powers.DoubleTapPower
import com.megacrit.cardcrawl.powers.DuplicationPower
import com.megacrit.cardcrawl.powers.VulnerablePower
import com.megacrit.cardcrawl.relics.ChemicalX
import com.megacrit.cardcrawl.ui.panels.EnergyPanel
import dmgcalculator.entities.Action
import dmgcalculator.entities.asGroupActions
import dmgcalculator.entities.flatten
import dmgcalculator.util.Utils.addToBottom

private val randomAttackCards = listOf(
    SwordBoomerang.ID
)

private val giveVulnearableCards = listOf(
    Bash.ID,
    ThunderClap.ID,
    Shockwave.ID,
    Uppercut.ID,
    Terror.ID,
    BeamCell.ID,
    CrushJoints.ID,
    Indignation.ID
)

val AbstractCard.isRandomAttackCard: Boolean
    get() = randomAttackCards.contains(cardID)

val AbstractCard.isCardGiveVulnearable: Boolean
    get() = giveVulnearableCards.contains(cardID)

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

fun AbstractCard.getIntentActions(
    monster: AbstractMonster,
    monsterIndex: Int,
    aliveMonsterCount: Int,
): List<Action> {
    val baseAction = createIntentAction(monster, monsterIndex, aliveMonsterCount)
    val acttions = mutableListOf(baseAction)
    // Apply powers that modify action here if needed
    AbstractDungeon.player.powers.forEach { power ->
        when (power.ID) {
            DoubleTapPower.POWER_ID, DuplicationPower.POWER_ID -> {
                if (type == AbstractCard.CardType.ATTACK) {
                    if (isCardGiveVulnearable && !monster.hasPower(VulnerablePower.POWER_ID)) {
                        val tmpCard = makeSameInstanceOf()
                        val extraAction = monster.applyTemporaryPower(VulnerablePower(monster, 1, false)) {
                            tmpCard.createIntentAction(monster, monsterIndex, aliveMonsterCount)
                        }
                        acttions.addToBottom(extraAction)
                    } else {
                        acttions.addToBottom(baseAction)
                    }
                } else if (power.ID == DuplicationPower.POWER_ID) {
                    acttions.addToBottom(baseAction)
                }
            }
        }
    }
    return acttions.flatten()
}

private fun AbstractCard.createIntentAction(
    monster: AbstractMonster,
    monsterIndex: Int,
    aliveMonsterCount: Int,
): Action {
    return if (type == AbstractCard.CardType.ATTACK) {
        calculateCardDamage(monster)
        val damagePerHit = getDamagePerHit(monsterIndex)
        val cardHitCount = this.cardHitCount
        if (cardHitCount > 1) {
            if (isRandomAttackCard && aliveMonsterCount > 1) {
                List(cardHitCount) {
                    Action.DamageNormal(0, damagePerHit)
                }.asGroupActions()
            } else {
                List(cardHitCount) {
                    Action.DamageNormal(damagePerHit)
                }.asGroupActions()
            }
        } else {
            Action.DamageNormal(damagePerHit)
        }
    } else {
        Action.NoAction
    }
}