package dmgcalculator.renderer

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.megacrit.cardcrawl.cards.AbstractCard
import com.megacrit.cardcrawl.cards.curses.Decay
import com.megacrit.cardcrawl.cards.curses.Regret
import com.megacrit.cardcrawl.cards.red.Rage
import com.megacrit.cardcrawl.cards.status.Burn
import com.megacrit.cardcrawl.dungeons.AbstractDungeon
import com.megacrit.cardcrawl.powers.*
import com.megacrit.cardcrawl.relics.CloakClasp
import com.megacrit.cardcrawl.relics.Orichalcum
import com.megacrit.cardcrawl.relics.OrnamentalFan
import dmgcalculator.entities.Action
import dmgcalculator.entities.CreatureInfo
import dmgcalculator.entities.asGroupedAction
import dmgcalculator.entities.calculateOutcome
import dmgcalculator.entities.flatten
import dmgcalculator.util.*
import dmgcalculator.util.Utils.addToBottom
import dmgcalculator.util.Utils.addToTop

object PlayerRenderer {

    private var cachedMsg: String? = null

    fun render(sb: SpriteBatch, hoveredCard: AbstractCard?, isPlayerTurn: Boolean) {
        val msg = if (isPlayerTurn) {
            getIntentActions(hoveredCard).calculateOutcome(
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

    private fun AbstractCard.createIntentActions(): List<Action> = buildList {
        val player = AbstractDungeon.player
        if (type == AbstractCard.CardType.ATTACK && player.hasPower(RagePower.POWER_ID)) {
            add(Action.GainBlock((player.getPower(RagePower.POWER_ID).amount)))
        }
        if (block > 0) {
            add(Action.GainBlock(block))
        }
    }

    private fun getIntentActions(hoveredCard: AbstractCard?): List<Action> {
        //Resolve card actions
        val cardActions = hoveredCard?.let { hoveringCard ->
            val cardHitCount = hoveringCard.getHitCount()
            val baseAction = when {
                cardHitCount > 1 -> {
                    List(cardHitCount) {
                        hoveringCard.createIntentActions().asGroupedAction()
                    }.asGroupedAction()
                }

                else -> {
                    hoveringCard.createIntentActions().asGroupedAction()
                }
            }
            val actions = mutableListOf(baseAction)
            AbstractDungeon.player.powers.forEach { power ->
                when (power.ID) {
                    DoubleTapPower.POWER_ID -> {
                        if (hoveringCard.type == AbstractCard.CardType.ATTACK) {
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
            actions.toList()
        } ?: emptyList()

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

                    OrnamentalFan.ID -> {
                        if (hoveredCard?.type == AbstractCard.CardType.ATTACK && (relic.counter + 1) % 3 == 0) {
                            addToBottom(Action.GainBlock(4))
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

        return (cardActions + relicEffect + powerPreHandEffects + handDamageActions + powerAfterHandEffects + monsterAttackIntentActions).flatten()
    }

}
