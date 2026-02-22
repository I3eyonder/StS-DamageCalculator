package dmgcalculator.entities

data class ExhaustInfo(
    val selfExhaust: Boolean = false,
    val exhaustInDrawPile: Int = 0,
    val drawCard: Int = 0,
    val exhaustInHand: List<SimpleCardInfo> = emptyList(),
) {
    val totalExhaust: Int
        get() = exhaustInDrawPile + exhaustInHand.size + if (selfExhaust) 1 else 0
}
