package dmgcalculator.util

import com.megacrit.cardcrawl.characters.AbstractPlayer
import com.megacrit.cardcrawl.core.AbstractCreature
import com.megacrit.cardcrawl.monsters.AbstractMonster
import com.megacrit.cardcrawl.monsters.AbstractMonster.Intent
import com.megacrit.cardcrawl.powers.CombustPower
import com.megacrit.cardcrawl.powers.TheBombPower
import com.megacrit.cardcrawl.powers.watcher.OmegaPower
import com.megacrit.cardcrawl.relics.StoneCalendar

fun AbstractMonster.getIntentMultiAmt(): Int {
    try {
        val value = getPrivateField<Int>("intentMultiAmt")
        return if (value > 0) value else 1 // fallback to 1 if -1 or 0
    } catch (_: Exception) {
        return 1
    }
}

val AbstractMonster.totalIntentDamage: Int
    get() {
        val hits = getIntentMultiAmt()
        val dmgPerHit = this.intentDmg
        return hits * dmgPerHit
    }

val Intent.isAttackingIntent: Boolean
    get() = this == Intent.ATTACK ||
            this == Intent.ATTACK_BUFF ||
            this == Intent.ATTACK_DEBUFF ||
            this == Intent.ATTACK_DEFEND

val List<AbstractMonster>.aliveMonsterNumber: Int
    get() = count {
        !it.isDeadOrEscaped
    }

val AbstractCreature.hasEndTurnDamage: Boolean
    get() {
        // Check creature powers
        val hasEndTurnDamagePower = powers.any { p ->
            p.ID == CombustPower.POWER_ID ||
                    p.ID == OmegaPower.POWER_ID ||
                    (p.ID.contains(TheBombPower.POWER_ID) && p.amount == 1)
        }
        if (hasEndTurnDamagePower) return true

        // Check Stone Calendar (players only)
        if (this is AbstractPlayer) {
            val relic = getRelic(StoneCalendar.ID)
            if (relic?.counter == 7) return true
        }

        return false
    }
