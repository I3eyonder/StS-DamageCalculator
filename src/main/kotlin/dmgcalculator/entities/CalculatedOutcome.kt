package dmgcalculator.entities

data class CalculatedOutcome(
    val baseHPAmount: Int,
    val baseBlockAmount: Int,
    var damageAmount: Int,
    var adjustHPAmount: Int,
    var remainBlockAmount: Int = baseBlockAmount,
) {

    val remainHPAmount: Int
        get() = baseHPAmount - damageAmount + adjustHPAmount

    val blockedAmount: Int
        get() = baseBlockAmount - remainBlockAmount
}
