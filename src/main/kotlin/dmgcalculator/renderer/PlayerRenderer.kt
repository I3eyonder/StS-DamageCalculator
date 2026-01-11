package dmgcalculator.renderer

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.megacrit.cardcrawl.cards.curses.Decay
import com.megacrit.cardcrawl.cards.curses.Regret
import com.megacrit.cardcrawl.cards.status.Burn
import com.megacrit.cardcrawl.dungeons.AbstractDungeon
import com.megacrit.cardcrawl.powers.CombustPower
import com.megacrit.cardcrawl.powers.ConstrictedPower
import com.megacrit.cardcrawl.powers.MetallicizePower
import com.megacrit.cardcrawl.powers.PlatedArmorPower
import com.megacrit.cardcrawl.powers.RegenPower
import com.megacrit.cardcrawl.powers.RegenerateMonsterPower
import com.megacrit.cardcrawl.relics.CloakClasp
import com.megacrit.cardcrawl.relics.Orichalcum
import dmgcalculator.entities.*
import dmgcalculator.util.*
import dmgcalculator.util.Utils.addToBottom
import dmgcalculator.util.Utils.addToTop

object PlayerRenderer {

    private var cachedMsg: String? = null

    fun render(sb: SpriteBatch, isPlayerTurn: Boolean) {
        if (AbstractDungeon.isScreenUp) return
        if (AbstractDungeon.getCurrMapNode() == null) return
        if (AbstractDungeon.getMonsters() == null) return
        if (AbstractDungeon.player == null) return

        val msg = if (isPlayerTurn) {
            getIncomingActions().calculateOutcome(
                CreatureInfo(AbstractDungeon.player)
            ).let { (worstOutcome, bestOutcome) ->
                buildMessage(worstOutcome, bestOutcome)
            }.also {
                cachedMsg = it
            }
        } else {
            cachedMsg.orEmpty()
        }

        if (msg.isNotEmpty()) {
            sb.renderFixedSizeMessage(
                msg,
                AbstractDungeon.player.hb.cX,
                AbstractDungeon.player.hb.cY + AbstractDungeon.player.hb.height
            )
        }
    }

    private fun buildMessage(worstOutcome: Outcome, bestOutcome: Outcome): String {
        // --- Build output message ---
        return buildString {
            // --- Taken damage ---
            append(
                "Take %s damage".format(
                    Range(worstOutcome.damage, bestOutcome.damage).sorted().colored("#FF0000")
                )
            )

            // --- Blocked amount ---
            if (worstOutcome.blocked == bestOutcome.blocked) {
                if (bestOutcome.blocked > 0) {
                    append("\n")
                        .append(
                            "(%s blocked)".format(
                                bestOutcome.blocked.toString().colored("#00FF00")
                            )
                        )
                }
            } else {
                append("\n")
                    .append(
                        "(%s blocked)".format(
                            Range(worstOutcome.blocked, bestOutcome.blocked).sorted().colored("#00FF00")
                        )
                    )
            }

            // --- Extra info ---
            if (worstOutcome.adjustHP == bestOutcome.adjustHP) {
                if (bestOutcome.adjustHP < 0) {
                    append("\n")
                    append(
                        "Lose extra %s HP".format(
                            bestOutcome.adjustHP.unaryMinus().toString().colored("#FF0000")
                        )
                    )
                } else if (bestOutcome.adjustHP > 0) {
                    append("\n")
                    append(
                        "Gain extra %s HP".format(
                            bestOutcome.adjustHP.toString().colored("#00FF00")
                        )
                    )
                }
            }

            // --- Remaining HP ---
            if (worstOutcome.remainHP == bestOutcome.remainHP) {
                if (bestOutcome.isDead) {
                    append("\n")
                        .append("DEAD".colored("#FF0000"))
                } else {
                    append("\n")
                        .append(
                            "%s HP remains".format(
                                bestOutcome.remainHP.toString().colored("#00BFFF")
                            )
                        )
                }
            } else {
                append("\n")
                    .append(
                        "%s HP remains".format(
                            Range(worstOutcome.remainHP, bestOutcome.remainHP).sorted()
                                .colored("#00BFFF", reversed = true)
                        )
                    )
            }
        }
    }

    private fun getIncomingActions(): List<Action> {
        // Resolve relics
        val relicEffect = buildList {
            AbstractDungeon.player.relics.forEach { relic ->
                when (relic.relicId) {
                    Orichalcum.ID -> {
                        if (AbstractDungeon.player.currentBlock == 0 || (relic as Orichalcum).trigger) {
                            addToTop(Action.GainBlock(6))
                        }
                    }

                    CloakClasp.ID -> {
                        if (!AbstractDungeon.player.hand.group.isEmpty()) {
                            addToBottom(Action.GainBlock(AbstractDungeon.player.hand.group.size))
                        }
                    }
                }
            }
        }

        // Resolve power effects pre hand
        val powerPreHandEffects = buildList {
            AbstractDungeon.player.powers.forEach { power ->
                when (power.ID) {
                    PlatedArmorPower.POWER_ID,
                    MetallicizePower.POWER_ID,
                        -> addToBottom(Action.GainBlock(power.amount.coerceAtLeast(0)))
                }
            }
        }

        // Resolve hand intent damages
        val handDamages = buildList {
            AbstractDungeon.player.hand.group.forEach { card ->
                when (card.cardID) {
                    Burn.ID -> addToBottom(Action.DamageThorns(card.magicNumber))
                    Decay.ID -> addToBottom(Action.DamageThorns(2))
                    Regret.ID -> addToBottom(Action.LoseHP(AbstractDungeon.player.hand.size()))
                }
            }
        }

        // Resolve power effects after hand
        val powerAfterHandEffects = buildList {
            AbstractDungeon.player.powers.forEach { power ->
                when (power.ID) {
                    ConstrictedPower.POWER_ID -> addToBottom(Action.DamageThorns(power.amount))
                    CombustPower.POWER_ID -> addToBottom(Action.LoseHP(power.getPrivateField("hpLoss")))
                    RegenerateMonsterPower.POWER_ID -> addToBottom(Action.GainHP(power.amount))
                    RegenPower.POWER_ID -> addToTop(Action.GainHP(power.amount))
                }
            }
        }

        // Resolve monster intent damages
        val monsterIntentDamages = AbstractDungeon.getMonsters().aliveMonsters.getAttackIntentActions()

        return relicEffect + powerPreHandEffects + handDamages + powerAfterHandEffects + monsterIntentDamages
    }

}
