package dmgcalculator.util;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.green.Eviscerate;
import com.megacrit.cardcrawl.cards.green.RiddleWithHoles;
import com.megacrit.cardcrawl.cards.green.Skewer;
import com.megacrit.cardcrawl.cards.purple.Tantrum;
import com.megacrit.cardcrawl.cards.red.*;
import com.megacrit.cardcrawl.cards.tempCards.Expunger;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.CombustPower;
import com.megacrit.cardcrawl.powers.TheBombPower;
import com.megacrit.cardcrawl.powers.watcher.OmegaPower;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.relics.ChemicalX;
import com.megacrit.cardcrawl.relics.StoneCalendar;
import com.megacrit.cardcrawl.ui.panels.EnergyPanel;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class Utils {

    private static final List<String> RANDOM_ATTACK_CARDS;

    static {
        RANDOM_ATTACK_CARDS = new ArrayList<>();
        RANDOM_ATTACK_CARDS.add(SwordBoomerang.ID);
    }

    public static boolean isRandomAttackCard(String cardId) {
        return RANDOM_ATTACK_CARDS.contains(cardId);
    }

    public static int getCardHitCount(AbstractCard card) {
        switch (card.cardID) {
            case TwinStrike.ID:
                return 2;
            case RiddleWithHoles.ID:
                return 5;
            case Pummel.ID:
            case Tantrum.ID:
            case SwordBoomerang.ID:
                return card.magicNumber;
            case Whirlwind.ID:
            case Skewer.ID:
            case Expunger.ID: {
                int hits = EnergyPanel.totalCount;
                if (AbstractDungeon.player.hasRelic(ChemicalX.ID)) {
                    hits += 2;
                }
                return hits;
            }
            case Eviscerate.ID:
                return 3;
            case FiendFire.ID:
                return AbstractDungeon.player.hand.size() - 1;
            default:
                return 1;
        }
    }

    public static int getIntentMultiAmt(AbstractMonster m) {
        try {
            Field f = AbstractMonster.class.getDeclaredField("intentMultiAmt");
            f.setAccessible(true);
            int value = (int) f.get(m);
            return value > 0 ? value : 1; // fallback to 1 if -1 or 0
        } catch (Exception e) {
            return 1;
        }
    }

    public static boolean isAttackingIntent(AbstractMonster.Intent intent) {
        return intent == AbstractMonster.Intent.ATTACK ||
                intent == AbstractMonster.Intent.ATTACK_BUFF ||
                intent == AbstractMonster.Intent.ATTACK_DEBUFF ||
                intent == AbstractMonster.Intent.ATTACK_DEFEND;
    }

    public static boolean isPlayerTurn() {
        return AbstractDungeon.actionManager != null
                && !AbstractDungeon.actionManager.turnHasEnded;
    }

    public static int getAliveMonsterNumber(List<AbstractMonster> monsters) {
        int aliveMonsterNumber = 0;
        for (AbstractMonster m : monsters) {
            if (!m.isDeadOrEscaped()) {
                aliveMonsterNumber++;
            }
        }
        return aliveMonsterNumber;
    }

    public static int getNetDamageAmount(int rawDamageAmount, int blockAmount) {
        return Math.max(0, rawDamageAmount - blockAmount);
    }

    public static int getBlockedAmount(int damageAmount, int blockAmount) {
        return Math.min(damageAmount, blockAmount);
    }

    public static boolean canCardDealDamage(AbstractCard card) {
        if (card == null) {
            return false;
        }
        return card.baseDamage >= 0;
    }

    public static boolean hasEndTurnDamage(AbstractCreature creature) {
        if (creature == null) return false;
        ArrayList<AbstractPower> powers = creature.powers;
        for (AbstractPower power : powers) {
            if (CombustPower.POWER_ID.equals(power.ID)) return true;
            if (OmegaPower.POWER_ID.equals(power.ID)) return true;
            if (power.ID.contains(TheBombPower.POWER_ID) && power.amount == 1) return true;
        }
        if (creature instanceof AbstractPlayer) {
            AbstractRelic stoneCalendar = ((AbstractPlayer) creature).getRelic(StoneCalendar.ID);
            return stoneCalendar != null && stoneCalendar.counter == 7;
        }
        return false;
    }
}
