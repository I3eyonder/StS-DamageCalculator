package dmgcalculator.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;

import dmgcalculator.publisher.CardHoverPublisher;

@SpirePatch(
        clz = AbstractPlayer.class,
        method = "updateInput"
)
public class OnCardHoveredHook {

    @SpirePostfixPatch
    public static void onUpdate(AbstractPlayer __instance) {
        AbstractCard hoveredCard = __instance.hoveredCard;
        if (hoveredCard != null) {
            CardHoverPublisher.publishOnCardHovered(hoveredCard);
        } else {
            CardHoverPublisher.publishNoCardHovering();
        }
    }
}