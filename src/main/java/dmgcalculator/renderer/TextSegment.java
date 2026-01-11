package dmgcalculator.renderer;

import com.badlogic.gdx.graphics.Color;

import java.util.ArrayList;
import java.util.List;

public class TextSegment {
    public final String text;
    public final Color color;

    public TextSegment(String text, Color color) {
        this.text = text;
        this.color = color;
    }

    public static List<TextSegment> parseHexMarkup(String input) {
        List<TextSegment> result = new ArrayList<>();
        Color currentColor = Color.WHITE;

        int i = 0;
        while (i < input.length()) {

            // Detect closing tag: </#>
            if (input.startsWith("</#>", i)) {
                currentColor = Color.WHITE;
                i += 4; // skip "</#>"
                continue;
            }

            // Detect opening tag: <#RRGGBB>
            if (input.startsWith("<#", i)) {
                int end = input.indexOf('>', i);
                if (end != -1) {
                    String hex = input.substring(i + 2, end).trim();

                    try {
                        currentColor = Color.valueOf(hex);
                    } catch (Exception e) {
                        currentColor = Color.WHITE;
                    }

                    i = end + 1;
                    continue;
                }
            }

            // Normal text until next tag
            int nextTag = input.indexOf("<", i);
            if (nextTag == -1) nextTag = input.length();

            String text = input.substring(i, nextTag);
            if (!text.isEmpty()) {
                result.add(new TextSegment(text, currentColor));
            }

            i = nextTag;
        }

        return result;
    }



}
