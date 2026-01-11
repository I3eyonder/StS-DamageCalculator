package dmgcalculator.renderer;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import dmgcalculator.util.RendererUtils;
import dmgcalculator.util.TextUtils;
import dmgcalculator.util.Utils;

public class CalculateIncomingDamageRenderer {

    private static String cachedMsg;

    public static void render(SpriteBatch sb) {
        if (AbstractDungeon.isScreenUp) return;
        if (AbstractDungeon.getCurrMapNode() == null) return;
        if (AbstractDungeon.getMonsters() == null) return;
        if (AbstractDungeon.player == null) return;

        String msg;
        if (Utils.isPlayerTurn()) {
            msg = buildIncomingDamageMessage();
            cachedMsg = msg;
        } else {
            msg = cachedMsg;
        }

        BitmapFont font = FontHelper.cardTitleFont;

        float oldScale = font.getData().scaleX;
        font.getData().setScale(1.0f); // lock size

        RendererUtils.renderFormattedMultiline(
                sb,
                font,
                AbstractDungeon.player.hb.cX,
                AbstractDungeon.player.hb.cY + AbstractDungeon.player.hb.height,
                msg,
                8f
        );

        font.getData().setScale(oldScale); // restore
    }

    private static String buildIncomingDamageMessage() {
        int currentHealth = AbstractDungeon.player.currentHealth;
        int[] blockedDamage = new int[1];
        int netIncomingDamage = getTotalNetIncomingDamage(blockedDamage);
        int remainHP = Math.max(0, currentHealth - netIncomingDamage);

        String dmgColor;
        if (netIncomingDamage == 0) {
            dmgColor = "#00FF00";
        } else if (remainHP > 0) {
            dmgColor = "#FF4444";
        } else {
            dmgColor = "#FF0000";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Take %s damage", TextUtils.formatTextColor(String.valueOf(netIncomingDamage), dmgColor)));
        if (blockedDamage[0] > 0) {
            sb.append(" ")
                    .append(String.format("(%s blocked)", TextUtils.formatTextColor(String.valueOf(blockedDamage[0]), "#00FF00")));
        }
        if (remainHP > 0) {
            sb.append("\n")
                    .append(String.format("%s HP remains", TextUtils.formatTextColor(String.valueOf(remainHP), "#00BFFF")));
        } else {
            sb.append("\n")
                    .append(String.format("%s", TextUtils.formatTextColor("DEAD", "#FF0000")));
        }

        return sb.toString();
    }

    private static int getTotalNetIncomingDamage(int[] blockedDamage) {
        int netIncomingDamage = getTotalRawIncomingDamage();
        blockedDamage[0] = calculateBlock();
        netIncomingDamage -= blockedDamage[0];
        return Math.max(0, netIncomingDamage);
    }

    private static int calculateBlock() {
        int currentBlock = AbstractDungeon.player.currentBlock;
        return currentBlock;
    }

    private static int getTotalRawIncomingDamage() {
        int total = 0;

        for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
            if (!m.isDeadOrEscaped() && Utils.isAttackingIntent(m.intent)) {
                total += getIntentDamage(m);
            }
        }

        return total;
    }

    private static int getIntentDamage(AbstractMonster m) {
        int hits = Utils.getIntentMultiAmt(m);
        int dmgPerHit = m.getIntentDmg();
        return hits * dmgPerHit;
    }
}
