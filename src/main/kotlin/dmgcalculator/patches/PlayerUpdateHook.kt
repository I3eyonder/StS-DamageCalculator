package dmgcalculator.patches

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch
import com.megacrit.cardcrawl.characters.AbstractPlayer
import dmgcalculator.publisher.CardHoverPublisher
import dmgcalculator.publisher.PlayerEndTurnPublisher

@SpirePatch(clz = AbstractPlayer::class, method = "update")
object PlayerUpdateHook {

    @JvmStatic
    @SpirePostfixPatch
    fun onUpdate(player: AbstractPlayer) {
        val hoveredCard = player.hoveredCard
        if (hoveredCard != null) {
            CardHoverPublisher.publishCardHovered(hoveredCard)
        } else {
            CardHoverPublisher.publishCardUnHovered()
        }
        if (player.endTurnQueued) {
            PlayerEndTurnPublisher.publish()
        }
    }
}