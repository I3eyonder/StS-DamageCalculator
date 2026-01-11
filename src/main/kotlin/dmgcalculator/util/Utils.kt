package dmgcalculator.util

import com.megacrit.cardcrawl.monsters.AbstractMonster.Intent
import kotlin.math.min

object Utils {

    fun getNetDamageAmount(rawDamageAmount: Int, blockAmount: Int): Int =
        (rawDamageAmount - blockAmount).coerceAtLeast(0)

    fun getBlockedAmount(damageAmount: Int, blockAmount: Int): Int =
        min(damageAmount, blockAmount)

    val Intent.isAttackingIntent: Boolean
        get() = this in listOf(
            Intent.ATTACK,
            Intent.ATTACK_BUFF,
            Intent.ATTACK_DEBUFF,
            Intent.ATTACK_DEFEND
        )

    fun <T> MutableList<T>.addToTop(element: T) {
        this.add(0, element)
    }

    fun <T> MutableList<T>.addToBottom(element: T) {
        this.add(element)
    }

    fun <T> MutableList<T>.replacesWith(listBuilder: (List<T>) -> List<T>) {
        listBuilder(this@replacesWith).also {
            clear()
            addAll(it)
        }
    }

    fun <T> MutableList<T>.replacesWith(list: List<T>) {
        clear()
        addAll(list)
    }
}
