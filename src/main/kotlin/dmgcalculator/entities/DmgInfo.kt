package dmgcalculator.entities

import com.megacrit.cardcrawl.cards.DamageInfo

data class DmgInfo(
    val base: Int,
    val type: DamageInfo.DamageType,
    var amount: Int = base,
)
