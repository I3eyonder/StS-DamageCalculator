package dmgcalculator.entities

import com.megacrit.cardcrawl.core.AbstractCreature

sealed class Action(open val min: Int, open val max: Int, open val target: ActionTarget) {

    private val tags = mutableSetOf<String>()

    fun hasTag(tag: String) = tags.contains(tag)

    fun addTags(vararg tags: String) {
        this.tags.addAll(tags)
    }

    fun removeTags(vararg tags: String) {
        this.tags.removeAll(tags.toSet())
    }

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
        constructor(value: Int, target: AbstractCreature, filterable: Boolean = false) : this(
            value,
            ActionTarget.Single(target, filterable)
        )
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

    companion object {
        const val TAG_PENDING = "Action.Pending"
    }
}

sealed interface ActionTarget {
    data class Single(val target: AbstractCreature, val filterable: Boolean = true) : ActionTarget
    data object Random : ActionTarget
    data object All : ActionTarget
    data object None : ActionTarget
}

fun Action.withTags(vararg tags: String): Action = apply {
    addTags(*tags)
}

fun Action.withPendingTag(): Action = withTags(Action.TAG_PENDING)

fun Action.withoutPendingTag(): Action = apply {
    removeTags(Action.TAG_PENDING)
}

fun Action.hasPendingTag(): Boolean = hasTag(Action.TAG_PENDING)

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