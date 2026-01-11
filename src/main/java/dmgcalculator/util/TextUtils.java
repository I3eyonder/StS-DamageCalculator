package dmgcalculator.util;

public class TextUtils {

    public static String formatTextColor(String text, String hexColor) {
        if (hexColor.startsWith("#")) {
            return "<" + hexColor + ">" + text + "</#>";
        } else {
            return "<#" + hexColor + ">" + text + "</#>";
        }
    }
}
