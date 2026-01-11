package dmgcalculator.renderer

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.megacrit.cardcrawl.dungeons.AbstractDungeon
import dmgcalculator.entities.*
import dmgcalculator.util.*

object MonsterRenderer {

    fun render(sb: SpriteBatch) {
        if (AbstractDungeon.isScreenUp) return
        if (AbstractDungeon.getCurrMapNode() == null) return

        val player = AbstractDungeon.player
        val hoveringCard = player.hoveredCard?.makeStatEquivalentCopy()?.apply {
            applyPowers()
        }
        val hoveredMonster = player.getHoveredMonster()
        val aliveMonstersIndexed = AbstractDungeon.getMonsters().aliveMonstersIndexed
        val aliveMonsterCount = aliveMonstersIndexed.size
        val msgBuilder = StringBuilder()

        aliveMonstersIndexed.forEach { (index, monster) ->
            msgBuilder.clear()
            val creatureInfo = CreatureInfo(monster)
            val (worstCardOutcome, bestCardOutcome) = hoveringCard?.getIntentActions(monster, index, aliveMonsterCount)
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

    private fun buildMessage(
        msgBuilder: StringBuilder,
        worstOutcome: Outcome,
        bestOutcome: Outcome,
        showRemainHP: Boolean,
    ) {
        // --- Deal damage ---
        msgBuilder.append(
            "Take %s damage".format(
                Range(worstOutcome.damage, bestOutcome.damage).sorted().colored("#FF0000")
            )
        )

        // --- Blocked amount ---
        if (worstOutcome.blocked == bestOutcome.blocked) {
            if (bestOutcome.blocked > 0) {
                msgBuilder.append("\n")
                    .append(
                        "(%s blocked)".format(
                            bestOutcome.blocked.toString().colored("#00FF00")
                        )
                    )
            }
        } else {
            msgBuilder
                .append("\n").append(
                    "(%s blocked)".format(
                        Range(worstOutcome.blocked, bestOutcome.blocked).sorted().colored("#00FF00")
                    )
                )
        }

        // --- Extra info ---
        if (worstOutcome.adjustHP == bestOutcome.adjustHP) {
            if (bestOutcome.adjustHP < 0) {
                msgBuilder.append("\n")
                    .append(
                        "Lose extra %s HP".format(
                            bestOutcome.adjustHP.unaryMinus().toString().colored("#FF0000")
                        )
                    )
            } else if (bestOutcome.adjustHP > 0) {
                msgBuilder.append("\n")
                    .append(
                        "Gain extra %s HP".format(
                            bestOutcome.adjustHP.toString().colored("#00FF00")
                        )
                    )
            }
        }

        // --- Remaining HP ---
        if (showRemainHP) {
            if (worstOutcome.remainHP == bestOutcome.remainHP) {
                if (bestOutcome.isDead) {
                    msgBuilder.append("\n")
                        .append("DEAD".colored("#FF0000"))
                } else {
                    msgBuilder.append("\n")
                        .append(
                            "%s HP remains".format(
                                bestOutcome.remainHP.toString().colored("#00BFFF")
                            )
                        )
                }
            } else {
                msgBuilder.append("\n")
                    .append(
                        "%s HP remains".format(
                            Range(worstOutcome.remainHP, bestOutcome.remainHP).sorted()
                                .colored("#00BFFF", reversed = true)
                        )
                    )
            }
        }
    }
}
