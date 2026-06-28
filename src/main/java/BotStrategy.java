import java.util.ArrayList;

/**
 * Bot decision-making: which card to play and which color to call.
 *
 * This class knows about card ranks and the current game state (up-card,
 * called color) but contains no I/O.  Replacing or improving bot behaviour
 * only requires changing this class.
 *
 * The bot's card-selection priority (preserved from the original):
 *   1. DRAW_TWO (if legal)
 *   2. SKIP     (if legal)
 *   3. NUMBER   (if legal)
 *   4. Wild     (always legal — last resort)
 */
public final class BotStrategy {

    private BotStrategy() {}

    /**
     * Chooses the index of the card the bot will play, or -1 to draw.
     */
    public static int chooseCard(ArrayList<String> hand, String upCard, String calledColor) {
        // Priority 1: DRAW_TWO
        int idx = findLegal(hand, upCard, calledColor, Card.DRAW_TWO);
        if (idx >= 0) return idx;

        // Priority 2: SKIP
        idx = findLegal(hand, upCard, calledColor, Card.SKIP);
        if (idx >= 0) return idx;

        // Priority 3: NUMBER
        idx = findLegal(hand, upCard, calledColor, Card.NUMBER);
        if (idx >= 0) return idx;

        // Priority 4: any Wild (always legal)
        for (int i = 0; i < hand.size(); i++) {
            if (new Card(hand.get(i)).isWild()) return i;
        }

        return -1;  // must draw
    }

    /**
     * Chooses the color the bot will call after playing a wild.
     * Picks the color the bot holds the most of; ties go to R > Y > G > B.
     */
    public static String chooseColor(ArrayList<String> hand) {
        int r = 0, y = 0, g = 0, b = 0;
        for (String code : hand) {
            String c = new Card(code).color();
            if      (c.equals("R")) r++;
            else if (c.equals("Y")) y++;
            else if (c.equals("G")) g++;
            else if (c.equals("B")) b++;
        }
        if (r >= y && r >= g && r >= b) return "R";
        if (y >= r && y >= g && y >= b) return "Y";
        if (g >= r && g >= y && g >= b) return "G";
        return "B";
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private static int findLegal(ArrayList<String> hand, String upCard,
                                 String calledColor, String targetRank) {
        for (int i = 0; i < hand.size(); i++) {
            Card card = new Card(hand.get(i));
            if (card.rank().equals(targetRank)
                    && Rules.isLegal(card.code(), upCard, calledColor)) {
                return i;
            }
        }
        return -1;
    }
}
