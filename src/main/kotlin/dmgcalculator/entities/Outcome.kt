package dmgcalculator.entities

import com.megacrit.cardcrawl.characters.AbstractPlayer
import com.megacrit.cardcrawl.powers.CurlUpPower
import com.megacrit.cardcrawl.relics.Torii
import com.megacrit.cardcrawl.relics.TungstenRod
import dmgcalculator.util.Utils.getBlockedAmount
import dmgcalculator.util.Utils.getNetDamageAmount

data class Outcome(
    var remainHP: Int = 0,
    var remainBlock: Int = 0,
    var remainBuffer: Int = 0,
    var damage: Int = 0,
    var blocked: Int = 0,
    var adjustHP: Int = 0,
    var hasCurlUpPower: Boolean = false,
) {

    val isDead: Boolean
        get() = remainHP <= 0
}

fun Outcome.apply(
    action: Action,
    useMax: Boolean,
    creatureInfo: CreatureInfo,
) {
    when (action) {
        is Action.GainHP -> {
            if (remainHP > 0) {
                adjustHP += action.value
                remainHP = (remainHP + action.value).coerceAtMost(creatureInfo.creature.maxHealth)
            }
        }

        is Action.GainBlock -> {
            remainBlock += action.value
        }

        is Action.DamageNormal,
        is Action.DamageThorns,
        is Action.LoseHP,
            -> {
            // Shared logic for DamageNormal and DamageThorns
            var damage = if (useMax) {
                action.max
            } else {
                action.min
            }
            // Apply Intangible
            if (creatureInfo.hasIntangiblePlayerPower ||
                (creatureInfo.hasIntangiblePower && action is Action.DamageNormal)
            ) {
                damage = damage.coerceAtMost(1)
            }

            if (action !is Action.LoseHP) {
                // Apply Block
                val blocked = getBlockedAmount(damage, remainBlock)
                val netDamage = getNetDamageAmount(damage, remainBlock)
                this.blocked += blocked
                remainBlock = (remainBlock - blocked).coerceAtLeast(0)
                damage = netDamage
            }
            // Apply Buffer
            if (damage > 0 && remainBuffer > 0) {
                remainBuffer--
                damage = 0
            }

            // Apply Relics
            if (creatureInfo.creature is AbstractPlayer) {
                creatureInfo.creature.relics.forEach { relic ->
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

            // Apply Curl Up power
            if (damage > 0 && action is Action.DamageNormal && hasCurlUpPower) {
                val curlUpPower = creatureInfo.creature.getPower(CurlUpPower.POWER_ID)
                remainBlock += curlUpPower.amount
                hasCurlUpPower = false
            }

            if (action !is Action.LoseHP) {
                this.damage += damage
            } else {
                adjustHP -= damage
            }
            remainHP = (remainHP - damage).coerceAtLeast(0)
        }

        else -> {
            // No action
        }
    }
}