package dmgcalculator.entities

import com.megacrit.cardcrawl.core.AbstractCreature

sealed class Action(
    open val min: Int,
    open val max: Int,
    open val target: ActionTarget,
    open val triggerCondition: (CreatureInfo<AbstractCreature>) -> Boolean = { true },
) {

    private val _tags = mutableSetOf<ActionTag>()

    val tags: Set<ActionTag>
        get() = _tags.toSet()

    fun hasTag(tag: ActionTag) = _tags.contains(tag)

    fun addTags(tags: Set<ActionTag>) {
        this._tags.addAll(tags)
    }

    fun addTags(vararg tags: ActionTag) {
        this._tags.addAll(tags)
    }

    fun removeTags(vararg tags: ActionTag) {
        this._tags.removeAll(tags.toSet())
    }

    fun makeCopy(): Action {
        return makeCopyWithoutTags().apply {
            addTags(this@Action.tags)
        }
    }

    abstract fun makeCopyWithoutTags(): Action

    data class GroupedAction(val actions: List<Action>) : Action(0, 0, ActionTarget.None) {

        override fun makeCopyWithoutTags() = GroupedAction(actions)
    }

    data class DamageNormal(
        override val min: Int,
        override val max: Int,
        override val target: ActionTarget,
        override val triggerCondition: (CreatureInfo<AbstractCreature>) -> Boolean = { true },
    ) : Action(min, max, target, triggerCondition) {
        constructor(
            min: Int,
            max: Int,
            target: AbstractCreature,
            triggerCondition: (CreatureInfo<AbstractCreature>) -> Boolean = { true },
        ) : this(min, max, ActionTarget.Single(target), triggerCondition)

        constructor(
            value: Int,
            target: ActionTarget = ActionTarget.None,
            triggerCondition: (CreatureInfo<AbstractCreature>) -> Boolean = { true },
        ) : this(value, value, target, triggerCondition)

        constructor(
            value: Int,
            target: AbstractCreature,
            triggerCondition: (CreatureInfo<AbstractCreature>) -> Boolean = { true },
        ) : this(value, value, ActionTarget.Single(target), triggerCondition)

        override fun makeCopyWithoutTags(): Action = copy()
    }

    data class DamageThorns(
        override val min: Int,
        override val max: Int,
        override val target: ActionTarget = ActionTarget.All,
        override val triggerCondition: (CreatureInfo<AbstractCreature>) -> Boolean = { true },
    ) : Action(min, max, target, triggerCondition) {
        constructor(
            value: Int,
            target: ActionTarget = ActionTarget.All,
            triggerCondition: (CreatureInfo<AbstractCreature>) -> Boolean = { true },
        ) : this(value, value, target, triggerCondition)

        override fun makeCopyWithoutTags(): Action = copy()
    }

    data class LoseHP(
        override val min: Int,
        override val max: Int,
        override val target: ActionTarget,
        override val triggerCondition: (CreatureInfo<AbstractCreature>) -> Boolean = { true },
    ) : Action(min, max, target, triggerCondition) {
        constructor(
            value: Int,
            target: ActionTarget,
            triggerCondition: (CreatureInfo<AbstractCreature>) -> Boolean = { true },
        ) : this(value, value, target, triggerCondition)

        constructor(
            value: Int,
            target: AbstractCreature, filterable: Boolean = false,
            triggerCondition: (CreatureInfo<AbstractCreature>) -> Boolean = { true },
        ) : this(value, value, ActionTarget.Single(target, filterable), triggerCondition)

        override fun makeCopyWithoutTags(): Action = copy()
    }

    data class GainHP(
        val value: Int,
        override val target: ActionTarget,
        override val triggerCondition: (CreatureInfo<AbstractCreature>) -> Boolean = { true },
    ) : Action(value, value, target, triggerCondition) {
        constructor(
            value: Int,
            target: AbstractCreature,
            triggerCondition: (CreatureInfo<AbstractCreature>) -> Boolean = { true },
        ) : this(value, ActionTarget.Single(target, false), triggerCondition)

        override fun makeCopyWithoutTags(): Action = copy()
    }

    data class GainBlock(
        val value: Int,
        override val target: ActionTarget,
        override val triggerCondition: (CreatureInfo<AbstractCreature>) -> Boolean = { true },
    ) : Action(value, value, target, triggerCondition) {
        constructor(
            value: Int,
            target: AbstractCreature,
            triggerCondition: (CreatureInfo<AbstractCreature>) -> Boolean = { true },
        ) : this(value, ActionTarget.Single(target, false), triggerCondition)

        override fun makeCopyWithoutTags(): Action = copy()
    }

    data object RefineStats : Action(0, 0, ActionTarget.None) {

        override fun makeCopyWithoutTags(): Action = RefineStats
    }

    data object NoAction : Action(0, 0, ActionTarget.None) {

        override fun makeCopyWithoutTags(): Action = NoAction
    }
}

sealed interface ActionTarget {
    data class Single(val target: AbstractCreature, val filterable: Boolean = true) : ActionTarget
    data object Random : ActionTarget
    data object All : ActionTarget
    data object None : ActionTarget
}

sealed interface ActionTag {
    data object Bane : ActionTag
    data object Pending : ActionTag
}

fun Action.withTags(vararg tags: ActionTag): Action = apply {
    addTags(*tags)
}

fun Action.withPendingTag(): Action = withTags(ActionTag.Pending)

fun Action.withoutPendingTag(): Action = apply {
    removeTags(ActionTag.Pending)
}

fun Action.hasPendingTag(): Boolean = hasTag(ActionTag.Pending)

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