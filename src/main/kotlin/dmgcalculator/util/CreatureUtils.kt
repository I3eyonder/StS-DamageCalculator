package dmgcalculator.util

import com.megacrit.cardcrawl.cards.DamageInfo
import com.megacrit.cardcrawl.characters.AbstractPlayer
import com.megacrit.cardcrawl.core.AbstractCreature
import com.megacrit.cardcrawl.monsters.AbstractMonster
import com.megacrit.cardcrawl.monsters.MonsterGroup
import com.megacrit.cardcrawl.powers.CombustPower
import com.megacrit.cardcrawl.powers.TheBombPower
import com.megacrit.cardcrawl.powers.watcher.OmegaPower
import com.megacrit.cardcrawl.relics.StoneCalendar
import dmgcalculator.entities.DmgInfo
import dmgcalculator.entities.Range
import dmgcalculator.entities.asRange
import dmgcalculator.util.Utils.isAttackingIntent

fun AbstractMonster.getIntentMultiAmt(): Int {
    try {
        val value = getPrivateField<Int>("intentMultiAmt")
        return if (value > 0) value else 1 // fallback to 1 if -1 or 0
    } catch (_: Exception) {
        return 1
    }
}

val MonsterGroup.aliveMonsters: List<AbstractMonster>
    get() = monsters.filter {
        !it.isDeadOrEscaped
    }

fun List<AbstractMonster>.getIntentDamages(): List<DmgInfo> = flatMap {
    it.getIntentDamages()
}

fun AbstractMonster.getIntentDamages(): List<DmgInfo> {
    return if (intent.isAttackingIntent) {
        val hits = getIntentMultiAmt()
        val dmgPerHit = this.intentDmg
        List(hits) {
            DmgInfo(
                dmgPerHit.asRange(),
                DamageInfo.DamageType.NORMAL
            )
        }
    } else {
        listOf()
    }
}

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
