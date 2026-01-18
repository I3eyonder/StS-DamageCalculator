package dmgcalculator.entities

class CalculatedOutcome(
    val baseHPAmount: Range,
    val baseBlockAmount: Range,
) {

    var damageAmount = Range()
    var adjustHPAmount = Range()
    var remainBlockAmount = baseBlockAmount.copy()

    val remainHPAmount: Range
        get() = baseHPAmount - damageAmount + adjustHPAmount

    val blockedAmount: Range
        get() = baseBlockAmount - remainBlockAmount
}
