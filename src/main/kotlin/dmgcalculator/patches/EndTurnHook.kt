package dmgcalculator.patches

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch
import com.megacrit.cardcrawl.actions.GameActionManager
import dmgcalculator.publisher.PlayerEndTurnPublisher

@SpirePatch(clz = GameActionManager::class, method = "callEndOfTurnActions")
object EndTurnHook {

    @JvmStatic
    @SpirePrefixPatch
    fun onUpdate(gameActionManager: GameActionManager) {
        PlayerEndTurnPublisher.publish()
    }
}