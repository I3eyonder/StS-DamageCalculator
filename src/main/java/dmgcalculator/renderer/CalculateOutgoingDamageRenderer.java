package dmgcalculator.renderer;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.CombustPower;
import com.megacrit.cardcrawl.powers.TheBombPower;
import com.megacrit.cardcrawl.powers.watcher.OmegaPower;

import java.util.ArrayList;

import dmgcalculator.entities.Range;
import dmgcalculator.util.RendererUtils;
import dmgcalculator.util.TextUtils;
import dmgcalculator.util.Utils;

public class CalculateOutgoingDamageRenderer {

    private static final Range cardIntentDamageRange = new Range();
    private static final Range cardNetDamageRange = new Range();
    private static final Range endTurnNetDamageRange = new Range();
    private static final Range blockedAmountRange = new Range();
    private static final Range remainBlockAmountRange = new Range();
    private static final Range remainHPRange = new Range();

    public static void render(SpriteBatch sb, AbstractCard hoveredCard) {
        if (AbstractDungeon.isScreenUp) return;
        if (AbstractDungeon.getCurrMapNode() == null) return;
        AbstractCard tmpHoveredCard = null;
        if (Utils.canCardDealDamage(hoveredCard)) {
            tmpHoveredCard = hoveredCard.makeStatEquivalentCopy();
            tmpHoveredCard.applyPowers();
        }
        ArrayList<AbstractMonster> monsters = AbstractDungeon.getMonsters().monsters;
        int aliveMonsterNumber = Utils.getAliveMonsterNumber(monsters);
        for (int i = 0; i < monsters.size(); i++) {
            AbstractMonster monster = monsters.get(i);
            if (!monster.isDeadOrEscaped()) {
                initData(monster);
                StringBuilder msgBuilder = new StringBuilder();
                if (tmpHoveredCard != null) {
                    tmpHoveredCard.calculateCardDamage(monster);
                    int damagePerHit = getDamagePerHit(tmpHoveredCard, i);
                    updateCardIntentDamageRange(tmpHoveredCard, aliveMonsterNumber, damagePerHit);
                    updateCardNetDamageAndBlocked();
                    updateRemainHPAndBlock(cardNetDamageRange);
                    buildDamageInfoMessage(msgBuilder, cardNetDamageRange);
                } else {
                    cardIntentDamageRange.set(0);
                    updateCardNetDamageAndBlocked();
                }
                if (remainHPRange.getMax() > 0) {
                    if (Utils.hasEndTurnDamage(AbstractDungeon.player)) {
                        updateEndTurnNetDamageAndBlocked();
                        updateRemainHPAndBlock(endTurnNetDamageRange);
                        if (tmpHoveredCard != null) {
                            msgBuilder.append("\n");
                        }
                        msgBuilder.append("--End Turn--\n");
                        buildDamageInfoMessage(msgBuilder, endTurnNetDamageRange);
                    }
                }
                BitmapFont font = FontHelper.cardTitleFont;
                float oldScale = font.getData().scaleX;
                font.getData().setScale(1.0f); // lock size
                RendererUtils.renderFormattedMultiline(sb, font, monster.hb.cX, monster.hb.cY + monster.hb.height,
                        msgBuilder.toString(), 8f);
                font.getData().setScale(oldScale); // restore
            }
        }
    }

    private static void buildDamageInfoMessage(StringBuilder msgBuilder, Range cardNetDamageRange) {
        if (cardNetDamageRange.isConstantsValue()) {
            msgBuilder.append(String.format("Deal %s damage",
                    TextUtils.formatTextColor(String.valueOf(cardNetDamageRange.getValue()), "#00FF00")));
        } else {
            msgBuilder.append(String.format("Deal %s damage",
                    TextUtils.formatTextColor(cardNetDamageRange.getMin() + "~" + cardNetDamageRange.getMax(), "#00FF00")));
        }
        if (blockedAmountRange.isConstantsValue()) {
            if (blockedAmountRange.getValue() > 0) {
                msgBuilder.append(" ")
                        .append(String.format("(%s blocked)", TextUtils.formatTextColor(String.valueOf(blockedAmountRange.getValue()), "#00FF00")));
            }
        } else {
            msgBuilder.append(" ")
                    .append(String.format("(%s blocked)", TextUtils.formatTextColor(blockedAmountRange.getMin() + "~" + blockedAmountRange.getMax(), "#00FF00")));
        }
        if (remainHPRange.isConstantsValue()) {
            if (remainHPRange.getValue() <= 0) {
                msgBuilder.append("\n")
                        .append(TextUtils.formatTextColor("DEAD", "#FF0000"));
            } else {
                msgBuilder.append("\n")
                        .append(String.format("%s HP remains",
                                TextUtils.formatTextColor(String.valueOf(remainHPRange.getValue()), "#00BFFF")));
            }
        } else {
            msgBuilder.append("\n")
                    .append(String.format("%s HP remains",
                            TextUtils.formatTextColor(remainHPRange.getMax() + "~" + remainHPRange.getMin(), "#00BFFF")));
        }
    }

    private static int getEndTurnDamageAmount() {
        int endTurnDamage = 0;
        for (AbstractPower power : AbstractDungeon.player.powers) {
            if (CombustPower.POWER_ID.equals(power.ID)) {
                endTurnDamage += power.amount;
            } else if (OmegaPower.POWER_ID.equals(power.ID)) {
                endTurnDamage += power.amount;
            } else if (power.ID.contains(TheBombPower.POWER_ID) && power.amount == 1) {
                endTurnDamage += 40; //TODO get power.damage
            }
        }
        return endTurnDamage;
    }

    private static void updateEndTurnNetDamageAndBlocked() {
        int rawEndTurnDamageAmount = getEndTurnDamageAmount();
        endTurnNetDamageRange.set(Utils.getNetDamageAmount(rawEndTurnDamageAmount, remainBlockAmountRange.getMax()),
                Utils.getNetDamageAmount(rawEndTurnDamageAmount, remainBlockAmountRange.getMin()));
        blockedAmountRange.set(Utils.getBlockedAmount(rawEndTurnDamageAmount, remainBlockAmountRange.getMin()),
                Utils.getBlockedAmount(rawEndTurnDamageAmount, remainBlockAmountRange.getMax()));
    }

    private static void updateCardNetDamageAndBlocked() {
        cardNetDamageRange.set(Utils.getNetDamageAmount(cardIntentDamageRange.getMin(), remainBlockAmountRange.getMax()),
                Utils.getNetDamageAmount(cardIntentDamageRange.getMax(), remainBlockAmountRange.getMin()));
        blockedAmountRange.set(Utils.getBlockedAmount(cardIntentDamageRange.getMin(), remainBlockAmountRange.getMin()),
                Utils.getBlockedAmount(cardIntentDamageRange.getMax(), remainBlockAmountRange.getMax()));
    }

    private static void updateRemainHPAndBlock(Range damageRange) {
        remainHPRange.set(Math.max(0, remainHPRange.getMin() - damageRange.getMax()),
                Math.max(0, remainHPRange.getMax() - damageRange.getMin()));
        remainBlockAmountRange.set(Math.max(0, remainBlockAmountRange.getMax() - blockedAmountRange.getMax()),
                Math.max(0, remainBlockAmountRange.getMax() - blockedAmountRange.getMin()));
    }

    private static void initData(AbstractMonster monster) {
        remainHPRange.set(monster.currentHealth);
        remainBlockAmountRange.set(monster.currentBlock);
    }

    private static void updateCardIntentDamageRange(AbstractCard tmpCard, int monsterNum, int damagePerHit) {
        int hitCount = Utils.getCardHitCount(tmpCard);
        if (Utils.isRandomAttackCard(tmpCard.cardID) && monsterNum > 1) {
            cardIntentDamageRange.set(0, damagePerHit * hitCount);
        } else {
            cardIntentDamageRange.set(damagePerHit * hitCount);
        }
    }

    private static int getDamagePerHit(AbstractCard tmpCard, int i) {
        if (tmpCard.multiDamage != null) {
            return tmpCard.multiDamage[i];
        } else {
            return tmpCard.damage;
        }
    }
}
