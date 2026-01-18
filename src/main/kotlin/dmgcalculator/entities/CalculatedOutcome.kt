package dmgcalculator.entities

class CalculatedOutcome(baseHP: Int, baseBlock: Int) {

    val baseHPAmount = Range(baseHP)
    val baseBlockAmount = Range(baseBlock)
    val damageAmount = Range()
    val adjustHPAmount = Range()
    val remainBlockAmount = Range(baseBlock)

    val remainHPAmount: Range
        get() = baseHPAmount - damageAmount + adjustHPAmount

    val blockedAmount: Range
        get() = baseBlockAmount - remainBlockAmount
}
