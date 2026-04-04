package dmgcalculator.entities


import com.megacrit.cardcrawl.characters.AbstractPlayer
import com.megacrit.cardcrawl.core.AbstractCreature
import com.megacrit.cardcrawl.dungeons.AbstractDungeon
import com.megacrit.cardcrawl.monsters.AbstractMonster
import com.megacrit.cardcrawl.monsters.beyond.Nemesis
import com.megacrit.cardcrawl.powers.*
import com.megacrit.cardcrawl.relics.*
import dmgcalculator.util.Utils.addToTop
import dmgcalculator.util.Utils.getBlockedAmount
import dmgcalculator.util.Utils.getNetDamageAmount

data class CreatureInfo<T : AbstractCreature>(
    val creature: T,
    var remainHP: Int = creature.currentHealth,
    var remainBlock: Int = creature.currentBlock,
    val powersAmount: MutableMap<String, Int> = creature.powers.associate {
        it.ID to it.amount
    }.toMutableMap(),
) {

    private val refineActions = mutableListOf<Action>()

    val pendingActions = mutableListOf<Action>()

    val creatureId: String
        get() = creature.id

    val maxHP: Int
        get() = creature.maxHealth

    val isDead: Boolean
        get() = remainHP <= 0

    val isPlayer: Boolean
        get() = creature.isPlayer

    fun hasPower(powerId: String): Boolean {
        return powersAmount.contains(powerId)
    }

    fun getPowerAmount(powerId: String): Int {
        return powersAmount[powerId] ?: -1
    }

    fun reducePowerAmount(powerId: String, amountToReduce: Int) {
        powersAmount[powerId] = getPowerAmount(powerId).minus(amountToReduce).coerceAtLeast(-1)
    }

    fun increasePowerAmount(powerId: String, amountToIncrease: Int) {
        powersAmount[powerId] = getPowerAmount(powerId).plus(amountToIncrease)
    }

    fun setPowerAmount(powerId: String, newAmount: Int) {
        powersAmount[powerId] = newAmount
    }

    fun takeActions(actions: List<Action>, useMax: Boolean): ActionResult {
        return actions.fold(ActionResult.EMPTY) { acc, action ->
            acc + takeAction(action, useMax)
        }
    }

    fun takeAction(action: Action, useMax: Boolean): ActionResult {
        if (action.hasPendingTag()) {
            pendingActions.add(action.makeCopy().withoutPendingTag())
            return ActionResult.EMPTY
        }
        return when (action) {
            is Action.GainHP -> {
                if (!isDead) {
                    remainHP = (remainHP + action.value).coerceAtMost(maxHP)
                    ActionResult(adjustHP = action.value)
                } else {
                    ActionResult.EMPTY
                }
            }

            is Action.GainBlock -> {
                remainBlock += action.value
                ActionResult.EMPTY
            }

            Action.RefineStats -> {
                refineActions.toList().let { actions ->
                    refineActions.clear()
                    if (actions.isNotEmpty()) {
                        takeActions(actions, useMax)
                    } else {
                        ActionResult.EMPTY
                    }
                }
            }

            is Action.DamageNormal,
            is Action.DamageThorns,
            is Action.LoseHP,
                -> {
                var blockedAmount = 0
                var damage = if (useMax) {
                    action.max
                } else {
                    action.min
                }
                // Apply Intangible
                if (hasPower(IntangiblePlayerPower.POWER_ID) ||
                    (hasPower(IntangiblePower.POWER_ID) && creatureId == Nemesis.ID)
                ) {
                    damage = damage.coerceAtMost(1)
                }

                if (action !is Action.LoseHP) {
                    // Apply Block
                    val blocked = getBlockedAmount(damage, remainBlock)
                    val netDamage = getNetDamageAmount(damage, remainBlock)
                    blockedAmount += blocked
                    remainBlock = (remainBlock - blocked).coerceAtLeast(0)
                    damage = netDamage
                }
                // Apply Buffer
                if (damage > 0 && getPowerAmount(BufferPower.POWER_ID) > 0) {
                    reducePowerAmount(BufferPower.POWER_ID, 1)
                    damage = 0
                }

                // Apply Relics
                if (creature is AbstractPlayer) {
                    creature.relics.forEach { relic ->
                        when (relic.relicId) {
                            TungstenRod.ID ->
                                damage = (damage - 1).coerceAtLeast(0)

                            Torii.ID -> {
                                if (action !is Action.LoseHP && action !is Action.DamageThorns && damage in 2..5) {
                                    damage = 1
                                }
                            }
                        }
                    }
                }

                if (creature is AbstractMonster) {
                    AbstractDungeon.player.relics.forEach { relic ->
                        when (relic.relicId) {
                            Boot.ID -> {
                                if (action !is Action.LoseHP && action !is Action.DamageThorns && damage in 1..4) {
                                    damage = 5
                                }
                            }

                            HandDrill.ID -> {
                                if (blockedAmount > 0 && remainBlock == 0) {
                                    // Armor broken
                                    if (AbstractDungeon.player.hasPower(SadisticPower.POWER_ID)) {
                                        val sadisticPower = AbstractDungeon.player.getPower(SadisticPower.POWER_ID)
                                        refineActions.add(Action.DamageThorns(sadisticPower.amount, action.target))
                                    }
                                }
                            }
                        }
                    }
                }

                // Apply Invincible power
                val invincibleAmount = getPowerAmount(InvinciblePower.POWER_ID)
                if (invincibleAmount >= 0) {
                    damage = damage.coerceAtMost(invincibleAmount)
                    setPowerAmount(InvinciblePower.POWER_ID, (invincibleAmount - damage).coerceAtLeast(0))
                }

                if (damage > 0 && action is Action.DamageNormal) {
                    // Apply Curl Up power
                    val curlUpPowerAmount = getPowerAmount(CurlUpPower.POWER_ID)
                    if (curlUpPowerAmount > 0) {
                        refineActions.addToTop(Action.GainBlock(curlUpPowerAmount, action.target))
                        setPowerAmount(CurlUpPower.POWER_ID, -1)
                    }

                    if (creature is AbstractMonster) {
                        // Apply EnvenomPower
                        AbstractDungeon.player.getPower(EnvenomPower.POWER_ID)?.let { envenomPower ->
                            var poisonAmount = envenomPower.amount
                            if (AbstractDungeon.player.hasRelic(SneckoSkull.ID)) {
                                poisonAmount += 1
                            }
                            ifDebuffApplied {
                                refineActions.add(
                                    Action.LoseHP(poisonAmount, action.target).withPendingTag()
                                )
                                if (action.hasTag(ActionTag.Bane)) {
                                    refineActions.addToTop(action.copy())
                                }
                                AbstractDungeon.player.getPower(SadisticPower.POWER_ID)?.let { sadisticPower ->
                                    refineActions.add(Action.DamageThorns(sadisticPower.amount, action.target))
                                }
                            }
                        }
                    }

                    // Apply MalleablePower
                    val malleableAmount = getPowerAmount(MalleablePower.POWER_ID)
                    if (malleableAmount >= 0 && damage < remainHP) {
                        refineActions.add(Action.GainBlock(malleableAmount, action.target))
                        increasePowerAmount(MalleablePower.POWER_ID, 1)
                    }
                    refineActions.add(Action.RefineStats)
                }

                remainHP = (remainHP - damage).coerceAtLeast(0)
                if (action !is Action.LoseHP) {
                    ActionResult(
                        takenDamage = damage,
                        blockedAmount = blockedAmount,
                    )
                } else {
                    ActionResult(
                        adjustHP = -damage,
                        blockedAmount = blockedAmount,
                    )
                }
            }

            else -> {
                ActionResult.EMPTY
            }
        }
    }
}

fun CreatureInfo<out AbstractCreature>.ifDebuffApplied(block: () -> Unit) {
    if (getPowerAmount(ArtifactPower.POWER_ID) <= 0) {
        block()
    } else {
        reducePowerAmount(ArtifactPower.POWER_ID, 1)
    }
}
