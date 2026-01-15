package dmgcalculator.interfaces

import com.megacrit.cardcrawl.cards.AbstractCard

interface CardHoverSubscriber {
    fun onCardHovered(card: AbstractCard)
    fun onCardUnHovered()
}
