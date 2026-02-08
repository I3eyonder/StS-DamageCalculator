package dmgcalculator.util

import com.megacrit.cardcrawl.cards.AbstractCard
import com.megacrit.cardcrawl.cards.blue.BeamCell
import com.megacrit.cardcrawl.cards.blue.RipAndTear
import com.megacrit.cardcrawl.cards.blue.ThunderStrike
import com.megacrit.cardcrawl.cards.green.*
import com.megacrit.cardcrawl.cards.purple.CrushJoints
import com.megacrit.cardcrawl.cards.purple.Indignation
import com.megacrit.cardcrawl.cards.purple.Ragnarok
import com.megacrit.cardcrawl.cards.purple.Tantrum
import com.megacrit.cardcrawl.cards.red.*
import com.megacrit.cardcrawl.cards.tempCards.Expunger
import com.megacrit.cardcrawl.dungeons.AbstractDungeon
import com.megacrit.cardcrawl.orbs.Lightning
import com.megacrit.cardcrawl.relics.ChemicalX
import com.megacrit.cardcrawl.ui.panels.EnergyPanel

private val randomAttackCards = listOf(
    SwordBoomerang.ID,
    Ragnarok.ID,
    RipAndTear.ID,
    ThunderStrike.ID
)

private val giveVulnearableCards = listOf(
    Bash.ID,
    ThunderClap.ID,
    Shockwave.ID,
    Uppercut.ID,
    Terror.ID,
    BeamCell.ID,
    CrushJoints.ID,
    Indignation.ID
)

val AbstractCard.isRandomAttackCard: Boolean
    get() = randomAttackCards.contains(cardID)

val AbstractCard.canGiveVulnearable: Boolean
    get() = giveVulnearableCards.contains(cardID)

fun AbstractCard.getHitCount(): Int = when (cardID) {
    TwinStrike.ID -> 2
    RiddleWithHoles.ID -> 5
    Pummel.ID, Tantrum.ID, SwordBoomerang.ID, Ragnarok.ID, RipAndTear.ID -> magicNumber
    Whirlwind.ID, Skewer.ID, Expunger.ID -> {
        var hits = EnergyPanel.totalCount
        if (AbstractDungeon.player.hasRelic(ChemicalX.ID)) {
            hits += 2
        }
        hits
    }

    Eviscerate.ID -> 3
    FiendFire.ID -> AbstractDungeon.player.hand.size() - 1
    DaggerSpray.ID -> 2
    ThunderStrike.ID -> {
        AbstractDungeon.actionManager.orbsChanneledThisCombat.count {
            it is Lightning
        }
    }

    else -> 1
}

fun AbstractCard.getDamagePerHit(monsterIndex: Int): Int = if (multiDamage != null) {
    multiDamage[monsterIndex]
} else {
    damage
}