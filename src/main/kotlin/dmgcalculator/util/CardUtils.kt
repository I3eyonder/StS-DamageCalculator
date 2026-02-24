package dmgcalculator.util

import com.megacrit.cardcrawl.cards.AbstractCard
import com.megacrit.cardcrawl.cards.blue.*
import com.megacrit.cardcrawl.cards.colorless.*
import com.megacrit.cardcrawl.cards.green.*
import com.megacrit.cardcrawl.cards.purple.*
import com.megacrit.cardcrawl.cards.red.*
import com.megacrit.cardcrawl.cards.tempCards.Expunger
import com.megacrit.cardcrawl.dungeons.AbstractDungeon
import com.megacrit.cardcrawl.orbs.Lightning
import com.megacrit.cardcrawl.powers.CorruptionPower
import com.megacrit.cardcrawl.powers.DarkEmbracePower
import com.megacrit.cardcrawl.relics.ChemicalX
import com.megacrit.cardcrawl.ui.panels.EnergyPanel
import dmgcalculator.entities.ExhaustInfo
import dmgcalculator.entities.SimpleCardInfo

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

fun AbstractCard.getExhaustInfo(hand: List<SimpleCardInfo>): ExhaustInfo {
    val selfExhaust = when {
        exhaust -> true
        AbstractDungeon.player.hasPower(CorruptionPower.POWER_ID) && type == AbstractCard.CardType.SKILL -> true
        else -> false
    }
    return when (cardID) {
        Havoc.ID, Omniscience.ID -> ExhaustInfo(
            selfExhaust = selfExhaust,
            exhaustInDrawPile = 1,
        )

        Purity.ID -> ExhaustInfo(
            selfExhaust = selfExhaust,
            exhaustInHand = hand.filterNot {
                it.isHovered
            }.take(3)
        )

        Recycle.ID, TrueGrit.ID -> ExhaustInfo(
            selfExhaust = selfExhaust,
            exhaustInHand = hand.filterNot {
                it.isHovered
            }.take(1),
        )

        BurningPact.ID -> ExhaustInfo(
            selfExhaust = selfExhaust,
            exhaustInHand = hand.filterNot {
                it.isHovered
            }.take(1),
            drawCard = magicNumber,
        )

        SecondWind.ID, SeverSoul.ID -> ExhaustInfo(
            selfExhaust = selfExhaust,
            exhaustInHand = hand.filter {
                it.type != AbstractCard.CardType.ATTACK && !it.isHovered
            },
        )

        FiendFire.ID -> ExhaustInfo(
            selfExhaust = selfExhaust,
            exhaustInHand = hand.filterNot {
                it.isHovered
            },
        )

        else -> ExhaustInfo(selfExhaust = selfExhaust)
    }.let {
        if (AbstractDungeon.player.hasPower(DarkEmbracePower.POWER_ID)) {
            val darkEmbracePower = AbstractDungeon.player.getPower(DarkEmbracePower.POWER_ID)
            it.copy(
                drawCard = it.drawCard + it.totalExhaust * darkEmbracePower.amount,
            )
        } else {
            it
        }
    }
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
    SecondWind.ID -> {
        AbstractDungeon.player.hand.group.count {
            it.type != AbstractCard.CardType.ATTACK && it != AbstractDungeon.player.hoveredCard
        }
    }

    else -> 1
}

fun AbstractCard.getDamagePerHit(monsterIndex: Int): Int = if (multiDamage != null) {
    multiDamage[monsterIndex]
} else {
    damage
}