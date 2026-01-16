package dmgcalculator.util

import com.megacrit.cardcrawl.actions.GameActionManager
import com.megacrit.cardcrawl.dungeons.AbstractDungeon
import com.megacrit.cardcrawl.monsters.AbstractMonster.Intent
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
}
