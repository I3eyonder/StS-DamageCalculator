package dmgcalculator.entities

sealed class Action(open val min: Int, open val max: Int) {

    data class DamageNormal(override val min: Int, override val max: Int) : Action(min, max) {
        constructor(value: Int) : this(value, value)
    }

    data class DamageThorns(override val min: Int, override val max: Int) : Action(min, max) {

        constructor(value: Int) : this(value, value)
    }

    data class LoseHP(val value: Int) : Action(value, value)
    data class GainHP(val value: Int) : Action(value, value)
    data class GainBlock(val value: Int) : Action(value, value)
}

fun List<Action>.calculateWorstOutcome(creatureInfo: CreatureInfo): Outcome {
    return Outcome(
        creatureInfo.remainHP,
        creatureInfo.remainBlock,
        creatureInfo.remainBuffer,
    ).also {
        forEach { action ->
            it.apply(action, false, creatureInfo)
        }
    }
}

fun List<Action>.calculateBestOutcome(creatureInfo: CreatureInfo): Outcome {
    return Outcome(
        creatureInfo.remainHP,
        creatureInfo.remainBlock,
        creatureInfo.remainBuffer,
    ).also {
        forEach { action ->
            it.apply(action, true, creatureInfo)
        }
    }
}

fun List<Action>.calculateOutcome(creatureInfo: CreatureInfo): Pair<Outcome, Outcome> {
    return calculateWorstOutcome(creatureInfo) to calculateBestOutcome(creatureInfo)
}