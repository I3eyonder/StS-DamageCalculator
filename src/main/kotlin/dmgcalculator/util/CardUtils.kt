package dmgcalculator.util

import com.megacrit.cardcrawl.cards.AbstractCard
import com.megacrit.cardcrawl.cards.green.Eviscerate
import com.megacrit.cardcrawl.cards.green.RiddleWithHoles
import com.megacrit.cardcrawl.cards.green.Skewer
import com.megacrit.cardcrawl.cards.purple.Tantrum
import com.megacrit.cardcrawl.cards.red.*
import com.megacrit.cardcrawl.cards.tempCards.Expunger
import com.megacrit.cardcrawl.dungeons.AbstractDungeon
import com.megacrit.cardcrawl.relics.ChemicalX
import com.megacrit.cardcrawl.ui.panels.EnergyPanel

private val RANDOM_ATTACK_CARDS = listOf(
    SwordBoomerang.ID
)

val AbstractCard.isRandomAttackCard: Boolean
    get() = RANDOM_ATTACK_CARDS.contains(cardID)

val AbstractCard.cardHitCount: Int
    get() = when (cardID) {
        TwinStrike.ID -> 2
        RiddleWithHoles.ID -> 5
        Pummel.ID, Tantrum.ID, SwordBoomerang.ID -> magicNumber
        Whirlwind.ID, Skewer.ID, Expunger.ID -> {
            var hits = EnergyPanel.totalCount
            if (AbstractDungeon.player.hasRelic(ChemicalX.ID)) {
                hits += 2
            }
            hits
        }

        Eviscerate.ID -> 3
        FiendFire.ID -> AbstractDungeon.player.hand.size() - 1
        else -> 1
    }


val AbstractCard.canDealDamage: Boolean
    get() = baseDamage >= 0