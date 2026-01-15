package dmgcalculator.publisher

import com.megacrit.cardcrawl.cards.AbstractCard
import dmgcalculator.interfaces.CardHoverSubscriber

object CardHoverPublisher {

    private val cardHoverSubscribers = mutableListOf<CardHoverSubscriber>()

    private var hoveredCard: AbstractCard? = null

    fun publishCardHovered(card: AbstractCard) {
        if (hoveredCard !== card) {
            hoveredCard = card
            cardHoverSubscribers.forEach {
                it.onCardHovered(card)
            }
        }
    }

    fun publishCardUnHovered() {
        if (hoveredCard != null) {
            hoveredCard = null
            cardHoverSubscribers.forEach {
                it.onCardUnHovered()
            }
        }
    }

    fun subscribe(subscriber: CardHoverSubscriber) {
        cardHoverSubscribers.add(subscriber)
    }
}
