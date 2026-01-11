package dmgcalculator.renderer

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.megacrit.cardcrawl.cards.curses.Decay
import com.megacrit.cardcrawl.cards.curses.Regret
import com.megacrit.cardcrawl.cards.status.Burn
import com.megacrit.cardcrawl.dungeons.AbstractDungeon
import com.megacrit.cardcrawl.powers.*
import com.megacrit.cardcrawl.relics.CloakClasp
import com.megacrit.cardcrawl.relics.Orichalcum
import dmgcalculator.entities.Action
import dmgcalculator.entities.CreatureInfo
import dmgcalculator.entities.calculateOutcome
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
                buildString {
                    buildOutcomeMessage(worstOutcome, bestOutcome)
                }
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

        // Resolve hand intent actions
        val handDamageActions = buildList {
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
                    CombustPower.POWER_ID -> power.getPrivateField<Int>("hpLoss")?.let {
                        addToBottom(Action.LoseHP(it))
                    }

                    RegenerateMonsterPower.POWER_ID -> addToBottom(Action.GainHP(power.amount))
                    RegenPower.POWER_ID -> addToTop(Action.GainHP(power.amount))
                }
            }
        }

        // Resolve monster attack intent
        val monsterAttackIntentActions = AbstractDungeon.getMonsters().aliveMonsters.getAttackIntentActions()

        return relicEffect + powerPreHandEffects + handDamageActions + powerAfterHandEffects + monsterAttackIntentActions
    }

}
