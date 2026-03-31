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
import com.megacrit.cardcrawl.core.AbstractCreature
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
import dmgcalculator.config.ModConfig
import dmgcalculator.entities.*
import dmgcalculator.util.*
import dmgcalculator.util.Utils.addDuplicationCardActionIfNeeded
import dmgcalculator.util.Utils.addToBottom
import dmgcalculator.util.Utils.replacesWith
import dmgcalculator.util.Utils.toSimpleCardInfoList

object MonsterRenderer {

    private val cachedMsg = mutableMapOf<AbstractMonster, String>()

    fun render(sb: SpriteBatch, hoveredCard: AbstractCard?, isPlayerTurn: Boolean) {
        val aliveMonstersIndexed = AbstractDungeon.getMonsters().aliveMonstersIndexed
        val hoveredMonster = AbstractDungeon.player.getHoveredMonster()
        val aliveMonsterCount = aliveMonstersIndexed.size
        val renderMessages = if (isPlayerTurn) {
            cachedMsg.clear()
            createRenderMessages(aliveMonstersIndexed, aliveMonsterCount, hoveredCard, hoveredMonster)
        } else cachedMsg.ifEmpty {
            createRenderMessages(aliveMonstersIndexed, aliveMonsterCount).also {
                cachedMsg.putAll(it)
            }
        }
        aliveMonstersIndexed.forEach { (_, monster) ->
            renderMessages[monster]?.let { msg ->
                if (msg.isNotEmpty()) {
                    sb.renderFixedSizeMessage(
                        msg,
                        monster.hb.cX,
                        monster.hb.cY + monster.hb.height / 2 + 125f,
                    )
                }
            }
        }
    }

    private fun createRenderMessages(
        aliveMonstersIndexed: List<IndexedValue<AbstractMonster>>,
        aliveMonsterCount: Int,
        hoveredCard: AbstractCard? = null,
        hoveredMonster: AbstractMonster? = null,
    ): Map<AbstractMonster, String> = buildMap {
        aliveMonstersIndexed.forEach { (index, monster) ->
            val renderMessages = mutableListOf<String>()
            val initialMonsterInfo = CreatureInfo(monster)
            val cardIntentActions = getCardIntentActions(
                hoveredCard = hoveredCard,
                hoveredMonster = hoveredMonster,
                monsterInfo = initialMonsterInfo,
                monsterIndex = index,
                aliveMonsterCount = aliveMonsterCount,
            )
            val worstMonsterInfo = initialMonsterInfo.copy(
                powersAmount = initialMonsterInfo.powersAmount.toMutableMap(),
            )
            val bestMonsterInfo = initialMonsterInfo.copy(
                powersAmount = initialMonsterInfo.powersAmount.toMutableMap(),
            )
            if (cardIntentActions.isNotEmpty()) {
                // For player, worst result on monster mean they take minimum of damages
                val worstActionResult = worstMonsterInfo.takeActions(cardIntentActions, false)
                // For player, best result on monster mean they take maximum of damages
                val bestActionResult = bestMonsterInfo.takeActions(cardIntentActions, true)
                if (worstActionResult != ActionResult.EMPTY || bestActionResult != ActionResult.EMPTY) {
                    renderMessages.add(
                        buildString {
                            append("--Card--".colored("#FCBA03"))
                            appendLine()
                            append(
                                buildOutcomeMessage(
                                    worstOutcomeResult = OutcomeResult(worstMonsterInfo, worstActionResult),
                                    bestOutcomeResult = OutcomeResult(bestMonsterInfo, bestActionResult),
                                ),
                            )
                        }
                    )
                }
            }

            if (!worstMonsterInfo.isDead) {
                // For player, worst result on monster mean they take minimum of damages
                val worstEndTurnActionResult =
                    worstMonsterInfo.getEndTurnIntentActions(aliveMonsterCount, hoveredCard).let {
                        worstMonsterInfo.takeActions(it, false)
                    }
                // For player, best result on monster mean they take maximum of damages
                val bestEndTurnActionResult =
                    bestMonsterInfo.getEndTurnIntentActions(aliveMonsterCount, hoveredCard).let {
                        bestMonsterInfo.takeActions(it, true)
                    }
                if (worstEndTurnActionResult != ActionResult.EMPTY || bestEndTurnActionResult != ActionResult.EMPTY) {
                    renderMessages.add(
                        buildString {
                            append("--End Turn--".colored("#FCBA03"))
                            appendLine()
                            append(
                                buildOutcomeMessage(
                                    worstOutcomeResult = OutcomeResult(worstMonsterInfo, worstEndTurnActionResult),
                                    bestOutcomeResult = OutcomeResult(bestMonsterInfo, bestEndTurnActionResult),
                                ),
                            )
                        }
                    )
                }
            }

            if (ModConfig.calculatePlayerThornsDamage && !worstMonsterInfo.isDead) {
                // For player, worst result on monster mean they take minimum of damages
                val worstThornActionResult = worstMonsterInfo.pendingActions.let {
                    worstMonsterInfo.takeActions(it, false)
                }
                // For player, best result on monster mean they take maximum of damages
                val bestThornActionResult = bestMonsterInfo.pendingActions.let {
                    bestMonsterInfo.takeActions(it, true)
                }
                if (worstThornActionResult != ActionResult.EMPTY || bestThornActionResult != ActionResult.EMPTY) {
                    renderMessages.add(
                        buildString {
                            append("--Thorn--".colored("#FCBA03"))
                            appendLine()
                            append(
                                buildOutcomeMessage(
                                    worstOutcomeResult = OutcomeResult(worstMonsterInfo, worstThornActionResult),
                                    bestOutcomeResult = OutcomeResult(bestMonsterInfo, bestThornActionResult),
                                ),
                            )
                        }
                    )
                }
            }
            this[monster] = renderMessages.joinToString("\n")
        }
    }

    private fun getCardIntentActions(
        hoveredCard: AbstractCard?,
        hoveredMonster: AbstractMonster?,
        monsterInfo: CreatureInfo<AbstractMonster>,
        monsterIndex: Int,
        aliveMonsterCount: Int,
    ): List<Action> {
        return hoveredCard?.let { hoveringCard ->
            if (!hoveringCard.isCardPlayable()) {
                return@let emptyList()
            }
            hoveringCard.getIntentActions(monsterInfo, monsterIndex, aliveMonsterCount).run {
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
        }.orEmpty()
    }

    private fun CreatureInfo<AbstractMonster>.getEndTurnIntentActions(
        aliveMonsterCount: Int,
        hoveredCard: AbstractCard?,
    ): List<Action> = buildList {
        // player relics damage
        AbstractDungeon.player.relics.forEach { relic ->
            when (relic.relicId) {
                StoneCalendar.ID -> {
                    if (relic.counter == 7) {
                        addToBottom(Action.DamageThorns(52))
                    }
                }

                Orichalcum.ID -> {
                    if (AbstractDungeon.player.currentBlock == 0 || (relic as Orichalcum).trigger)
                        if (AbstractDungeon.player.hasPower(JuggernautPower.POWER_ID)) {
                            val juggernautPower = AbstractDungeon.player.getPower(JuggernautPower.POWER_ID)
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
                    if (AbstractDungeon.player.hand.group.isNotEmpty())
                        if (AbstractDungeon.player.hasPower(JuggernautPower.POWER_ID)) {
                            val juggernautPower = AbstractDungeon.player.getPower(JuggernautPower.POWER_ID)
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
        val playerOrbs = AbstractDungeon.player.orbs.plus(hoveredCard?.getChannelingOrbs().orEmpty()).filterNot {
            it is EmptyOrbSlot
        }.takeLast(AbstractDungeon.player.maxOrbs).let {
            if (hoveredCard?.isOrbEvokeCard == true) {
                it.drop(1)
            } else {
                it
            }
        }
        val orbDamageMultiplier = if (creature.hasPower(LockOnPower.POWER_ID)) 1.5f else 1f
        playerOrbs.forEach { orb ->
            when (orb.ID) {
                Lightning.ORB_ID -> {
                    val damageAmount = orb.passiveAmount.times(orbDamageMultiplier).toInt()
                    if (AbstractDungeon.player.hasPower(ElectroPower.POWER_ID)) {
                        addToBottom(Action.DamageThorns(damageAmount))
                    } else {
                        addToBottom(Action.DamageThorns(0, damageAmount, ActionTarget.Random))
                    }
                }

                Frost.ORB_ID -> {
                    if (AbstractDungeon.player.hasPower(JuggernautPower.POWER_ID)) {
                        val juggernautPower = AbstractDungeon.player.getPower(JuggernautPower.POWER_ID)
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
        AbstractDungeon.player.powers.forEach { power ->
            when {
                power.ID == CombustPower.POWER_ID ||
                        power.ID == OmegaPower.POWER_ID -> addToBottom(Action.DamageThorns(power.amount))

                power.ID.contains(TheBombPower.POWER_ID) && power.amount == 1 -> power.getPrivateField<Int>("damage")
                    ?.let {
                        addToBottom(Action.DamageThorns(it))
                    }

                power.ID == ThornsPower.POWER_ID -> {
                    repeat(this@getEndTurnIntentActions.creature.getAttackIntentActions().size) {
                        addToBottom(Action.DamageThorns(power.amount).withPendingTag())
                    }
                }
            }
        }

        // monster poison damage
        creature.getPower(PoisonPower.POWER_ID)?.let { poisonPower ->
            addToBottom(Action.LoseHP(poisonPower.amount, creature))
        }

        // pending actions
        addAll(pendingActions).also {
            pendingActions.clear()
        }
    }

    private fun AbstractCard.getIntentActions(
        monsterInfo: CreatureInfo<AbstractMonster>,
        monsterIndex: Int,
        aliveMonsterCount: Int,
    ): List<Action> {
        val baseAction = createIntentActions(monsterInfo, monsterIndex, aliveMonsterCount).asGroupedAction()
        val actions = mutableListOf<Action>(baseAction)

        // Apply powers that modify attack here if needed
        if (canGiveVulnerable && !monsterInfo.hasPower(VulnerablePower.POWER_ID)) {
            monsterInfo.creature.applyTemporaryPowers(VulnerablePower(monsterInfo.creature, 1, false)) {
                if (AbstractDungeon.player.hasPower(VigorPower.POWER_ID)) {
                    AbstractDungeon.player.temporaryRemoveAmountFromPowers(VigorPower.POWER_ID) {
                        actions.addDuplicationCardActionIfNeeded(this@getIntentActions) {
                            createIntentActions(
                                monsterInfo,
                                monsterIndex,
                                aliveMonsterCount
                            ).asGroupedAction()
                        }
                    }
                } else {
                    actions.addDuplicationCardActionIfNeeded(this@getIntentActions) {
                        createIntentActions(monsterInfo, monsterIndex, aliveMonsterCount).asGroupedAction()
                    }
                }
            }
        } else {
            if (AbstractDungeon.player.hasPower(VigorPower.POWER_ID)) {
                AbstractDungeon.player.temporaryRemoveAmountFromPowers(VigorPower.POWER_ID) {
                    actions.addDuplicationCardActionIfNeeded(this@getIntentActions) {
                        createIntentActions(monsterInfo, monsterIndex, aliveMonsterCount).asGroupedAction()
                    }
                }
            } else {
                actions.addDuplicationCardActionIfNeeded(this) {
                    createIntentActions(monsterInfo, monsterIndex, aliveMonsterCount).asGroupedAction()
                }
            }
        }

        // Apply Charon's Ashes relic if needed
        AbstractDungeon.player.getRelic(CharonsAshes.ID)?.let { charonsAshesRelic ->
            actions.replacesWith { originActions ->
                var hand = AbstractDungeon.player.hand.group.toSimpleCardInfoList()
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
        AbstractDungeon.player.getRelic(LetterOpener.ID)?.let { letterOpenerRelic ->
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
            var hand = AbstractDungeon.player.hand.group.toSimpleCardInfoList()
            originActions.mapIndexed { index, action ->
                val monsterExtraActions = monsterInfo.creature.powers.mapNotNull { power ->
                    when (power.ID) {
                        ChokePower.POWER_ID -> Action.LoseHP(power.amount, monsterInfo.creature)
                        else -> null
                    }
                }
                val playerExtraActions = AbstractDungeon.player.powers.mapNotNull { power ->
                    when (power.ID) {
                        ThousandCutsPower.POWER_ID -> Action.DamageThorns(power.amount)
                        JuggernautPower.POWER_ID -> {
                            if (AbstractDungeon.player.hasPower(FeelNoPainPower.POWER_ID)) {
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
                                                    ActionTarget.Single(monsterInfo.creature)
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
                                                    ActionTarget.Single(monsterInfo.creature)
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
        AbstractDungeon.player.getPower(PanachePower.POWER_ID)?.let { panachePower ->
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

    private fun MutableList<Action>.addOrbEvokeAction(
        orbsToEvoke: AbstractOrb,
        orbDamageMultiplier: Float,
        monster: AbstractMonster,
        aliveMonsterCount: Int,
    ) {
        when (orbsToEvoke.ID) {
            Lightning.ORB_ID -> {
                val damageAmount = orbsToEvoke.evokeAmount.times(orbDamageMultiplier).toInt()
                if (AbstractDungeon.player.hasPower(ElectroPower.POWER_ID)) {
                    add(Action.DamageThorns(damageAmount))
                } else {
                    add(Action.DamageThorns(0, damageAmount, ActionTarget.Random))
                }
            }

            Frost.ORB_ID -> {
                if (AbstractDungeon.player.hasPower(JuggernautPower.POWER_ID)) {
                    val juggernautPower = AbstractDungeon.player.getPower(JuggernautPower.POWER_ID)
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

    private fun AbstractCard.createIntentActions(
        monsterInfo: CreatureInfo<AbstractMonster>,
        monsterIndex: Int,
        aliveMonsterCount: Int,
    ): List<Action> {
        val monster = monsterInfo.creature
        calculateCardDamage(monster)
        val cardHitCount = getActionHitCount()
        val damagePerHit = getDamagePerHit(monsterIndex)
        val orbDamageMultiplier = if (monsterInfo.hasPower(LockOnPower.POWER_ID)) 1.5f else 1f

        return List(cardHitCount) {
            buildList {
                if (damagePerHit > 0) {
                    if (isRandomAttackCard && aliveMonsterCount > 1) {
                        add(Action.DamageNormal(0, damagePerHit, ActionTarget.Random))
                    } else {
                        if (cardID == Bane.ID) {
                            if (monsterInfo.hasPower(PoisonPower.POWER_ID)) {
                                add(Action.DamageNormal(damagePerHit, monster))
                                add(Action.DamageNormal(damagePerHit, monster))
                            } else {
                                add(
                                    Action.DamageNormal(damagePerHit, monster).withTags(Bane.ID)
                                )
                            }
                        } else {
                            add(Action.DamageNormal(damagePerHit, monster))
                        }
                    }
                }

                // Apply MarkPower if needed
                if (cardID == PressurePoints.ID) {
                    monsterInfo.powersAmount[MarkPower.POWER_ID]?.let { markAmount ->
                        add(Action.LoseHP(markAmount, monster))
                    }
                    add(Action.LoseHP(magicNumber, ActionTarget.Single(monster)))
                }

                // Apply player's Juggernaut power if needed
                AbstractDungeon.player.getPower(JuggernautPower.POWER_ID)?.let { juggernautPower ->
                    if (type == CardType.ATTACK && AbstractDungeon.player.hasPower(RagePower.POWER_ID)) {
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
                    if (AbstractDungeon.player.hasPower(AfterImagePower.POWER_ID)) {
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
                    if (block > 0 && baseBlock >= 0 && !isFakeGainBlockCard) {
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
                AbstractDungeon.player.getPower(SadisticPower.POWER_ID)?.let { sadisticPower ->
                    if (isDebuffCard) {
                        val shouldTrigger = when (cardID) {
                            GoForTheEyes.ID -> monster.intentBaseDmg >= 0
                            CrushJoints.ID -> AbstractDungeon.actionManager.cardsPlayedThisCombat.lastOrNull()?.type == CardType.SKILL
                            SashWhip.ID -> AbstractDungeon.actionManager.cardsPlayedThisCombat.lastOrNull()?.type == CardType.ATTACK
                            Indignation.ID -> AbstractDungeon.player.stance.ID == WrathStance.STANCE_ID

                            else -> !canGivePoison
                        }
                        if (shouldTrigger) {
                            repeat(getDebuffInstanceCount()) {
                                monsterInfo.ifNoArtifactPower {
                                    add(Action.DamageThorns(sadisticPower.amount, ActionTarget.Single(monster)))
                                }
                            }
                        }
                    }
                    if (AbstractDungeon.player.hasPower(WaveOfTheHandPower.POWER_ID)) {
                        if (type == CardType.ATTACK && AbstractDungeon.player.hasPower(RagePower.POWER_ID)) {
                            monsterInfo.ifNoArtifactPower {
                                add(Action.DamageThorns(sadisticPower.amount, ActionTarget.All))
                            }
                        }
                        if (block > 0 && baseBlock >= 0) {
                            monsterInfo.ifNoArtifactPower {
                                add(Action.DamageThorns(sadisticPower.amount, ActionTarget.All))
                            }
                        }
                    }
                }

                // Evoke orbs if needed
                AbstractDungeon.player.orbs.plus(getChannelingOrbs()).filterNot {
                    it is EmptyOrbSlot
                }.dropLast(AbstractDungeon.player.maxOrbs)
                    .forEach { evokedOrb ->
                        addOrbEvokeAction(evokedOrb, orbDamageMultiplier, monster, aliveMonsterCount)
                    }
                if (isOrbEvokeCard) {
                    AbstractDungeon.player.orbs.firstOrNull()?.let { orbsToEvoke ->
                        addOrbEvokeAction(orbsToEvoke, orbDamageMultiplier, monster, aliveMonsterCount)
                    }
                }

                // Apply poison if needed
                if (canGivePoison) {
                    when (cardID) {
                        BouncingFlask.ID -> {
                            repeat(magicNumber) {
                                monsterInfo.ifNoArtifactPower {
                                    if (aliveMonsterCount == 1) {
                                        var poisonAmount = getPoisonAmount(monster)
                                        if (AbstractDungeon.player.hasRelic(SneckoSkull.ID)) {
                                            poisonAmount += 1
                                        }
                                        add(Action.LoseHP(poisonAmount, monster, true).withPendingTag())
                                    }
                                    AbstractDungeon.player.getPower(SadisticPower.POWER_ID)?.let { sadisticPower ->
                                        if (aliveMonsterCount > 1) {
                                            add(Action.DamageThorns(0, sadisticPower.amount, ActionTarget.Random))
                                        } else {
                                            add(Action.DamageThorns(sadisticPower.amount, ActionTarget.Single(monster)))
                                        }
                                    }
                                }
                            }
                        }

                        else -> {
                            monsterInfo.ifNoArtifactPower {
                                var poisonAmount = getPoisonAmount(monster)
                                if (AbstractDungeon.player.hasRelic(SneckoSkull.ID)) {
                                    poisonAmount += 1
                                }
                                add(Action.LoseHP(poisonAmount, monster, true).withPendingTag())
                                AbstractDungeon.player.getPower(SadisticPower.POWER_ID)?.let { sadisticPower ->
                                    add(Action.DamageThorns(sadisticPower.amount, ActionTarget.Single(monster)))
                                }
                            }
                        }
                    }
                }
            }.asGroupedAction()
        }
    }
}
