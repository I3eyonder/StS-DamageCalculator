package dmgcalculator.renderer

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.megacrit.cardcrawl.cards.AbstractCard
import com.megacrit.cardcrawl.cards.AbstractCard.CardType
import com.megacrit.cardcrawl.cards.colorless.BandageUp
import com.megacrit.cardcrawl.cards.colorless.Bite
import com.megacrit.cardcrawl.cards.colorless.JAX
import com.megacrit.cardcrawl.cards.curses.Decay
import com.megacrit.cardcrawl.cards.curses.Pain
import com.megacrit.cardcrawl.cards.curses.Regret
import com.megacrit.cardcrawl.cards.red.Bloodletting
import com.megacrit.cardcrawl.cards.red.Entrench
import com.megacrit.cardcrawl.cards.red.Hemokinesis
import com.megacrit.cardcrawl.cards.red.Offering
import com.megacrit.cardcrawl.cards.status.Burn
import com.megacrit.cardcrawl.characters.AbstractPlayer
import com.megacrit.cardcrawl.dungeons.AbstractDungeon
import com.megacrit.cardcrawl.orbs.AbstractOrb
import com.megacrit.cardcrawl.orbs.EmptyOrbSlot
import com.megacrit.cardcrawl.orbs.Frost
import com.megacrit.cardcrawl.powers.*
import com.megacrit.cardcrawl.powers.watcher.BlockReturnPower
import com.megacrit.cardcrawl.powers.watcher.LikeWaterPower
import com.megacrit.cardcrawl.relics.*
import com.megacrit.cardcrawl.stances.CalmStance
import dmgcalculator.entities.*
import dmgcalculator.util.*
import dmgcalculator.util.Utils.addDuplicationCardActionIfNeeded
import dmgcalculator.util.Utils.addToBottom
import dmgcalculator.util.Utils.addToTop
import dmgcalculator.util.Utils.getEvokedOrbsWhenExceededMax
import dmgcalculator.util.Utils.getRetainedOrbsWhenExceededMax
import dmgcalculator.util.Utils.replacesWith
import dmgcalculator.util.Utils.toSimpleCardInfoList

object PlayerRenderer {

    private var cachedMsg = ""
    private var isPlayerTurn = false

    fun render(sb: SpriteBatch, hoveredCard: AbstractCard?, isPlayerTurn: Boolean) {
        this.isPlayerTurn = isPlayerTurn
        val msg = if (isPlayerTurn) {
            cachedMsg = ""
            createRenderMessage(hoveredCard)
        } else cachedMsg.ifEmpty {
            createRenderMessage().also {
                cachedMsg = it.ifEmpty {
                    " "
                }
            }
        }

        if (msg.isNotEmpty()) {
            sb.renderFixedSizeMessage(
                msg,
                AbstractDungeon.player.hb.cX,
                AbstractDungeon.player.hb.cY + AbstractDungeon.player.hb.height / 2 + 125f,
            )
        }
    }

    private fun createRenderMessage(hoveredCard: AbstractCard? = null): String {
        val renderMessages = mutableListOf<String>()
        val initialPlayerInfo = CreatureInfo(AbstractDungeon.player)
        val (cardActions, remainingHandCards) = getCardIntentActions(hoveredCard)
        val worstPlayerInfo = initialPlayerInfo.copy(
            powersAmount = initialPlayerInfo.powersAmount.toMutableMap(),
        )
        val bestPlayerInfo = initialPlayerInfo.copy(
            powersAmount = initialPlayerInfo.powersAmount.toMutableMap(),
        )

        // Card actions
        if (cardActions.isNotEmpty()) {
            val worstCardActionResult = worstPlayerInfo.takeActions(cardActions, true)
            val bestCardActionResult = bestPlayerInfo.takeActions(cardActions, false)
            if (worstCardActionResult != ActionResult.EMPTY || bestCardActionResult != ActionResult.EMPTY) {
                renderMessages.add(
                    buildString {
                        append("--Card--".colored("#FCBA03"))
                        appendLine()
                        append(
                            buildOutcomeMessage(
                                worstOutcomeResult = OutcomeResult(worstPlayerInfo, worstCardActionResult),
                                bestOutcomeResult = OutcomeResult(bestPlayerInfo, bestCardActionResult),
                            ),
                        )
                    }
                )
            }
        }

        if (!bestPlayerInfo.isDead) {
            val worstEndTurnActionResult =
                getEndTurnIntentActions(worstPlayerInfo, hoveredCard, remainingHandCards).let {
                    worstPlayerInfo.takeActions(it, true)
                }
            val bestEndTurnActionResult =
                getEndTurnIntentActions(bestPlayerInfo, hoveredCard, remainingHandCards).let {
                    bestPlayerInfo.takeActions(it, false)
                }
            if (worstEndTurnActionResult != ActionResult.EMPTY || bestEndTurnActionResult != ActionResult.EMPTY) {
                renderMessages.add(
                    buildString {
                        append("--End Turn--".colored("#FCBA03"))
                        appendLine()
                        append(
                            buildOutcomeMessage(
                                worstOutcomeResult = OutcomeResult(worstPlayerInfo, worstEndTurnActionResult),
                                bestOutcomeResult = OutcomeResult(bestPlayerInfo, bestEndTurnActionResult),
                            ),
                        )
                    }
                )
            }
        }

        return renderMessages.joinToString("\n")
    }

    private fun AbstractCard.createIntentActions(): List<Action> = buildList {
        repeat(getActionHitCount()) {
            if (block > 0 && baseBlock >= 0 && !isFakeGainBlockCard) {
                add(Action.GainBlock(block, AbstractDungeon.player))
            }
            val targetingMonsters = if (isDamageAllEnemiesCard) {
                AbstractDungeon.getMonsters().aliveMonsters
            } else {
                listOfNotNull(AbstractDungeon.player.getHoveredMonster())
            }
            targetingMonsters.forEach { targetingMonster ->
                targetingMonster.getPower(BlockReturnPower.POWER_ID)?.let { blockReturnPower ->
                    add(Action.GainBlock(blockReturnPower.amount, AbstractDungeon.player))
                }
                targetingMonster.getPower(ThornsPower.POWER_ID)?.let { thornsPower ->
                    add(
                        Action.DamageThorns(
                            thornsPower.amount,
                            ActionTarget.Single(AbstractDungeon.player, false),
                        )
                    )
                }
            }
        }

        if (cardID == Entrench.ID && AbstractDungeon.player.currentBlock > 0) {
            add(Action.GainBlock(AbstractDungeon.player.currentBlock, AbstractDungeon.player))
        }

        AbstractDungeon.getMonsters().aliveMonsters.forEach { monster ->
            monster.getPower(SharpHidePower.POWER_ID)?.let { sharpHidePower ->
                if (type == CardType.ATTACK) {
                    add(
                        Action.DamageThorns(
                            sharpHidePower.amount,
                            ActionTarget.Single(AbstractDungeon.player, false),
                        )
                    )
                }
            }
            monster.getPower(BeatOfDeathPower.POWER_ID)?.let { beatOfDeathPower ->
                add(
                    Action.DamageThorns(
                        beatOfDeathPower.amount,
                        ActionTarget.Single(AbstractDungeon.player, false),
                    )
                )
            }
        }

        AbstractDungeon.player.getPower(RagePower.POWER_ID)?.let { ragePower ->
            if (type == CardType.ATTACK) {
                add(Action.GainBlock(ragePower.amount, AbstractDungeon.player))
            }
        }
        AbstractDungeon.player.getPower(AfterImagePower.POWER_ID)?.let { afterImagePower ->
            add(Action.GainBlock(afterImagePower.amount, AbstractDungeon.player))
        }

        // Healing or Losing HP effects
        when (cardID) {
            BandageUp.ID, Bite.ID -> {
                add(Action.GainHP(magicNumber, AbstractDungeon.player))
            }

            Offering.ID -> {
                add(Action.LoseHP(6, AbstractDungeon.player))
            }

            Bloodletting.ID, JAX.ID -> {
                add(Action.LoseHP(3, AbstractDungeon.player))
            }

            Hemokinesis.ID -> {
                add(Action.LoseHP(magicNumber, AbstractDungeon.player))
            }
        }

        // Losing HP by curse
        if (type == CardType.CURSE && AbstractDungeon.player.hasRelic(BlueCandle.ID)) {
            add(Action.LoseHP(1, AbstractDungeon.player))
        }

        AbstractDungeon.player.hand.group.count {
            it.cardID == Pain.ID && it.uuid != this@createIntentActions.uuid
        }.let { painCardCount ->
            if (painCardCount > 0) {
                add(Action.LoseHP(painCardCount, AbstractDungeon.player))
            }
        }

        // Apply BirdFacedUrn relic if needed
        if (type == CardType.POWER && AbstractDungeon.player.hasRelic(BirdFacedUrn.ID)) {
            add(Action.GainHP(2, AbstractDungeon.player))
        }

        // Evoke orbs if needed
        val accumulatePlayerOrbs = AbstractDungeon.player.orbs
            .plus(getChannelingOrbs())
            .filterNot {
                it is EmptyOrbSlot
            }
        // Evoked orbs due to exceeded max
        accumulatePlayerOrbs.getEvokedOrbsWhenExceededMax().forEach { evokedOrb ->
            addOrbAction(evokedOrb, true)
        }
        if (isOrbEvokeCard) {
            // Evoke orbs from retained orbs
            accumulatePlayerOrbs.getRetainedOrbsWhenExceededMax()
                .let { retainedOrbs ->
                    retainedOrbs.take(getOrbEvokeCount(retainedOrbs.size))
                }
                .forEach { evokingOrb ->
                    repeat(getEvokeHitCount()) {
                        addOrbAction(evokingOrb, true)
                    }
                }
        }
    }

    private fun MutableList<Action>.addOrbAction(orb: AbstractOrb, isEvoke: Boolean) {
        if (orb.ID == Frost.ORB_ID) {
            val blockAmount = if (isEvoke) {
                orb.evokeAmount
            } else {
                orb.passiveAmount
            }
            addToBottom(Action.GainBlock(blockAmount, AbstractDungeon.player))
        }
    }

    private fun getCardIntentActions(hoveredCard: AbstractCard? = null): Pair<List<Action>, List<AbstractCard>> {
        val handCards = AbstractDungeon.player.hand.group.toMutableList()

        //Resolve card actions
        val cardActions = hoveredCard?.let { hoveringCard ->
            if (!hoveringCard.isCardPlayable()) {
                return@let emptyList()
            }

            val baseAction = hoveringCard.createIntentActions().asGroupedAction()
            val actions = mutableListOf<Action>(baseAction)
            if (hoveringCard.cardID == Entrench.ID) {
                val initialCurrentBlock = AbstractDungeon.player.currentBlock
                actions.addDuplicationCardActionIfNeeded(hoveringCard) {
                    AbstractDungeon.player.currentBlock *= 2
                    createIntentActions().asGroupedAction()
                }
                AbstractDungeon.player.currentBlock = initialCurrentBlock
            } else {
                actions.addDuplicationCardActionIfNeeded(hoveringCard) {
                    createIntentActions().asGroupedAction()
                }
            }

            // Apply FeelNoPain power if needed
            AbstractDungeon.player.getPower(FeelNoPainPower.POWER_ID)?.let { feelNoPainPower ->
                actions.replacesWith { originActions ->
                    var handCardInfos = handCards.toSimpleCardInfoList()
                    originActions.mapIndexed { index, action ->
                        val exhaustInfo = hoveringCard.getExhaustInfo(handCardInfos)
                        listOf(action)
                            .plus(
                                if (exhaustInfo.selfExhaust && index == 0) {
                                    Action.GainBlock(feelNoPainPower.amount, AbstractDungeon.player)
                                } else {
                                    Action.NoAction
                                }
                            )
                            .plus(
                                List(exhaustInfo.exhaustInHand.size + exhaustInfo.exhaustInDrawPile) {
                                    Action.GainBlock(feelNoPainPower.amount, AbstractDungeon.player)
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
        }?.flatten().orEmpty()
        return cardActions to handCards
    }

    private fun getEndTurnIntentActions(
        creatureInfo: CreatureInfo<AbstractPlayer>,
        hoveredCard: AbstractCard? = null,
        remainingHandCards: List<AbstractCard>,
    ): List<Action> {
        val player = creatureInfo.creature
        // Resolve relics
        val relicEffect = buildList {
            player.relics.forEach { relic ->
                when (relic.relicId) {
                    Orichalcum.ID -> {
                        if (creatureInfo.remainBlock == 0 || (relic as Orichalcum).trigger) {
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
            val accumulateOrbs = creatureInfo.creature.orbs
                .plus(hoveredCard?.getChannelingOrbs().orEmpty())
                .filterNot {
                    it is EmptyOrbSlot
                }
                .let {
                    if (player.hasRelic(FrozenCore.ID) &&
                        it.size < creatureInfo.creature.maxOrbs &&
                        isPlayerTurn
                    ) {
                        it.plus(Frost())
                    } else {
                        it
                    }
                }
            // Retained orbs
            accumulateOrbs.getRetainedOrbsWhenExceededMax()
                .let { retainedOrbs ->
                    if (hoveredCard?.isOrbEvokeCard == true) {
                        retainedOrbs.drop(hoveredCard.getOrbEvokeCount(retainedOrbs.size))
                    } else {
                        retainedOrbs
                    }
                }
                .forEachIndexed { orbIndex, retainOrb ->
                    addOrbAction(retainOrb, false)
                    if (player.hasRelic(GoldPlatedCables.ID) && orbIndex == 0) {
                        addOrbAction(retainOrb, false)
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

        // Resolve start of next turn actions
        val startOfNextTurnActions = buildList {
            if (player.hasPower(PoisonPower.POWER_ID)) {
                val poisonPower = player.getPower(PoisonPower.POWER_ID)
                addToBottom(Action.LoseHP(poisonPower.amount, player))
            }
        }

        // Resolve pending actions
        val pendingActions = creatureInfo.pendingActions.toList().also {
            creatureInfo.pendingActions.clear()
        }

        return (relicEffect + powerPreHandEffects + handDamageActions +
                orbsActions + powerAfterHandEffects + monsterAttackIntentActions +
                startOfNextTurnActions + pendingActions).flatten()
    }

}
