package dmgcalculator.util;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import java.util.List;

import dmgcalculator.renderer.TextSegment;

public class RendererUtils {

    /**
     * Renders multi‑line text that supports inline hex‑color markup on each line.
     * <p>
     * The input string may contain newline characters (<code>\n</code>) to indicate
     * explicit line breaks. Each line is processed independently using the same
     * hex‑color markup rules as {@link #renderFormattedText}, allowing segments
     * of text to be wrapped in tags of the form:
     * <pre>
     *     &lt;#RRGGBB&gt;colored text&lt;/#&gt;
     * </pre>
     * Each opening tag applies the specified color (in standard 6‑digit hex), and the
     * closing tag &lt;/#&gt; restores the color back to white. Any text outside of tags is
     * rendered in white.
     * <p>
     * This method:
     * <ul>
     *     <li>Splits the input into lines using <code>\n</code></li>
     *     <li>Computes the total vertical height of the block</li>
     *     <li>Vertically centers the entire block around the given Y coordinate</li>
     *     <li>Renders each line using {@link #renderFormattedText}</li>
     * </ul>
     * <p>
     * This method does not perform automatic word‑wrapping; only explicit newline
     * characters create new lines.
     *
     * @param sb          the SpriteBatch used for rendering
     * @param font        the BitmapFont used to draw the text
     * @param x           the horizontal center position for each rendered line
     * @param y           the vertical center of the entire multi‑line block
     * @param markup      the input string containing plain text and optional hex‑color tags
     * @param lineSpacing the vertical spacing (in pixels) between consecutive lines
     */
    public static void renderFormattedMultiline(
            SpriteBatch sb, BitmapFont font, float x, float y, String markup, float lineSpacing) {

        // Split into lines by '\n'
        String[] lines = markup.split("\n");

        // Starting Y so the whole block is vertically centered
        float totalHeight = lines.length * font.getCapHeight() + (lines.length - 1) * lineSpacing;
        float cy = y + totalHeight / 2f;

        for (String line : lines) {
            renderFormattedText(sb, font, x, cy, line);
            cy -= font.getCapHeight() + lineSpacing;
        }
    }


    /**
     * Renders a single line of text that supports inline hex‑color markup.
     * <p>
     * The markup format allows segments of the text to be wrapped in tags of the form:
     * <pre>
     *     &lt;#RRGGBB&gt;colored text&lt;/#&gt;
     * </pre>
     * Each opening tag applies the specified color (in standard 6‑digit hex), and the closing
     * tag &lt;/#&gt; restores the color back to white. Any text outside of tags is rendered in white.
     * <p>
     * This method:
     * <ul>
     *     <li>Parses the markup into ordered text segments with associated colors</li>
     *     <li>Measures the total width of all segments</li>
     *     <li>Horizontally centers the entire line around the given (x, y) coordinate</li>
     *     <li>Draws each segment left‑to‑right using its assigned color</li>
     * </ul>
     * <p>
     * Only single‑line rendering is supported. For multi‑line text, split the input on newline
     * characters and call this method once per line.
     *
     * @param sb     the SpriteBatch used for rendering
     * @param font   the BitmapFont used to draw the text
     * @param x      the horizontal center position of the full rendered line
     * @param y      the baseline Y coordinate for the text
     * @param markup the input string containing plain text and optional hex‑color tags
     */
    public static void renderFormattedText(SpriteBatch sb, BitmapFont font, float x, float y, String markup) {
        List<TextSegment> segments = TextSegment.parseHexMarkup(markup);

        // 1) Measure total width
        float totalWidth = 0f;
        for (TextSegment seg : segments) {
            GlyphLayout gl = new GlyphLayout(font, seg.text);
            totalWidth += gl.width;
        }

        // 2) Compute starting x so the whole line is centered once
        float cx = x - totalWidth / 2f;

        // 3) Draw each segment in order, left to right
        for (TextSegment seg : segments) {
            font.setColor(seg.color);
            GlyphLayout gl = new GlyphLayout(font, seg.text);
            font.draw(sb, gl, cx, y);

            cx += gl.width; // move cursor to the right
        }

        font.setColor(Color.WHITE); // reset
    }
}
