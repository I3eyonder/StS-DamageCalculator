package dmgcalculator.renderer

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.megacrit.cardcrawl.cards.AbstractCard
import com.megacrit.cardcrawl.cards.AbstractCard.CardType
import com.megacrit.cardcrawl.cards.blue.GoForTheEyes
import com.megacrit.cardcrawl.cards.green.Bane
import com.megacrit.cardcrawl.cards.green.BouncingFlask
import com.megacrit.cardcrawl.cards.purple.CrushJoints
import com.megacrit.cardcrawl.cards.purple.Indignation
import com.megacrit.cardcrawl.cards.purple.PressurePoints
import com.megacrit.cardcrawl.cards.purple.SashWhip
import com.megacrit.cardcrawl.characters.AbstractPlayer
import com.megacrit.cardcrawl.dungeons.AbstractDungeon
import com.megacrit.cardcrawl.monsters.AbstractMonster
import com.megacrit.cardcrawl.orbs.*
import com.megacrit.cardcrawl.powers.*
import com.megacrit.cardcrawl.powers.watcher.MarkPower
import com.megacrit.cardcrawl.powers.watcher.OmegaPower
import com.megacrit.cardcrawl.powers.watcher.VigorPower
import com.megacrit.cardcrawl.powers.watcher.WaveOfTheHandPower
import com.megacrit.cardcrawl.relics.*
import com.megacrit.cardcrawl.stances.WrathStance
import dmgcalculator.entities.*
import dmgcalculator.util.*
import dmgcalculator.util.Utils.addToBottom
import dmgcalculator.util.Utils.replacesWith
import dmgcalculator.util.Utils.toSimpleCardInfoList

object MonsterRenderer {

    fun render(sb: SpriteBatch, hoveredCard: AbstractCard?) {
        val player = AbstractDungeon.player
        val hoveredMonster = player.getHoveredMonster()
        val aliveMonstersIndexed = AbstractDungeon.getMonsters().aliveMonstersIndexed
        val aliveMonsterCount = aliveMonstersIndexed.size
        val msgBuilder = StringBuilder()

        aliveMonstersIndexed.forEach { (index, monster) ->
            msgBuilder.clear()
            val creatureInfo = CreatureInfo(monster)
            val cardActions = hoveredCard?.getIntentActions(monster, index, aliveMonsterCount)?.run {
                if (hoveredMonster != null) {
                    this.filter { action ->
                        action.target.let { target ->
                            if (target is ActionTarget.Single && target.filterable) {
                                target.target == hoveredMonster
                            } else {
                                true
                            }
                        }
                    }
                } else {
                    this
                }
            }
            val (worstCardOutcome, bestCardOutcome) = cardActions?.calculateOutcome(creatureInfo) ?: (null to null)
            val endTurnIntentActions =
                monster.getEndTurnIntentActions(player, aliveMonsterCount, hoveredCard, cardActions)
            if (worstCardOutcome != null && bestCardOutcome != null) {
                val showCardRemainHP = bestCardOutcome.isDead || endTurnIntentActions.isEmpty()
                msgBuilder.buildOutcomeMessage(
                    worstOutcome = worstCardOutcome,
                    bestOutcome = bestCardOutcome,
                    showRemainHP = showCardRemainHP
                )
                if (!showCardRemainHP) {
                    msgBuilder.append("\n")
                        .append("--End Turn--".colored("#FCBA03"))
                        .append("\n")
                    val worstEndTurnCalculatedOutcome = endTurnIntentActions.calculateWorstOutcome(
                        creatureInfo.copy(
                            remainHP = worstCardOutcome.remainHP,
                            remainBlock = worstCardOutcome.remainBlock,
                            remainBuffer = worstCardOutcome.remainBuffer,
                            hasCurlUpPower = worstCardOutcome.hasCurlUpPower,
                            invincibleAmount = worstCardOutcome.invincibleAmount,
                            malleableAmount = worstCardOutcome.malleableAmount,
                        )
                    )
                    val bestEndTurnCalculatedOutcome = endTurnIntentActions.calculateBestOutcome(
                        creatureInfo.copy(
                            remainHP = bestCardOutcome.remainHP,
                            remainBlock = bestCardOutcome.remainBlock,
                            remainBuffer = bestCardOutcome.remainBuffer,
                            hasCurlUpPower = bestCardOutcome.hasCurlUpPower,
                            invincibleAmount = bestCardOutcome.invincibleAmount,
                            malleableAmount = bestCardOutcome.malleableAmount,
                        )
                    )
                    msgBuilder.buildOutcomeMessage(
                        worstOutcome = worstEndTurnCalculatedOutcome,
                        bestOutcome = bestEndTurnCalculatedOutcome,
                    )
                }
            } else if (endTurnIntentActions.isNotEmpty()) {
                msgBuilder.append("--End Turn--".colored("#FCBA03"))
                    .append("\n")
                val (worstEndTurnOutcome, bestEndTurnOutcome) = endTurnIntentActions.calculateOutcome(
                    creatureInfo
                )
                msgBuilder.buildOutcomeMessage(
                    worstOutcome = worstEndTurnOutcome,
                    bestOutcome = bestEndTurnOutcome,
                )
            }
            val msg = msgBuilder.toString()
            if (msg.isNotEmpty()) {
                sb.renderFixedSizeMessage(
                    msgBuilder.toString(),
                    monster.hb.cX,
                    monster.hb.cY + monster.hb.height / 2 + 125f,
                )
            }
        }
    }

    private fun AbstractMonster.getEndTurnIntentActions(
        player: AbstractPlayer,
        aliveMonsterCount: Int,
        hoveredCard: AbstractCard?,
        cardActions: List<Action>?,
    ): List<Action> = buildList {
        // player relics damage
        player.relics.forEach { relic ->
            when (relic.relicId) {
                StoneCalendar.ID -> {
                    if (relic.counter == 7) {
                        addToBottom(Action.DamageThorns(52))
                    }
                }

                Orichalcum.ID -> {
                    if (player.currentBlock == 0 || (relic as Orichalcum).trigger)
                        if (player.hasPower(JuggernautPower.POWER_ID)) {
                            val juggernautPower = player.getPower(JuggernautPower.POWER_ID)
                            if (aliveMonsterCount > 1) {
                                addToBottom(
                                    Action.DamageThorns(
                                        0,
                                        juggernautPower.amount,
                                        ActionTarget.Random
                                    )
                                )
                            } else {
                                addToBottom(
                                    Action.DamageThorns(
                                        juggernautPower.amount,
                                        ActionTarget.All
                                    )
                                )
                            }
                        }
                }

                CloakClasp.ID -> {
                    if (player.hand.group.isNotEmpty())
                        if (player.hasPower(JuggernautPower.POWER_ID)) {
                            val juggernautPower = player.getPower(JuggernautPower.POWER_ID)
                            if (aliveMonsterCount > 1) {
                                addToBottom(
                                    Action.DamageThorns(
                                        0,
                                        juggernautPower.amount,
                                        ActionTarget.Random
                                    )
                                )
                            } else {
                                addToBottom(
                                    Action.DamageThorns(
                                        juggernautPower.amount,
                                        ActionTarget.All
                                    )
                                )
                            }
                        }
                }
            }
        }

        // player orbs damage
        val playerOrbs = player.orbs.plus(hoveredCard?.getChannelingOrbs().orEmpty()).filterNot {
            it is EmptyOrbSlot
        }.takeLast(player.maxOrbs).let {
            if (hoveredCard?.isOrbEvokeCard == true) {
                it.drop(1)
            } else {
                it
            }
        }
        val orbDamageMultiplier = if (hasPower(LockOnPower.POWER_ID)) 1.5f else 1f
        playerOrbs.forEach { orb ->
            when (orb.ID) {
                Lightning.ORB_ID -> {
                    val damageAmount = orb.passiveAmount.times(orbDamageMultiplier).toInt()
                    if (player.hasPower(ElectroPower.POWER_ID)) {
                        addToBottom(Action.DamageThorns(damageAmount))
                    } else {
                        addToBottom(Action.DamageThorns(0, damageAmount, ActionTarget.Random))
                    }
                }

                Frost.ORB_ID -> {
                    if (player.hasPower(JuggernautPower.POWER_ID)) {
                        val juggernautPower = player.getPower(JuggernautPower.POWER_ID)
                        if (aliveMonsterCount > 1) {
                            add(
                                Action.DamageThorns(
                                    0,
                                    juggernautPower.amount,
                                    ActionTarget.Random,
                                )
                            )
                        } else {
                            add(
                                Action.DamageThorns(
                                    juggernautPower.amount,
                                    ActionTarget.All,
                                )
                            )
                        }
                    }
                }
            }
        }

        // player powers damage
        player.powers.forEach { power ->
            when {
                power.ID == CombustPower.POWER_ID ||
                        power.ID == OmegaPower.POWER_ID -> addToBottom(Action.DamageThorns(power.amount))

                power.ID.contains(TheBombPower.POWER_ID) && power.amount == 1 -> power.getPrivateField<Int>("damage")
                    ?.let {
                        addToBottom(Action.DamageThorns(it))
                    }
            }
        }

        // monster poison damage
        cardActions?.forEach {
            if (it is Action.StackPoison) {
                addToBottom(Action.LoseHP(it.value, it.target))
            }
        }
        if (hasPower(PoisonPower.POWER_ID)) {
            val poisonPower = getPower(PoisonPower.POWER_ID)
            addToBottom(Action.LoseHP(poisonPower.amount, this@getEndTurnIntentActions))
        }

    }

    private fun AbstractCard.getIntentActions(
        monster: AbstractMonster,
        monsterIndex: Int,
        aliveMonsterCount: Int,
    ): List<Action> {
        val player = AbstractDungeon.player
        val baseAction = createIntentAction(monster, monsterIndex, aliveMonsterCount)
        val actions = mutableListOf(baseAction)

        // Apply powers that modify action here if needed
        fun createDuplicationAttackAction(): Action {
            val monsterTemporaryPowers = buildList {
                if (canGiveVulnerable && !monster.hasPower(VulnerablePower.POWER_ID)) {
                    add(VulnerablePower(monster, 1, false))
                }
            }
            val playerTemporaryRemoveAmountPowers = buildList {
                if (player.hasPower(VigorPower.POWER_ID)) {
                    add(VigorPower.POWER_ID)
                }
            }
            return if (monsterTemporaryPowers.isNotEmpty() || playerTemporaryRemoveAmountPowers.isNotEmpty()) {
                monster.applyTemporaryPowers(monsterTemporaryPowers) {
                    player.temporaryRemoveAmountFromPowers(playerTemporaryRemoveAmountPowers) {
                        createIntentAction(monster, monsterIndex, aliveMonsterCount)
                    }
                }
            } else {
                baseAction
            }
        }

        player.powers.forEach { power ->
            when (power.ID) {
                DoubleTapPower.POWER_ID, DuplicationPower.POWER_ID -> {
                    if (type == CardType.ATTACK) {
                        actions.addToBottom(createDuplicationAttackAction())
                    } else if (power.ID == DuplicationPower.POWER_ID) {
                        actions.addToBottom(baseAction)
                    }
                }

                EchoPower.POWER_ID -> {
                    val cardsDoubledThisTurn = power.getPrivateField<Int>("cardsDoubledThisTurn") ?: 0
                    if (power.amount > 0 &&
                        AbstractDungeon.actionManager.cardsPlayedThisTurn.size + 1 - cardsDoubledThisTurn <= power.amount
                    ) {
                        if (type == CardType.ATTACK) {
                            actions.addToBottom(createDuplicationAttackAction())
                        } else {
                            actions.addToBottom(baseAction)
                        }
                    }
                }
            }
        }

        // Apply Necronomicon relic if needed
        player.getRelic(Necronomicon.ID)?.let { necronomiconRelic ->
            if (type == CardType.ATTACK &&
                (costForTurn >= 2 && !freeToPlayOnce || cost == -1 && energyOnUse >= 2) &&
                necronomiconRelic.checkTrigger()
            ) {
                actions.addToBottom(createDuplicationAttackAction())
            }
        }

        // Apply Charon's Ashes relic if needed
        player.getRelic(CharonsAshes.ID)?.let { charonsAshesRelic ->
            actions.replacesWith { originActions ->
                var hand = player.hand.group.toSimpleCardInfoList()
                originActions.mapIndexed { index, action ->
                    val exhaustInfo = getExhaustInfo(hand)
                    listOf(action)
                        .plus(
                            if (exhaustInfo.selfExhaust && index == 0) {
                                Action.DamageThorns(CharonsAshes.DMG)
                            } else {
                                Action.NoAction
                            }
                        )
                        .plus(
                            List(exhaustInfo.exhaustInHand.size + exhaustInfo.exhaustInDrawPile) {
                                Action.DamageThorns(CharonsAshes.DMG)
                            }
                        )
                        .asGroupedAction()
                        .also {
                            hand = hand.filterNot {
                                it.isHovered || exhaustInfo.exhaustInHand.contains(it)
                            } + List(exhaustInfo.drawCard) {
                                SimpleCardInfo.DUMMY
                            }
                        }
                }
            }
        }

        // Apply LetterOpener relic if needed
        player.getRelic(LetterOpener.ID)?.let { letterOpenerRelic ->
            var counter = letterOpenerRelic.counter
            if (type == CardType.SKILL) {
                actions.replacesWith { originActions ->
                    originActions.map { action ->
                        counter++
                        if (counter % 3 == 0) {
                            counter = 0
                            // insert an extra action when countdown reaches zero, then reset to 5
                            listOf(action, Action.DamageThorns(5)).asGroupedAction()
                        } else {
                            action
                        }
                    }
                }
            }
        }

        // Apply monster debuff and player buff if needed
        actions.replacesWith { originActions ->
            var hand = player.hand.group.toSimpleCardInfoList()
            originActions.mapIndexed { index, action ->
                val monsterExtraActions = monster.powers.mapNotNull { power ->
                    when (power.ID) {
                        ChokePower.POWER_ID -> Action.LoseHP(power.amount, monster)
                        else -> null
                    }
                }
                val playerExtraActions = player.powers.mapNotNull { power ->
                    when (power.ID) {
                        ThousandCutsPower.POWER_ID -> Action.DamageThorns(power.amount)
                        JuggernautPower.POWER_ID -> {
                            if (player.hasPower(FeelNoPainPower.POWER_ID)) {
                                val exhaustInfo = getExhaustInfo(hand)
                                emptyList<Action>()
                                    .plus(
                                        if (exhaustInfo.selfExhaust && index == 0) {
                                            if (aliveMonsterCount > 1) {
                                                Action.DamageThorns(
                                                    0,
                                                    power.amount,
                                                    ActionTarget.Random
                                                )
                                            } else {
                                                Action.DamageThorns(
                                                    power.amount,
                                                    ActionTarget.Single(monster)
                                                )
                                            }
                                        } else {
                                            Action.NoAction
                                        }
                                    )
                                    .plus(
                                        List(exhaustInfo.exhaustInHand.size + exhaustInfo.exhaustInDrawPile) {
                                            if (aliveMonsterCount > 1) {
                                                Action.DamageThorns(
                                                    0,
                                                    power.amount,
                                                    ActionTarget.Random
                                                )
                                            } else {
                                                Action.DamageThorns(
                                                    power.amount,
                                                    ActionTarget.Single(monster)
                                                )
                                            }
                                        }
                                    )
                                    .asGroupedAction()
                                    .also {
                                        hand = hand.filterNot {
                                            it.isHovered || exhaustInfo.exhaustInHand.contains(it)
                                        }.plus(
                                            List(exhaustInfo.drawCard) {
                                                SimpleCardInfo.DUMMY
                                            }
                                        ).take(10)
                                    }
                            } else {
                                null
                            }
                        }

                        else -> null
                    }
                }
                (monsterExtraActions + playerExtraActions).let { extraActions ->
                    if (extraActions.isEmpty()) {
                        listOf(action, Action.RefineStats).asGroupedAction()
                    } else {
                        listOf(action).plus(extraActions).plus(Action.RefineStats).asGroupedAction()
                    }
                }
            }
        }

        // Apply Panache if needed
        player.getPower(PanachePower.POWER_ID)?.let { panachePower ->
            try {
                // panache.amount is counted down from 5 (remaining actions until next extra)
                var remaining = panachePower.amount
                val panacheDamage = panachePower.getPrivateField<Int>("damage")!!
                actions.replacesWith { originActions ->
                    originActions.map { action ->
                        remaining--
                        if (remaining == 0) {
                            remaining = PanachePower.CARD_AMT
                            // insert an extra action when countdown reaches zero, then reset to 5
                            listOf(action, Action.DamageThorns(panacheDamage)).asGroupedAction()
                        } else {
                            action
                        }
                    }
                }
            } catch (_: Exception) {
                // ignore errors and do nothing
            }
        }
        return actions.flatten()
    }

    private fun AbstractCard.createIntentAction(
        monster: AbstractMonster,
        monsterIndex: Int,
        aliveMonsterCount: Int,
    ): Action {
        val player = AbstractDungeon.player
        calculateCardDamage(monster)
        val cardHitCount = getActionHitCount()
        val damagePerHit = getDamagePerHit(monsterIndex)
        val orbDamageMultiplier = if (monster.hasPower(LockOnPower.POWER_ID)) 1.5f else 1f
        fun MutableList<Action>.addOrbEvokeAction(orbsToEvoke: AbstractOrb) {
            when (orbsToEvoke.ID) {
                Lightning.ORB_ID -> {
                    val damageAmount = orbsToEvoke.evokeAmount.times(orbDamageMultiplier).toInt()
                    if (player.hasPower(ElectroPower.POWER_ID)) {
                        add(Action.DamageThorns(damageAmount))
                    } else {
                        add(Action.DamageThorns(0, damageAmount, ActionTarget.Random))
                    }
                }

                Frost.ORB_ID -> {
                    if (player.hasPower(JuggernautPower.POWER_ID)) {
                        val juggernautPower = player.getPower(JuggernautPower.POWER_ID)
                        if (aliveMonsterCount > 1) {
                            add(
                                Action.DamageThorns(
                                    0,
                                    juggernautPower.amount,
                                    ActionTarget.Random
                                )
                            )
                        } else {
                            add(
                                Action.DamageThorns(
                                    juggernautPower.amount,
                                    ActionTarget.Single(monster)
                                )
                            )
                        }
                    }
                }

                Dark.ORB_ID -> {
                    val lowestHPMonster = AbstractDungeon.getMonsters().aliveMonsters.minBy {
                        it.currentHealth
                    }
                    val damageAmount = orbsToEvoke.evokeAmount.times(orbDamageMultiplier).toInt()
                    add(Action.DamageThorns(damageAmount, ActionTarget.Single(lowestHPMonster)))
                }
            }
        }

        var monsterArtifactPowerAmount = monster.getPower(ArtifactPower.POWER_ID)?.amount ?: 0

        return List(cardHitCount) {
            buildList {
                if (damagePerHit > 0) {
                    if (isRandomAttackCard && aliveMonsterCount > 1) {
                        add(Action.DamageNormal(0, damagePerHit, ActionTarget.Random))
                    } else {
                        add(Action.DamageNormal(damagePerHit, monster))
                        if (cardID == Bane.ID && monster.hasPower(PoisonPower.POWER_ID)) {
                            add(Action.DamageNormal(damagePerHit, monster))
                        }
                    }
                }

                // Apply MarkPower if needed
                if (cardID == PressurePoints.ID) {
                    monster.getPower(MarkPower.POWER_ID)?.let { markPower ->
                        add(Action.LoseHP(markPower.amount, monster))
                    }
                    add(Action.LoseHP(magicNumber, ActionTarget.Single(monster)))
                }

                // Apply player's Juggernaut power if needed
                player.getPower(JuggernautPower.POWER_ID)?.let { juggernautPower ->
                    if (type == CardType.ATTACK && player.hasPower(RagePower.POWER_ID)) {
                        if (aliveMonsterCount > 1) {
                            add(
                                Action.DamageThorns(
                                    0,
                                    juggernautPower.amount,
                                    ActionTarget.Random
                                )
                            )
                        } else {
                            add(
                                Action.DamageThorns(
                                    juggernautPower.amount,
                                    ActionTarget.Single(monster)
                                )
                            )
                        }
                    }
                    if (player.hasPower(AfterImagePower.POWER_ID)) {
                        if (aliveMonsterCount > 1) {
                            add(
                                Action.DamageThorns(
                                    0,
                                    juggernautPower.amount,
                                    ActionTarget.Random
                                )
                            )
                        } else {
                            add(
                                Action.DamageThorns(
                                    juggernautPower.amount,
                                    ActionTarget.Single(monster)
                                )
                            )
                        }
                    }
                    if (block > 0 && !isFakeGainBlockCard) {
                        if (aliveMonsterCount > 1) {
                            add(
                                Action.DamageThorns(
                                    0,
                                    juggernautPower.amount,
                                    ActionTarget.Random
                                )
                            )
                        } else {
                            add(
                                Action.DamageThorns(
                                    juggernautPower.amount,
                                    ActionTarget.Single(monster)
                                )
                            )
                        }
                    }
                }

                // Apply player's Sadistic power if needed
                player.getPower(SadisticPower.POWER_ID)?.let { sadisticPower ->
                    if (isDebuffCard) {
                        val shouldTrigger = when (cardID) {
                            GoForTheEyes.ID -> monster.intentBaseDmg >= 0
                            CrushJoints.ID -> AbstractDungeon.actionManager.cardsPlayedThisCombat.lastOrNull()?.type == CardType.SKILL
                            SashWhip.ID -> AbstractDungeon.actionManager.cardsPlayedThisCombat.lastOrNull()?.type == CardType.ATTACK
                            Indignation.ID -> player.stance.ID == WrathStance.STANCE_ID

                            else -> !canGivePoison
                        }
                        if (shouldTrigger) {
                            repeat(getDebuffInstanceCount()) {
                                if (monsterArtifactPowerAmount-- <= 0) {
                                    add(Action.DamageThorns(sadisticPower.amount, ActionTarget.Single(monster)))
                                }
                            }
                        }
                    }
                    if (player.hasPower(WaveOfTheHandPower.POWER_ID)) {
                        if (type == CardType.ATTACK && player.hasPower(RagePower.POWER_ID)) {
                            if (monsterArtifactPowerAmount-- <= 0) {
                                add(Action.DamageThorns(sadisticPower.amount, ActionTarget.All))
                            }
                        }
                        if (block > 0) {
                            if (monsterArtifactPowerAmount-- <= 0) {
                                add(Action.DamageThorns(sadisticPower.amount, ActionTarget.All))
                            }
                        }
                    }
                }

                // Evoke orbs if needed
                player.orbs.plus(getChannelingOrbs()).filterNot {
                    it is EmptyOrbSlot
                }.dropLast(player.maxOrbs)
                    .forEach { evokedOrb ->
                        addOrbEvokeAction(evokedOrb)
                    }
                if (isOrbEvokeCard) {
                    player.orbs.firstOrNull()?.let { orbsToEvoke ->
                        addOrbEvokeAction(orbsToEvoke)
                    }
                }

                // Apply poison if needed
                if (canGivePoison) {
                    fun applySadisticPowerIfNeed() {
                        player.getPower(SadisticPower.POWER_ID)?.let { sadisticPower ->
                            if (aliveMonsterCount > 1) {
                                add(Action.DamageThorns(0, sadisticPower.amount, ActionTarget.Random))
                            } else {
                                add(Action.DamageThorns(sadisticPower.amount, ActionTarget.Single(monster)))
                            }
                        }
                    }
                    when (cardID) {
                        BouncingFlask.ID -> {
                            repeat(getDebuffInstanceCount()) {
                                if (monsterArtifactPowerAmount-- <= 0) {
                                    if (aliveMonsterCount == 1) {
                                        add(Action.StackPoison(getPoisonAmount(monster), monster))
                                    }
                                    applySadisticPowerIfNeed()
                                }
                            }
                        }

                        else -> {
                            if (monsterArtifactPowerAmount-- <= 0) {
                                add(Action.StackPoison(getPoisonAmount(monster), monster))
                                applySadisticPowerIfNeed()
                            }
                        }
                    }
                }
            }.asGroupedAction()
        }.asGroupedAction()
    }
}
