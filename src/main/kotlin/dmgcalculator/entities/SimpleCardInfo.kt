package dmgcalculator.entities

import com.megacrit.cardcrawl.cards.AbstractCard

data class SimpleCardInfo(
    val cardId: String,
    val type: AbstractCard.CardType,
    val isHovered: Boolean,
) {

    companion object {
        val DUMMY = SimpleCardInfo("", AbstractCard.CardType.STATUS, false)
    }
}
