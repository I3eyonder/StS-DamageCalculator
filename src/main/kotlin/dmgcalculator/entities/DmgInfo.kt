package dmgcalculator.entities

import com.megacrit.cardcrawl.cards.DamageInfo

data class DmgInfo(
    var amount: Int,
    val type: DamageInfo.DamageType,
)