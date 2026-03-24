package dmgcalculator.entities

import com.megacrit.cardcrawl.core.AbstractCreature

data class OutcomeResult(
    val creatureInfo: CreatureInfo<out AbstractCreature>,
    val actionResult: ActionResult,
)
