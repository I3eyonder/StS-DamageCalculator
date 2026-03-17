package dmgcalculator.util

import com.megacrit.cardcrawl.characters.AbstractPlayer
import com.megacrit.cardcrawl.core.AbstractCreature
import com.megacrit.cardcrawl.monsters.AbstractMonster
import com.megacrit.cardcrawl.monsters.MonsterGroup
import com.megacrit.cardcrawl.powers.AbstractPower
import dmgcalculator.entities.Action
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
