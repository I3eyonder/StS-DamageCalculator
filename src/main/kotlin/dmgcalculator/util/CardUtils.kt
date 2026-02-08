package dmgcalculator.util

import com.megacrit.cardcrawl.cards.AbstractCard
import com.megacrit.cardcrawl.cards.blue.BeamCell
import com.megacrit.cardcrawl.cards.blue.RipAndTear
import com.megacrit.cardcrawl.cards.blue.ThunderStrike
import com.megacrit.cardcrawl.cards.green.*
import com.megacrit.cardcrawl.cards.purple.CrushJoints
import com.megacrit.cardcrawl.cards.purple.Indignation
import com.megacrit.cardcrawl.cards.purple.Ragnarok
import com.megacrit.cardcrawl.cards.purple.Tantrum
import com.megacrit.cardcrawl.cards.red.*
import com.megacrit.cardcrawl.cards.tempCards.Expunger
import com.megacrit.cardcrawl.dungeons.AbstractDungeon
import com.megacrit.cardcrawl.monsters.AbstractMonster
import com.megacrit.cardcrawl.orbs.Lightning
import com.megacrit.cardcrawl.powers.*
import com.megacrit.cardcrawl.powers.watcher.VigorPower
import com.megacrit.cardcrawl.relics.ChemicalX
import com.megacrit.cardcrawl.ui.panels.EnergyPanel
import dmgcalculator.entities.Action
import dmgcalculator.entities.asGroupedAction
import dmgcalculator.entities.flatten
import dmgcalculator.util.Utils.addToBottom
import dmgcalculator.util.Utils.replacesWith

private val randomAttackCards = listOf(
    SwordBoomerang.ID,
    Ragnarok.ID,
    RipAndTear.ID,
    ThunderStrike.ID
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

val AbstractCard.canGiveVulnearable: Boolean
    get() = giveVulnearableCards.contains(cardID)

fun AbstractCard.getHitCount(): Int = when (cardID) {
    TwinStrike.ID -> 2
    RiddleWithHoles.ID -> 5
    Pummel.ID, Tantrum.ID, SwordBoomerang.ID, Ragnarok.ID, RipAndTear.ID -> magicNumber
    Whirlwind.ID, Skewer.ID, Expunger.ID -> {
        var hits = EnergyPanel.totalCount
        if (AbstractDungeon.player.hasRelic(ChemicalX.ID)) {
            hits += 2
        }
        hits
    }

    Eviscerate.ID -> 3
    FiendFire.ID -> AbstractDungeon.player.hand.size() - 1
    DaggerSpray.ID -> 2
    ThunderStrike.ID -> {
        AbstractDungeon.actionManager.orbsChanneledThisCombat.count {
            it is Lightning
        }
    }

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
    val actions = mutableListOf(baseAction)
    val player = AbstractDungeon.player

    // Apply powers that modify action here if needed
    fun createExtraAttackAction(): Action {
        val monsterTemporayPowers = buildList {
            if (canGiveVulnearable && !monster.hasPower(VulnerablePower.POWER_ID)) {
                add(VulnerablePower(monster, 1, false))
            }
        }
        val playerTemporaryRemoveAmountPowers = buildList {
            if (player.hasPower(VigorPower.POWER_ID)) {
                add(VigorPower.POWER_ID)
            }
        }
        return if (monsterTemporayPowers.isNotEmpty() || playerTemporaryRemoveAmountPowers.isNotEmpty()) {
            monster.applyTemporaryPowers(monsterTemporayPowers) {
                player.temporaryRemoveAmountFromPowers(playerTemporaryRemoveAmountPowers) {
                    createIntentAction(monster, monsterIndex, aliveMonsterCount)
                }
            }
        } else {
            baseAction
        }
    }

    player.powers.forEach { power ->
        when (power.ID) {
            DoubleTapPower.POWER_ID, DuplicationPower.POWER_ID -> {
                if (type == AbstractCard.CardType.ATTACK) {
                    actions.addToBottom(createExtraAttackAction())
                } else if (power.ID == DuplicationPower.POWER_ID) {
                    actions.addToBottom(baseAction)
                }
            }

            EchoPower.POWER_ID -> {
                val cardsDoubledThisTurn = power.getPrivateField<Int>("cardsDoubledThisTurn") ?: 0
                if (power.amount > 0 &&
                    AbstractDungeon.actionManager.cardsPlayedThisTurn.size + 1 - cardsDoubledThisTurn <= power.amount
                ) {
                    if (type == AbstractCard.CardType.ATTACK) {
                        actions.addToBottom(createExtraAttackAction())
                    } else {
                        actions.addToBottom(baseAction)
                    }
                }
            }
        }
    }

    // Apply monster debuff and player buff if needed
    actions.replacesWith { originActions ->
        originActions.map { action ->
            val monsterExtraActions = monster.powers.mapNotNull { power ->
                when (power.ID) {
                    ChokePower.POWER_ID -> Action.LoseHP(power.amount)
                    else -> null
                }
            }
            val playerExtraActions = player.powers.mapNotNull { power ->
                when (power.ID) {
                    ThousandCutsPower.POWER_ID -> Action.DamageThorns(power.amount)
                    else -> null
                }
            }
            (monsterExtraActions + playerExtraActions).let { extraActions ->
                if (extraActions.isEmpty()) {
                    listOf(action, Action.RefineStats).asGroupedAction()
                } else {
                    listOf(action).plus(extraActions).plus(Action.RefineStats).asGroupedAction()
                }
            }
        }
    }

    // Apply Panache if needed
    player.getPower(PanachePower.POWER_ID)?.let { panachePower ->
        try {
            // panache.amount is counted down from 5 (remaining actions until next extra)
            var remaining = panachePower.amount
            val panacheDamage = panachePower.getPrivateField<Int>("damage")!!
            actions.replacesWith { originActions ->
                buildList {
                    for (action in originActions) {
                        add(action)
                        remaining--
                        if (remaining == 0) {
                            // insert an extra action when countdown reaches zero, then reset to 5
                            add(Action.DamageThorns(panacheDamage))
                            remaining = PanachePower.CARD_AMT
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // ignore errors and do nothing
        }
    }
    return actions.flatten()
}

private fun AbstractCard.createIntentAction(
    monster: AbstractMonster,
    monsterIndex: Int,
    aliveMonsterCount: Int,
): Action {
    return if (type == AbstractCard.CardType.ATTACK) {
        calculateCardDamage(monster)
        val damagePerHit = getDamagePerHit(monsterIndex)
        val cardHitCount = getHitCount()
        when {
            cardHitCount > 1 -> {
                if (isRandomAttackCard && aliveMonsterCount > 1) {
                    List(cardHitCount) {
                        Action.DamageNormal(0, damagePerHit, monster)
                    }.asGroupedAction()
                } else {
                    List(cardHitCount) {
                        Action.DamageNormal(damagePerHit, monster)
                    }.asGroupedAction()
                }
            }

            cardHitCount == 1 -> {
                Action.DamageNormal(damagePerHit, monster)
            }

            else -> {
                Action.NoAction
            }
        }
    } else {
        Action.NoAction
    }
}