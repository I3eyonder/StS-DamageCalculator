package dmgcalculator.patches

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch
import com.megacrit.cardcrawl.characters.AbstractPlayer
import dmgcalculator.publisher.CardHoverPublisher.publishCardUnHovered
import dmgcalculator.publisher.CardHoverPublisher.publishCardHovered

@SpirePatch(clz = AbstractPlayer::class, method = "updateInput")
object OnCardHoveredHook {

    @JvmStatic
    @SpirePostfixPatch
    fun onUpdate(__instance: AbstractPlayer) {
        val hoveredCard = __instance.hoveredCard
        if (hoveredCard != null) {
            publishCardHovered(hoveredCard)
        } else {
            publishCardUnHovered()
        }
    }
}