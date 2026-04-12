package dmgcalculator.util

import com.megacrit.cardcrawl.cards.AbstractCard
import com.megacrit.cardcrawl.cards.AbstractCard.CardType
import com.megacrit.cardcrawl.cards.blue.*
import com.megacrit.cardcrawl.cards.colorless.*
import com.megacrit.cardcrawl.cards.green.*
import com.megacrit.cardcrawl.cards.purple.*
import com.megacrit.cardcrawl.cards.red.*
import com.megacrit.cardcrawl.cards.tempCards.Expunger
import com.megacrit.cardcrawl.core.AbstractCreature
import com.megacrit.cardcrawl.dungeons.AbstractDungeon
import com.megacrit.cardcrawl.orbs.*
import com.megacrit.cardcrawl.powers.CorruptionPower
import com.megacrit.cardcrawl.powers.DarkEmbracePower
import com.megacrit.cardcrawl.powers.PoisonPower
import com.megacrit.cardcrawl.powers.StormPower
import com.megacrit.cardcrawl.relics.BlueCandle
import com.megacrit.cardcrawl.relics.ChemicalX
import com.megacrit.cardcrawl.relics.MedicalKit
import com.megacrit.cardcrawl.ui.panels.EnergyPanel
import dmgcalculator.entities.ExhaustInfo
import dmgcalculator.entities.SimpleCardInfo

private val randomAttackCards = listOf(
    SwordBoomerang.ID,
    Ragnarok.ID,
    RipAndTear.ID,
    ThunderStrike.ID,
)

private val damageAllEnemiesCards = listOf(
    Cleave.ID,
    ThunderClap.ID,
    Whirlwind.ID,
    Immolate.ID,
    Reaper.ID,
    DaggerSpray.ID,
    AllOutAttack.ID,
    DieDieDie.ID,
    GrandFinale.ID,
    SweepingBeam.ID,
    Blizzard.ID,
    DoomAndGloom.ID,
    Hyperbeam.ID,
    Consecrate.ID,
    Conclude.ID,
    DramaticEntrance.ID,
)

private val giveVulnerableCards = listOf(
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

private val orbEvokeCards = listOf(
    Dualcast.ID,
    MultiCast.ID,
    Recursion.ID,
)

private val orbChannelCards = listOf(
    Zap.ID,
    BallLightning.ID,
    ColdSnap.ID,
    Coolheaded.ID,
    Chaos.ID,
    Chill.ID,
    Darkness.ID,
    DoomAndGloom.ID,
    Fusion.ID,
    Glacier.ID,
    Tempest.ID,
    Electrodynamics.ID,
    MeteorStrike.ID,
    Rainbow.ID,
)

private val givePoisonCards = listOf(
    DeadlyPoison.ID,
    PoisonedStab.ID,
    Catalyst.ID,
    CripplingPoison.ID,
    CorpseExplosion.ID,
    BouncingFlask.ID,
)

private val fakeGainBlockCards = listOf(
    RitualDagger.ID,
)

val AbstractCard.isOrbChannelCard: Boolean
    get() = orbChannelCards.contains(cardID)

val AbstractCard.isOrbEvokeCard: Boolean
    get() = when (cardID) {
        Fission.ID -> upgraded
        else -> orbEvokeCards.contains(cardID)
    }

val AbstractCard.isRandomAttackCard: Boolean
    get() = randomAttackCards.contains(cardID)

val AbstractCard.isDamageAllEnemiesCard: Boolean
    get() = damageAllEnemiesCards.contains(cardID)

val AbstractCard.canGiveVulnerable: Boolean
    get() = giveVulnerableCards.contains(cardID)

val AbstractCard.isDebuffCard: Boolean
    get() = applyDebuffCards.contains(cardID)

val AbstractCard.canGivePoison: Boolean
    get() = givePoisonCards.contains(cardID)

val AbstractCard.isFakeGainBlockCard: Boolean
    get() = fakeGainBlockCards.contains(cardID)

fun AbstractCard.getDebuffInstanceCount(): Int = when {
    cardID == Shockwave.ID -> 2
    cardID == Uppercut.ID -> 2
    cardID == CripplingPoison.ID -> 2
    cardID == CorpseExplosion.ID -> 2
    cardID == Malaise.ID -> 2
    cardID == BouncingFlask.ID -> magicNumber
    isDebuffCard -> 1
    else -> 0
}

fun AbstractCard.getPoisonAmount(target: AbstractCreature): Int = when (cardID) {
    DeadlyPoison.ID,
    PoisonedStab.ID,
    CripplingPoison.ID,
    CorpseExplosion.ID,
        -> magicNumber

    Catalyst.ID -> {
        target.getPower(PoisonPower.POWER_ID)?.amount?.times(
            if (upgraded) 2 else 1
        ) ?: 0
    }

    BouncingFlask.ID -> 3

    else -> 0
}

fun AbstractCard.getExhaustInfo(hand: List<SimpleCardInfo>): ExhaustInfo {
    val selfExhaust = when {
        exhaust -> true
        AbstractDungeon.player.hasPower(CorruptionPower.POWER_ID) && type == CardType.SKILL -> true
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
                it.type != CardType.ATTACK && !it.isHovered
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

fun AbstractCard.getChannelingOrbs(): List<AbstractOrb> {
    return when (cardID) {
        Zap.ID -> {
            listOf(Lightning())
        }

        BallLightning.ID -> {
            List(magicNumber) {
                Lightning()
            }
        }

        ColdSnap.ID -> {
            List(magicNumber) {
                Frost()
            }
        }

        Coolheaded.ID -> {
            listOf(Frost())
        }

        Chaos.ID -> {
            if (upgraded) {
                List(2) {
                    Plasma()
                }
            } else {
                listOf(Plasma())
            }
        }

        Chill.ID -> {
            val aliveMonsterCount = AbstractDungeon.getMonsters().aliveMonsters.size
            List(aliveMonsterCount * magicNumber) {
                Frost()
            }
        }

        Darkness.ID -> {
            listOf(Dark())
        }

        DoomAndGloom.ID -> {
            listOf(Dark())
        }

        Fusion.ID -> {
            List(magicNumber) {
                Plasma()
            }
        }

        Glacier.ID -> {
            List(magicNumber) {
                Frost()
            }
        }

        Tempest.ID -> {
            var count = EnergyPanel.totalCount
            if (AbstractDungeon.player.hasRelic(ChemicalX.ID)) {
                count += 2
            }
            List(count) {
                Lightning()
            }
        }

        Electrodynamics.ID -> {
            List(magicNumber) {
                Lightning()
            }
        }

        MeteorStrike.ID -> {
            List(magicNumber) {
                Plasma()
            }
        }

        Rainbow.ID -> {
            listOf(
                Lightning(),
                Frost(),
                Dark(),
            )
        }

        else -> emptyList()
    } + if (type == CardType.POWER && AbstractDungeon.player.hasPower(StormPower.POWER_ID)) {
        val stormPower = AbstractDungeon.player.getPower(StormPower.POWER_ID)
        List(stormPower.amount) {
            Lightning()
        }
    } else {
        emptyList()
    }
}

fun AbstractCard.getActionHitCount(): Int = when (cardID) {
    DaggerSpray.ID, Dualcast.ID, GlassKnife.ID,
    TwinStrike.ID, FlyingSleeves.ID,
        -> 2

    RiddleWithHoles.ID -> 5
    Pummel.ID, Tantrum.ID, SwordBoomerang.ID, Ragnarok.ID,
    RipAndTear.ID, Expunger.ID,
        -> magicNumber

    Whirlwind.ID, Skewer.ID, ReinforcedBody.ID,
    Tempest.ID, Transmutation.ID, MultiCast.ID,
        -> {
        var hits = EnergyPanel.totalCount
        if (AbstractDungeon.player.hasRelic(ChemicalX.ID)) {
            hits += 2
        }
        hits
    }

    Eviscerate.ID -> 3
    FiendFire.ID -> AbstractDungeon.player.hand.size() - 1
    ThunderStrike.ID -> {
        AbstractDungeon.actionManager.orbsChanneledThisCombat.count {
            it is Lightning
        }
    }

    SecondWind.ID -> {
        AbstractDungeon.player.hand.group.count {
            it.type != CardType.ATTACK && it != AbstractDungeon.player.hoveredCard
        }
    }

    Barrage.ID -> {
        AbstractDungeon.player.orbs.count {
            it !is EmptyOrbSlot
        }
    }

    Finisher.ID -> {
        AbstractDungeon.actionManager.cardsPlayedThisTurn.count {
            it.type == CardType.ATTACK
        }
    }

    Flechettes.ID -> {
        AbstractDungeon.player.hand.group.count {
            it.type == CardType.SKILL
        }
    }

    BowlingBash.ID -> {
        AbstractDungeon.getCurrRoom().monsters.aliveMonsters.size
    }

    else -> 1
}

fun AbstractCard.getDamagePerHit(monsterIndex: Int): Int = if (multiDamage != null) {
    multiDamage[monsterIndex]
} else if (baseDamage >= 0) {
    damage
} else {
    0
}

fun AbstractCard.isCardPlayable(): Boolean {
    val player = AbstractDungeon.player
    if (type == CardType.STATUS &&
        costForTurn < -1 &&
        !player.hasRelic(MedicalKit.ID)
    ) {
        return false
    }
    if (type == CardType.CURSE &&
        costForTurn < -1 &&
        !player.hasRelic(BlueCandle.ID)
    ) {
        return false
    }
    return true
}