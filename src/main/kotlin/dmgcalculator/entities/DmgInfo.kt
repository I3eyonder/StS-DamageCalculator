package dmgcalculator.entities

import com.megacrit.cardcrawl.cards.DamageInfo

class DmgInfo(
    val amount: Range,
    val type: DamageInfo.DamageType,
) {

    constructor(amount: Int, type: DamageInfo.DamageType) : this(Range(amount), type)
}