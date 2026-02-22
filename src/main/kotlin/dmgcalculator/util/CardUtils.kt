package dmgcalculator.util

import com.megacrit.cardcrawl.cards.AbstractCard
import com.megacrit.cardcrawl.cards.blue.BeamCell
import com.megacrit.cardcrawl.cards.blue.GoForTheEyes
import com.megacrit.cardcrawl.cards.blue.LockOn
import com.megacrit.cardcrawl.cards.blue.MultiCast
import com.megacrit.cardcrawl.cards.blue.ReinforcedBody
import com.megacrit.cardcrawl.cards.blue.RipAndTear
import com.megacrit.cardcrawl.cards.blue.Tempest
import com.megacrit.cardcrawl.cards.blue.ThunderStrike
import com.megacrit.cardcrawl.cards.colorless.Blind
import com.megacrit.cardcrawl.cards.colorless.DarkShackles
import com.megacrit.cardcrawl.cards.colorless.Transmutation
import com.megacrit.cardcrawl.cards.colorless.Trip
import com.megacrit.cardcrawl.cards.green.*
import com.megacrit.cardcrawl.cards.purple.Collect
import com.megacrit.cardcrawl.cards.purple.CrushJoints
import com.megacrit.cardcrawl.cards.purple.Indignation
import com.megacrit.cardcrawl.cards.purple.PressurePoints
import com.megacrit.cardcrawl.cards.purple.Ragnarok
import com.megacrit.cardcrawl.cards.purple.SashWhip
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
    ThunderStrike.ID,
)

private val giveVulnearableCards = listOf(
    Bash.ID,
    ThunderClap.ID,
    Shockwave.ID,
    Uppercut.ID,
    Terror.ID,
    BeamCell.ID,
    CrushJoints.ID,
    Indignation.ID,
)

private val applyDebuffCards = listOf(
    Blind.ID,
    DarkShackles.ID,
    Trip.ID,
    Bash.ID,
    Clothesline.ID,
    ThunderClap.ID,
    Disarm.ID,
    Intimidate.ID,
    Shockwave.ID,
    Uppercut.ID,
    Neutralize.ID,
    DeadlyPoison.ID,
    PiercingWail.ID,
    PoisonedStab.ID,
    SuckerPunch.ID,
    BouncingFlask.ID,
    Choke.ID,
    CripplingPoison.ID,
    LegSweep.ID,
    Terror.ID,
    CorpseExplosion.ID,
    Malaise.ID,
    BeamCell.ID,
    GoForTheEyes.ID,
    LockOn.ID,
    CrushJoints.ID,
    PressurePoints.ID,
    SashWhip.ID,
    Indignation.ID,
)

val AbstractCard.isRandomAttackCard: Boolean
    get() = randomAttackCards.contains(cardID)

val AbstractCard.canGiveVulnearable: Boolean
    get() = giveVulnearableCards.contains(cardID)

val AbstractCard.isDebuffCard: Boolean
    get() = applyDebuffCards.contains(cardID)

fun AbstractCard.getDebuffInstanceCount(): Int = when {
    cardID == Shockwave.ID -> 2
    cardID == Uppercut.ID -> 2
    cardID == CripplingPoison.ID -> 2
    cardID == CorpseExplosion.ID -> 2
    cardID == Malaise.ID -> 2
    cardID == BouncingFlask.ID -> 3
    isDebuffCard -> 1
    else -> 0
}

fun AbstractCard.getActionHitCount(): Int = when (cardID) {
    TwinStrike.ID -> 2
    RiddleWithHoles.ID -> 5
    Pummel.ID, Tantrum.ID, SwordBoomerang.ID, Ragnarok.ID,
    RipAndTear.ID, Expunger.ID,
        -> magicNumber

    Whirlwind.ID, Skewer.ID, ReinforcedBody.ID,
    Tempest.ID, Transmutation.ID,
        -> {
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