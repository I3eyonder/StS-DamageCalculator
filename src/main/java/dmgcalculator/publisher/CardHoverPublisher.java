package dmgcalculator.publisher;

import com.megacrit.cardcrawl.cards.AbstractCard;

import java.util.ArrayList;
import java.util.List;

import dmgcalculator.interfaces.OnCardHoveredSubscriber;

public class CardHoverPublisher {

    private static final List<OnCardHoveredSubscriber> onCardHoveredSubscribers = new ArrayList<>();

    private static AbstractCard hoveringCard;

    public static void publishOnCardHovered(AbstractCard card) {
        if (hoveringCard != card) {
            hoveringCard = card;
            onCardHoveredSubscribers.forEach(subscriber -> {
                subscriber.onCardHovered(card);
            });
        }
    }

    public static void publishNoCardHovering() {
        if (hoveringCard != null) {
            hoveringCard = null;
            onCardHoveredSubscribers.forEach(OnCardHoveredSubscriber::onNoCardHovering);
        }
    }

    public static void subscribe(OnCardHoveredSubscriber subscriber) {
        onCardHoveredSubscribers.add(subscriber);
    }
}
