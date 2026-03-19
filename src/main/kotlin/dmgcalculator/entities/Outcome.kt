package dmgcalculator.entities

import com.megacrit.cardcrawl.characters.AbstractPlayer
import com.megacrit.cardcrawl.dungeons.AbstractDungeon
import com.megacrit.cardcrawl.monsters.AbstractMonster
import com.megacrit.cardcrawl.powers.CurlUpPower
import com.megacrit.cardcrawl.powers.EnvenomPower
import com.megacrit.cardcrawl.powers.SadisticPower
import com.megacrit.cardcrawl.relics.Boot
import com.megacrit.cardcrawl.relics.HandDrill
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
    var pendingGainBlock: Int = 0,
    var invincibleAmount: Int = -1,
    var malleableAmount: Int = -1,
) {

    val isDead: Boolean
        get() = remainHP <= 0
}

fun Outcome.apply(
    action: Action,
    useMax: Boolean,
    creatureInfo: CreatureInfo,
) {
    val player = AbstractDungeon.player
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

        Action.RefineStats -> {
            if (pendingGainBlock > 0) {
                remainBlock += pendingGainBlock
                pendingGainBlock = 0
            }
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

            if (creatureInfo.creature is AbstractMonster) {
                AbstractDungeon.player.relics.forEach { relic ->
                    when (relic.relicId) {
                        Boot.ID -> {
                            if (action !is Action.LoseHP && action !is Action.DamageThorns && damage in 1..4) {
                                damage = 5
                            }
                        }

                        HandDrill.ID -> {
                            if (blocked > 0 && remainBlock == 0) {
                                // Ammor broken
                                if (AbstractDungeon.player.hasPower(SadisticPower.POWER_ID)) {
                                    val sadisticPower = AbstractDungeon.player.getPower(SadisticPower.POWER_ID)
                                    damage += sadisticPower.amount
                                }
                            }
                        }
                    }
                }
            }

            // Apply Invincible power
            if (damage > 0 && invincibleAmount >= 0) {
                damage = damage.coerceAtMost(invincibleAmount)
                invincibleAmount = (invincibleAmount - damage).coerceAtLeast(0)
            }

            if (damage > 0 && action is Action.DamageNormal) {
                // Apply Curl Up power
                if (hasCurlUpPower) {
                    val curlUpPower = creatureInfo.creature.getPower(CurlUpPower.POWER_ID)
                    pendingGainBlock += curlUpPower.amount
                    hasCurlUpPower = false
                }

                if (creatureInfo.creature is AbstractMonster) {
                    // Apply EnvenomPower
                    player.getPower(EnvenomPower.POWER_ID)?.let { envenomPower ->
                        player.getPower(SadisticPower.POWER_ID)?.let { sadisticPower ->
                            damage += sadisticPower.amount
                        }
                    }
                }

                // Apply MalleablePower
                if (malleableAmount >= 0 && damage < remainHP) {
                    pendingGainBlock += malleableAmount++
                }
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