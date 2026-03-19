package dmgcalculator.renderer

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.megacrit.cardcrawl.cards.AbstractCard
import com.megacrit.cardcrawl.cards.AbstractCard.CardType
import com.megacrit.cardcrawl.cards.colorless.BandageUp
import com.megacrit.cardcrawl.cards.colorless.Bite
import com.megacrit.cardcrawl.cards.colorless.JAX
import com.megacrit.cardcrawl.cards.colorless.RitualDagger
import com.megacrit.cardcrawl.cards.curses.Decay
import com.megacrit.cardcrawl.cards.curses.Pain
import com.megacrit.cardcrawl.cards.curses.Regret
import com.megacrit.cardcrawl.cards.red.Bloodletting
import com.megacrit.cardcrawl.cards.red.Hemokinesis
import com.megacrit.cardcrawl.cards.red.Offering
import com.megacrit.cardcrawl.cards.status.Burn
import com.megacrit.cardcrawl.dungeons.AbstractDungeon
import com.megacrit.cardcrawl.orbs.EmptyOrbSlot
import com.megacrit.cardcrawl.orbs.Frost
import com.megacrit.cardcrawl.powers.*
import com.megacrit.cardcrawl.powers.watcher.BlockReturnPower
import com.megacrit.cardcrawl.powers.watcher.LikeWaterPower
import com.megacrit.cardcrawl.relics.*
import com.megacrit.cardcrawl.stances.CalmStance
import dmgcalculator.entities.*
import dmgcalculator.util.*
import dmgcalculator.util.Utils.addToBottom
import dmgcalculator.util.Utils.addToTop
import dmgcalculator.util.Utils.replacesWith
import dmgcalculator.util.Utils.toSimpleCardInfoList

object PlayerRenderer {

    private var cachedMsg: String? = null
    private val msgBuilder = StringBuilder()

    fun render(sb: SpriteBatch, hoveredCard: AbstractCard?, isPlayerTurn: Boolean) {
        msgBuilder.clear()
        val msg = if (isPlayerTurn) {
            cachedMsg = null
            val creatureInfo = CreatureInfo(AbstractDungeon.player)
            val (cardActions, remainingHandCards) = getCardIntentActions(hoveredCard)
            val (worstCardOutcome, bestCardOutcome) = if (cardActions.isNotEmpty()) {
                cardActions.calculateOutcome(creatureInfo)
            } else {
                null to null
            }
            val endTurnIntentActions = getEndTurnIntentActions(hoveredCard, cardActions, remainingHandCards)
            if (worstCardOutcome != null && bestCardOutcome != null) {
                msgBuilder.buildOutcomeMessage(
                    worstOutcome = worstCardOutcome,
                    bestOutcome = bestCardOutcome,
                    showRemainHP = bestCardOutcome.adjustHP != 0,
                    showTakenDamage = false,
                )
                if (endTurnIntentActions.isNotEmpty() && !bestCardOutcome.isDead) {
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
            msgBuilder.toString()
        } else if (cachedMsg.isNullOrEmpty()) {
            val creatureInfo = CreatureInfo(AbstractDungeon.player)
            val (cardActions, remainingHandCards) = getCardIntentActions(null)
            val endTurnIntentActions = getEndTurnIntentActions(null, cardActions, remainingHandCards)
            if (endTurnIntentActions.isNotEmpty()) {
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
            msgBuilder.toString().also {
                cachedMsg = it
            }
        } else {
            cachedMsg.orEmpty()
        }

        if (msg.isNotEmpty()) {
            sb.renderFixedSizeMessage(
                msg,
                AbstractDungeon.player.hb.cX,
                AbstractDungeon.player.hb.cY + AbstractDungeon.player.hb.height / 2 + 125f,
            )
        }
    }

    private fun AbstractCard.createIntentActions(): List<Action> = buildList {
        val player = AbstractDungeon.player
        player.getPower(RagePower.POWER_ID)?.let { ragePower ->
            if (type == CardType.ATTACK) {
                add(Action.GainBlock(ragePower.amount, player))
            }
        }
        player.getHoveredMonster()?.let { hoveredMonster ->
            hoveredMonster.getPower(BlockReturnPower.POWER_ID)?.let { blockReturnPower ->
                if (type == CardType.ATTACK) {
                    add(Action.GainBlock(blockReturnPower.amount, player))
                }
            }
        }
        if (block > 0) {
            if (cardID !in listOf(RitualDagger.ID)) {
                add(Action.GainBlock(block, player))
            }
        }

        // Healing or Losing HP effects
        when (cardID) {
            BandageUp.ID, Bite.ID -> {
                add(Action.GainHP(magicNumber, player))
            }

            Offering.ID -> {
                add(Action.LoseHP(6, player))
            }

            Bloodletting.ID, JAX.ID -> {
                add(Action.LoseHP(3, player))
            }

            Hemokinesis.ID -> {
                add(Action.LoseHP(magicNumber, player))
            }
        }

        // Losing HP by curse
        if (player.hasRelic(BlueCandle.ID) && type == CardType.CURSE) {
            add(Action.LoseHP(1, player))
        }

        val painCardCount = player.hand.group.count {
            it.cardID == Pain.ID && it.uuid != this@createIntentActions.uuid
        }

        if (painCardCount > 0) {
            add(Action.LoseHP(painCardCount, player))
        }
    }

    private fun getCardIntentActions(hoveredCard: AbstractCard? = null): Pair<List<Action>, List<AbstractCard>> {
        val player = AbstractDungeon.player
        val handCards = AbstractDungeon.player.hand.group.toMutableList()
        //Resolve card actions
        val cardActions = hoveredCard?.let { hoveringCard ->
            val cardHitCount = hoveringCard.getActionHitCount()
            val baseAction = List(cardHitCount) {
                hoveringCard.createIntentActions().asGroupedAction()
            }.asGroupedAction()
            val actions = mutableListOf<Action>(baseAction)
            AbstractDungeon.player.powers.forEach { power ->
                when (power.ID) {
                    DoubleTapPower.POWER_ID -> {
                        if (hoveringCard.type == CardType.ATTACK) {
                            actions.addToBottom(baseAction)
                        }
                    }

                    DuplicationPower.POWER_ID -> {
                        actions.addToBottom(baseAction)
                    }

                    EchoPower.POWER_ID -> {
                        val cardsDoubledThisTurn = power.getPrivateField<Int>("cardsDoubledThisTurn") ?: 0
                        if (power.amount > 0 &&
                            AbstractDungeon.actionManager.cardsPlayedThisTurn.size + 1 - cardsDoubledThisTurn <= power.amount
                        ) {
                            actions.addToBottom(baseAction)
                        }
                    }
                }
            }

            // Apply Necronomicon relic if needed
            player.getRelic(Necronomicon.ID)?.let { necronomiconRelic ->
                if (hoveringCard.type == CardType.ATTACK && necronomiconRelic.checkTrigger()) {
                    actions.addToBottom(baseAction)
                }
            }

            // Apply BirdFacedUrn relic if needed
            if (player.hasRelic(BirdFacedUrn.ID) && hoveringCard.type == CardType.POWER) {
                actions.addToBottom(Action.GainHP(2, player))
            }

            // Apply FeelNoPain power if needed
            if (player.hasPower(FeelNoPainPower.POWER_ID)) {
                val feelNoPainPower = player.getPower(FeelNoPainPower.POWER_ID)
                actions.replacesWith { originActions ->
                    var handCardInfos = handCards.toSimpleCardInfoList()
                    originActions.mapIndexed { index, action ->
                        val exhaustInfo = hoveringCard.getExhaustInfo(handCardInfos)
                        listOf(action)
                            .plus(
                                if (exhaustInfo.selfExhaust && index == 0) {
                                    Action.GainBlock(feelNoPainPower.amount, player)
                                } else {
                                    Action.NoAction
                                }
                            )
                            .plus(
                                List(exhaustInfo.exhaustInHand.size + exhaustInfo.exhaustInDrawPile) {
                                    Action.GainBlock(feelNoPainPower.amount, player)
                                }
                            )
                            .asGroupedAction()
                            .also {
                                handCardInfos = handCardInfos.filterNot {
                                    it.isHovered || exhaustInfo.exhaustInHand.contains(it)
                                }.plus(
                                    List(exhaustInfo.drawCard) {
                                        SimpleCardInfo.DUMMY
                                    }
                                ).take(10)
                            }
                    }
                }
            }

            // Assume card has been played
            handCards.removeIf {
                it.uuid == hoveringCard.uuid
            }
            actions.toList()
        }?.flatten() ?: emptyList()
        return cardActions to handCards
    }

    private fun getEndTurnIntentActions(
        hoveredCard: AbstractCard? = null,
        cardActions: List<Action>,
        remainingHandCards: List<AbstractCard>,
    ): List<Action> {
        val player = AbstractDungeon.player
        // Resolve relics
        val relicEffect = buildList {
            player.relics.forEach { relic ->
                when (relic.relicId) {
                    Orichalcum.ID -> {
                        val willCardGainBlock = cardActions.flatten().any {
                            it is Action.GainBlock && it.value > 0
                        }
                        if ((player.currentBlock == 0 && !willCardGainBlock) || (relic as Orichalcum).trigger) {
                            addToTop(Action.GainBlock(6, player))
                        }
                    }

                    CloakClasp.ID -> {
                        if (!remainingHandCards.isEmpty()) {
                            addToBottom(Action.GainBlock(remainingHandCards.size, player))
                        }
                    }

                    OrnamentalFan.ID -> {
                        if (hoveredCard?.type == CardType.ATTACK && (relic.counter + 1) % 3 == 0) {
                            addToBottom(Action.GainBlock(4, player))
                        }
                    }
                }
            }
        }

        // Resolve power effects pre hand
        val powerPreHandEffects = buildList {
            player.powers.forEach { power ->
                when (power.ID) {
                    PlatedArmorPower.POWER_ID,
                    MetallicizePower.POWER_ID,
                        -> addToBottom(Action.GainBlock(power.amount.coerceAtLeast(0), player))

                    LikeWaterPower.POWER_ID -> {
                        if (player.stance.ID == CalmStance.STANCE_ID) {
                            addToBottom(Action.GainBlock(power.amount, player))
                        }
                    }
                }
            }
        }

        // Resolve hand intent actions
        val handDamageActions = buildList {
            remainingHandCards.forEach { card ->
                when (card.cardID) {
                    Burn.ID -> addToBottom(Action.DamageThorns(card.magicNumber))
                    Decay.ID -> addToBottom(Action.DamageThorns(2))
                    Regret.ID -> addToBottom(Action.LoseHP(remainingHandCards.size, player))
                }
            }
        }

        // Resolve orbs actions
        val orbsActions = buildList {
            val accumulateOrbs = player.orbs
                .plus(hoveredCard?.getChannelingOrbs().orEmpty())
                .filterNot {
                    it is EmptyOrbSlot
                }
            accumulateOrbs.dropLast(player.maxOrbs).forEach { evokedOrb ->
                if (evokedOrb.ID == Frost.ORB_ID) {
                    addToBottom(Action.GainBlock(evokedOrb.evokeAmount, player))
                }

            }
            accumulateOrbs.takeLast(player.maxOrbs).forEach { retainOrb ->
                if (retainOrb.ID == Frost.ORB_ID) {
                    addToBottom(Action.GainBlock(retainOrb.passiveAmount, player))
                }
            }
        }

        // Resolve power effects after hand
        val powerAfterHandEffects = buildList {
            player.powers.forEach { power ->
                when (power.ID) {
                    ConstrictedPower.POWER_ID -> addToBottom(Action.DamageThorns(power.amount))
                    CombustPower.POWER_ID -> power.getPrivateField<Int>("hpLoss")?.let {
                        addToBottom(Action.LoseHP(it, player))
                    }

                    RegenerateMonsterPower.POWER_ID -> addToBottom(Action.GainHP(power.amount, player))
                    RegenPower.POWER_ID -> addToTop(Action.GainHP(power.amount, player))
                }
            }
        }

        // Resolve monster attack intent
        val monsterAttackIntentActions = AbstractDungeon.getMonsters().aliveMonsters.getAttackIntentActions()

        val startOfNextTurnActions = buildList {
            if (player.hasPower(PoisonPower.POWER_ID)) {
                val poisonPower = player.getPower(PoisonPower.POWER_ID)
                addToBottom(Action.LoseHP(poisonPower.amount, player))
            }
        }

        return (relicEffect + powerPreHandEffects + handDamageActions +
                orbsActions + powerAfterHandEffects + monsterAttackIntentActions +
                startOfNextTurnActions).flatten()
    }

}
