package dmgcalculator.util

import com.megacrit.cardcrawl.monsters.AbstractMonster.Intent
import kotlin.math.min

object Utils {

    fun getNetDamageAmount(rawDamageAmount: Int, blockAmount: Int): Int =
        (rawDamageAmount - blockAmount).coerceAtLeast(0)

    fun getBlockedAmount(damageAmount: Int, blockAmount: Int): Int =
        min(damageAmount, blockAmount)

    val Intent.isAttackingIntent: Boolean
        get() = this == Intent.ATTACK ||
                this == Intent.ATTACK_BUFF ||
                this == Intent.ATTACK_DEBUFF ||
                this == Intent.ATTACK_DEFEND

    fun <T> MutableList<T>.addToTop(element: T) {
        this.add(0, element)
    }

    fun <T> MutableList<T>.addToBottom(element: T) {
        this.add(element)
    }
}
