package dmgcalculator.renderer

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.megacrit.cardcrawl.cards.AbstractCard
import com.megacrit.cardcrawl.dungeons.AbstractDungeon
import com.megacrit.cardcrawl.monsters.AbstractMonster
import com.megacrit.cardcrawl.powers.*
import com.megacrit.cardcrawl.powers.watcher.VigorPower
import dmgcalculator.entities.*
import dmgcalculator.util.*
import dmgcalculator.util.Utils.addToBottom
import dmgcalculator.util.Utils.replacesWith

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
            val (worstCardOutcome, bestCardOutcome) = hoveredCard?.getIntentActions(monster, index, aliveMonsterCount)
                ?.run {
                    if (hoveredMonster != null) {
                        this.filter { action ->
                            when (action) {
                                is Action.DamageNormal -> {
                                    if (action.target is ActionTarget.Single) {
                                        action.target.target == hoveredMonster
                                    } else {
                                        true
                                    }
                                }

                                is Action.DamageThorns -> {
                                    if (action.target is ActionTarget.Single) {
                                        action.target.target == hoveredMonster
                                    } else {
                                        true
                                    }
                                }

                                else -> true
                            }
                        }
                    } else {
                        this
                    }
                }?.calculateOutcome(creatureInfo) ?: (null to null)
            if (worstCardOutcome != null && bestCardOutcome != null) {
                val showCardRemainHP = bestCardOutcome.isDead || !player.hasEndTurnDamage()
                msgBuilder.buildOutcomeMessage(
                    worstCardOutcome,
                    bestCardOutcome,
                    showCardRemainHP
                )
                if (!showCardRemainHP) {
                    msgBuilder.append("\n")
                        .append("--End Turn--".colored("#FCBA03"))
                        .append("\n")
                    val endTurnIntentActions = player.getEndTurnIntentActions()
                    val worstEndTurnCalculatedOutcome = endTurnIntentActions.calculateWorstOutcome(
                        creatureInfo.copy(
                            remainHP = worstCardOutcome.remainHP,
                            remainBlock = worstCardOutcome.remainBlock,
                            remainBuffer = worstCardOutcome.remainBuffer,
                            hasCurlUpPower = worstCardOutcome.hasCurlUpPower
                        )
                    )
                    val bestEndTurnCalculatedOutcome = endTurnIntentActions.calculateBestOutcome(
                        creatureInfo.copy(
                            remainHP = bestCardOutcome.remainHP,
                            remainBlock = bestCardOutcome.remainBlock,
                            remainBuffer = bestCardOutcome.remainBuffer,
                            hasCurlUpPower = bestCardOutcome.hasCurlUpPower
                        )
                    )
                    msgBuilder.buildOutcomeMessage(
                        worstEndTurnCalculatedOutcome,
                        bestEndTurnCalculatedOutcome,
                    )
                }
            } else if (player.hasEndTurnDamage()) {
                msgBuilder.append("--End Turn--".colored("#FCBA03"))
                    .append("\n")
                val endTurnIntentActions = player.getEndTurnIntentActions()
                val (worstEndTurnOutcome, bestEndTurnOutcome) = endTurnIntentActions.calculateOutcome(
                    creatureInfo
                )
                msgBuilder.buildOutcomeMessage(
                    worstEndTurnOutcome,
                    bestEndTurnOutcome,
                )
            }
            val msg = msgBuilder.toString()
            if (msg.isNotEmpty()) {
                sb.renderFixedSizeMessage(msgBuilder.toString(), monster.hb.cX, monster.hb.cY + monster.hb.height)
            }
        }
    }

    private fun AbstractCard.getIntentActions(
        monster: AbstractMonster,
        monsterIndex: Int,
        aliveMonsterCount: Int,
    ): List<Action> {
        val baseAction = createIntentAction(monster, monsterIndex, aliveMonsterCount)
        val actions = mutableListOf(baseAction)
        val player = AbstractDungeon.player

        // Apply powers that modify action here if needed
        fun createExtraAttackAction(): Action {
            val monsterTemporayPowers = buildList {
                if (canGiveVulnearable && !monster.hasPower(VulnerablePower.POWER_ID)) {
                    add(VulnerablePower(monster, 1, false))
                }
            }
            val playerTemporaryRemoveAmountPowers = buildList {
                if (player.hasPower(VigorPower.POWER_ID)) {
                    add(VigorPower.POWER_ID)
                }
            }
            return if (monsterTemporayPowers.isNotEmpty() || playerTemporaryRemoveAmountPowers.isNotEmpty()) {
                monster.applyTemporaryPowers(monsterTemporayPowers) {
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
                    if (type == AbstractCard.CardType.ATTACK) {
                        actions.addToBottom(createExtraAttackAction())
                    } else if (power.ID == DuplicationPower.POWER_ID) {
                        actions.addToBottom(baseAction)
                    }
                }

                EchoPower.POWER_ID -> {
                    val cardsDoubledThisTurn = power.getPrivateField<Int>("cardsDoubledThisTurn") ?: 0
                    if (power.amount > 0 &&
                        AbstractDungeon.actionManager.cardsPlayedThisTurn.size + 1 - cardsDoubledThisTurn <= power.amount
                    ) {
                        if (type == AbstractCard.CardType.ATTACK) {
                            actions.addToBottom(createExtraAttackAction())
                        } else {
                            actions.addToBottom(baseAction)
                        }
                    }
                }
            }
        }

        // Apply monster debuff and player buff if needed
        actions.replacesWith { originActions ->
            originActions.map { action ->
                val monsterExtraActions = monster.powers.mapNotNull { power ->
                    when (power.ID) {
                        ChokePower.POWER_ID -> Action.LoseHP(power.amount)
                        else -> null
                    }
                }
                val playerExtraActions = player.powers.mapNotNull { power ->
                    when (power.ID) {
                        ThousandCutsPower.POWER_ID -> Action.DamageThorns(power.amount)
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
                    buildList {
                        for (action in originActions) {
                            add(action)
                            remaining--
                            if (remaining == 0) {
                                // insert an extra action when countdown reaches zero, then reset to 5
                                add(Action.DamageThorns(panacheDamage))
                                remaining = PanachePower.CARD_AMT
                            }
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
        return if (type == AbstractCard.CardType.ATTACK) {
            calculateCardDamage(monster)
            val damagePerHit = getDamagePerHit(monsterIndex)
            val cardHitCount = getHitCount()
            when {
                cardHitCount > 1 -> {
                    if (isRandomAttackCard && aliveMonsterCount > 1) {
                        List(cardHitCount) {
                            Action.DamageNormal(0, damagePerHit, monster)
                        }.asGroupedAction()
                    } else {
                        List(cardHitCount) {
                            Action.DamageNormal(damagePerHit, monster)
                        }.asGroupedAction()
                    }
                }

                cardHitCount == 1 -> {
                    Action.DamageNormal(damagePerHit, monster)
                }

                else -> {
                    Action.NoAction
                }
            }
        } else {
            Action.NoAction
        }
    }
}
