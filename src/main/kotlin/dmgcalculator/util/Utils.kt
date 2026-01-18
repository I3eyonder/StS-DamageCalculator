package dmgcalculator.util

import com.megacrit.cardcrawl.actions.GameActionManager
import com.megacrit.cardcrawl.cards.DamageInfo
import com.megacrit.cardcrawl.characters.AbstractPlayer
import com.megacrit.cardcrawl.core.AbstractCreature
import com.megacrit.cardcrawl.dungeons.AbstractDungeon
import com.megacrit.cardcrawl.monsters.AbstractMonster.Intent
import com.megacrit.cardcrawl.powers.BufferPower
import com.megacrit.cardcrawl.powers.IntangiblePlayerPower
import com.megacrit.cardcrawl.relics.Torii
import com.megacrit.cardcrawl.relics.TungstenRod
import dmgcalculator.entities.CalculatedOutcome
import dmgcalculator.entities.DmgInfo
import kotlin.math.min

object Utils {

    val isPlayerTurn: Boolean
        get() = AbstractDungeon.actionManager?.run {
            phase == GameActionManager.Phase.WAITING_ON_USER &&
                    !turnHasEnded
        } ?: false

    fun getNetDamageAmount(rawDamageAmount: Int, blockAmount: Int): Int =
        (rawDamageAmount - blockAmount).coerceAtLeast(0)

    fun getBlockedAmount(damageAmount: Int, blockAmount: Int): Int =
        min(damageAmount, blockAmount)

    val Intent.isAttackingIntent: Boolean
        get() = this == Intent.ATTACK ||
                this == Intent.ATTACK_BUFF ||
                this == Intent.ATTACK_DEBUFF ||
                this == Intent.ATTACK_DEFEND

    fun List<DmgInfo>.calculateDamage(target: AbstractCreature): CalculatedOutcome {
        val calculatedOutcome = CalculatedOutcome(
            target.currentHealth,
            target.currentBlock
        )

        val hasIntangiblePlayerPower = target.hasPower(IntangiblePlayerPower.POWER_ID)
        var bufferAmount = target.getPower(BufferPower.POWER_ID)?.amount ?: 0
        forEach { dmgInfo ->
            // Apply Intangible
            if (hasIntangiblePlayerPower) {
                dmgInfo.amount = dmgInfo.amount.coerceAtMost(1)
            }

            if (dmgInfo.type != DamageInfo.DamageType.HP_LOSS) {
                // Apply Block
                val blocked = Utils.getBlockedAmount(dmgInfo.amount, calculatedOutcome.remainBlockAmount)
                val netDamage = Utils.getNetDamageAmount(dmgInfo.amount, calculatedOutcome.remainBlockAmount)
                calculatedOutcome.remainBlockAmount -= blocked
                dmgInfo.amount = netDamage
            }
            // Apply Buffer
            if (dmgInfo.amount > 0 && bufferAmount > 0) {
                bufferAmount--
                dmgInfo.amount = 0
            }

            // Apply Relics
            if (target is AbstractPlayer) {
                target.relics.forEach { relic ->
                    when (relic.relicId) {
                        TungstenRod.ID ->
                            dmgInfo.amount = (dmgInfo.amount - 1).coerceAtLeast(0)

                        Torii.ID -> {
                            if (dmgInfo.type !in listOf(
                                    DamageInfo.DamageType.HP_LOSS,
                                    DamageInfo.DamageType.THORNS
                                ) && dmgInfo.amount in 2..5
                            ) {
                                dmgInfo.amount = 1
                            }
                        }
                    }
                }
            }

            calculatedOutcome.apply {
                if (dmgInfo.type != DamageInfo.DamageType.HP_LOSS) {
                    damageAmount += dmgInfo.amount
                } else {
                    adjustHPAmount += dmgInfo.amount.unaryMinus()
                }
            }
        }
        return calculatedOutcome
    }
}
