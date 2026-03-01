package dmgcalculator.util

import com.megacrit.cardcrawl.cards.AbstractCard
import com.megacrit.cardcrawl.characters.AbstractPlayer
import com.megacrit.cardcrawl.core.AbstractCreature
import com.megacrit.cardcrawl.monsters.AbstractMonster
import com.megacrit.cardcrawl.monsters.MonsterGroup
import com.megacrit.cardcrawl.orbs.Frost
import com.megacrit.cardcrawl.orbs.Lightning
import com.megacrit.cardcrawl.powers.*
import com.megacrit.cardcrawl.powers.watcher.OmegaPower
import com.megacrit.cardcrawl.relics.CloakClasp
import com.megacrit.cardcrawl.relics.Orichalcum
import com.megacrit.cardcrawl.relics.StoneCalendar
import dmgcalculator.entities.Action
import dmgcalculator.entities.ActionTarget
import dmgcalculator.util.Utils.addToBottom
import dmgcalculator.util.Utils.isAttackingIntent

fun AbstractMonster.getIntentMultiAmt(): Int {
    try {
        val value = getPrivateField<Int>("intentMultiAmt") ?: 0
        return if (value > 0) value else 1 // fallback to 1 if -1 or 0
    } catch (_: Exception) {
        return 1
    }
}

val MonsterGroup.aliveMonsters: List<AbstractMonster>
    get() = monsters.filter {
        !it.isDeadOrEscaped
    }

val MonsterGroup.aliveMonstersIndexed: List<IndexedValue<AbstractMonster>>
    get() = monsters.mapIndexedNotNull { index, monster ->
        monster.takeIf {
            !it.isDeadOrEscaped
        }?.let {
            IndexedValue(index, it)
        }
    }

fun List<AbstractMonster>.getAttackIntentActions(): List<Action> = flatMap {
    it.getAttackIntentActions()
}

fun AbstractMonster.getAttackIntentActions(): List<Action> {
    return if (intent.isAttackingIntent) {
        val hits = getIntentMultiAmt()
        val dmgPerHit = this.intentDmg
        List(hits) {
            Action.DamageNormal(dmgPerHit)
        }
    } else {
        emptyList()
    }
}

val List<AbstractMonster>.aliveMonsterNumber: Int
    get() = count {
        !it.isDeadOrEscaped
    }

fun AbstractPlayer.getHoveredMonster(): AbstractMonster? {
    return getPrivateField("hoveredMonster")
}

fun AbstractPlayer.getEndTurnIntentActions(
    aliveMonsterCount: Int,
    hoveredCard: AbstractCard?,
): List<Action> = buildList {
    // relics damage
    relics.forEach { relic ->
        when (relic.relicId) {
            StoneCalendar.ID -> {
                if (relic.counter == 7) {
                    addToBottom(Action.DamageThorns(52))
                }
            }

            Orichalcum.ID -> {
                if (currentBlock == 0 || (relic as Orichalcum).trigger)
                    if (hasPower(JuggernautPower.POWER_ID)) {
                        val juggernautPower = getPower(JuggernautPower.POWER_ID)
                        if (aliveMonsterCount > 1) {
                            addToBottom(
                                Action.DamageThorns(
                                    0,
                                    juggernautPower.amount,
                                    ActionTarget.Random
                                )
                            )
                        } else {
                            addToBottom(
                                Action.DamageThorns(
                                    juggernautPower.amount,
                                    ActionTarget.All
                                )
                            )
                        }
                    }
            }

            CloakClasp.ID -> {
                if (hand.group.isNotEmpty())
                    if (hasPower(JuggernautPower.POWER_ID)) {
                        val juggernautPower = getPower(JuggernautPower.POWER_ID)
                        if (aliveMonsterCount > 1) {
                            addToBottom(
                                Action.DamageThorns(
                                    0,
                                    juggernautPower.amount,
                                    ActionTarget.Random
                                )
                            )
                        } else {
                            addToBottom(
                                Action.DamageThorns(
                                    juggernautPower.amount,
                                    ActionTarget.All
                                )
                            )
                        }
                    }
            }
        }
    }

    // orbs damage
    val playerOrbs = if (hoveredCard?.isOrbEvokerCard == true) {
        orbs.drop(1)
    } else {
        orbs
    }
    playerOrbs.forEach { orb ->
        when (orb.ID) {
            Lightning.ORB_ID -> {
                if (hasPower(ElectroPower.POWER_ID)) {
                    addToBottom(Action.DamageThorns(orb.passiveAmount))
                } else {
                    addToBottom(Action.DamageThorns(0, orb.passiveAmount, ActionTarget.Random))
                }
            }

            Frost.ORB_ID -> {
                if (hasPower(JuggernautPower.POWER_ID)) {
                    val juggernautPower = getPower(JuggernautPower.POWER_ID)
                    if (aliveMonsterCount > 1) {
                        add(
                            Action.DamageThorns(
                                0,
                                juggernautPower.amount,
                                ActionTarget.Random,
                            )
                        )
                    } else {
                        add(
                            Action.DamageThorns(
                                juggernautPower.amount,
                                ActionTarget.All,
                            )
                        )
                    }
                }
            }
        }
    }

    // powers damage
    powers.forEach { power ->
        when {
            power.ID == CombustPower.POWER_ID ||
                    power.ID == OmegaPower.POWER_ID -> addToBottom(Action.DamageThorns(power.amount))

            power.ID.contains(TheBombPower.POWER_ID) && power.amount == 1 -> power.getPrivateField<Int>("damage")?.let {
                addToBottom(Action.DamageThorns(it))
            }
        }
    }
}

fun <T> AbstractCreature.temporaryRemoveAmountFromPowers(
    vararg powerIds: String,
    block: AbstractCreature.() -> T,
) = temporaryRemoveAmountFromPowers(powerIds.toList(), block)

fun <T> AbstractCreature.temporaryRemoveAmountFromPowers(
    powerIds: List<String>,
    block: AbstractCreature.() -> T,
): T {
    return powers.mapNotNull { power ->
        if (power.ID in powerIds) {
            val originalAmount = power.amount
            power.amount = 0
            power to originalAmount
        } else {
            null
        }
    }.let { modifiedPowers ->
        block().also {
            modifiedPowers.forEach { (power, originalAmount) ->
                power.amount = originalAmount
            }
        }
    }
}

fun <T> AbstractCreature.applyTemporaryPowers(
    vararg temporaryPowers: AbstractPower,
    block: AbstractCreature.() -> T,
): T = applyTemporaryPowers(temporaryPowers.toList(), block)

fun <T> AbstractCreature.applyTemporaryPowers(
    temporaryPowers: List<AbstractPower>,
    block: AbstractCreature.() -> T,
): T {
    val powersToRemove = mutableListOf<AbstractPower>()
    val powersToReduceAmount = mutableListOf<AbstractPower>()
    temporaryPowers.forEach { power ->
        if (!hasPower(power.ID)) {
            powersToRemove.add(power)
        } else {
            powersToReduceAmount.add(power)
        }
        addPower(power)
    }
    return block().also {
        powersToRemove.forEach { power ->
            powers.remove(power)
        }
        powersToReduceAmount.forEach { power ->
            power.amount = power.amount.unaryMinus()
            addPower(power)
        }
    }
}
