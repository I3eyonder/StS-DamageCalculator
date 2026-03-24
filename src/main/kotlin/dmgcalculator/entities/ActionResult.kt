package dmgcalculator.entities

data class ActionResult(
    val takenDamage: Int = 0,
    val blockedAmount: Int = 0,
    val adjustHP: Int = 0,
) {

    operator fun plus(actionResult: ActionResult): ActionResult {
        return ActionResult(
            takenDamage = takenDamage + actionResult.takenDamage,
            blockedAmount = blockedAmount + actionResult.blockedAmount,
            adjustHP = adjustHP + actionResult.adjustHP,
        )
    }

    companion object {
        val EMPTY = ActionResult()
    }
}
