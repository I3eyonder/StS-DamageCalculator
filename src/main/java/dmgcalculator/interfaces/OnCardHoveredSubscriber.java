package dmgcalculator.interfaces;

import com.megacrit.cardcrawl.cards.AbstractCard;

public interface OnCardHoveredSubscriber {
    void onCardHovered(AbstractCard card);
    void onNoCardHovering();
}
