package dmgcalculator.util

import com.megacrit.cardcrawl.dungeons.AbstractDungeon
import kotlin.math.min

object Utils {

    val isPlayerTurn: Boolean
        get() = AbstractDungeon.actionManager?.turnHasEnded?.not() ?: false

    fun getNetDamageAmount(rawDamageAmount: Int, blockAmount: Int): Int =
        (rawDamageAmount - blockAmount).coerceAtLeast(0)

    fun getBlockedAmount(damageAmount: Int, blockAmount: Int): Int =
        min(damageAmount, blockAmount)
}
