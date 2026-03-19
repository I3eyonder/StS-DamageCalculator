package dmgcalculator.entities

import com.megacrit.cardcrawl.core.AbstractCreature

sealed class Action(open val min: Int, open val max: Int, open val target: ActionTarget) {

    data class GroupedAction(val actions: List<Action>) : Action(0, 0, ActionTarget.None)

    data class DamageNormal(
        override val min: Int,
        override val max: Int,
        override val target: ActionTarget,
    ) : Action(min, max, target) {
        constructor(min: Int, max: Int, target: AbstractCreature) : this(min, max, ActionTarget.Single(target))
        constructor(value: Int, target: ActionTarget = ActionTarget.None) : this(value, value, target)
        constructor(value: Int, target: AbstractCreature) : this(value, value, ActionTarget.Single(target))
    }

    data class DamageThorns(
        override val min: Int,
        override val max: Int,
        override val target: ActionTarget = ActionTarget.All,
    ) : Action(min, max, target) {
        constructor(value: Int, target: ActionTarget = ActionTarget.All) : this(value, value, target)
    }

    data class LoseHP(
        val value: Int,
        override val target: ActionTarget,
    ) : Action(value, value, target) {
        constructor(value: Int, target: AbstractCreature) : this(value, ActionTarget.Single(target, false))
    }

    data class StackPoison(
        val value: Int,
        override val target: ActionTarget,
    ) : Action(value, value, target) {
        constructor(value: Int, target: AbstractCreature) : this(value, ActionTarget.Single(target, true))
    }

    data class GainHP(
        val value: Int,
        override val target: ActionTarget,
    ) : Action(value, value, target) {
        constructor(value: Int, target: AbstractCreature) : this(value, ActionTarget.Single(target, false))
    }

    data class GainBlock(
        val value: Int,
        override val target: ActionTarget,
    ) : Action(value, value, target) {
        constructor(value: Int, target: AbstractCreature) : this(value, ActionTarget.Single(target, false))
    }

    data object RefineStats : Action(0, 0, ActionTarget.None)
    data object NoAction : Action(0, 0, ActionTarget.None)
}

sealed interface ActionTarget {
    data class Single(val target: AbstractCreature, val filterable: Boolean = true) : ActionTarget
    data object Random : ActionTarget
    data object All : ActionTarget
    data object None : ActionTarget
}

fun List<Action>.asGroupedAction(): Action.GroupedAction {
    return Action.GroupedAction(this)
}

fun List<Action>.flatten(): List<Action> = buildList {
    this@flatten.forEach { action ->
        when (action) {
            is Action.GroupedAction -> addAll(action.actions.flatten())
            else -> add(action)
        }
    }
}

fun List<Action>.calculateWorstOutcome(creatureInfo: CreatureInfo): Outcome {
    return Outcome(
        remainHP = creatureInfo.remainHP,
        remainBlock = creatureInfo.remainBlock,
        remainBuffer = creatureInfo.remainBuffer,
        hasCurlUpPower = creatureInfo.hasCurlUpPower,
        invincibleAmount = creatureInfo.invincibleAmount,
        malleableAmount = creatureInfo.malleableAmount,
    ).also {
        forEach { action ->
            it.apply(action, false, creatureInfo)
        }
    }
}

fun List<Action>.calculateBestOutcome(creatureInfo: CreatureInfo): Outcome {
    return Outcome(
        remainHP = creatureInfo.remainHP,
        remainBlock = creatureInfo.remainBlock,
        remainBuffer = creatureInfo.remainBuffer,
        hasCurlUpPower = creatureInfo.hasCurlUpPower,
        invincibleAmount = creatureInfo.invincibleAmount,
        malleableAmount = creatureInfo.malleableAmount,
    ).also {
        forEach { action ->
            it.apply(action, true, creatureInfo)
        }
    }
}

fun List<Action>.calculateOutcome(creatureInfo: CreatureInfo): Pair<Outcome, Outcome> {
    val actions = filterNot { action -> action is Action.NoAction }
    return actions.calculateWorstOutcome(creatureInfo) to actions.calculateBestOutcome(creatureInfo)
}