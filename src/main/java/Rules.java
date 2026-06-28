/**
 * Pure, stateless rule logic for UNO.
 *
 * Every method here is a function: same inputs always produce the same output.
 * No global state, no I/O.  This makes rules independently testable.
 *
 * "Legal" means the card may be placed on the discard pile given the current
 * up-card and the color that was called after the most recent wild play.
 */
public final class Rules {

    /** Penalty applied when a player fails to call UNO in time. */
    public static final int MISSED_UNO_PENALTY_CARDS = 2;

    /** Default score needed across rounds to win the overall game. */
    public static final int DEFAULT_TARGET_SCORE = 500;

    private Rules() {}   // utility class, not instantiated

    /**
     * Returns true when {@code card} may legally be played on {@code upCard}
     * given an optional {@code calledColor} (empty string means no color was called).
     *
     * Matches:
     *   1. Wilds are always legal.
     *   2. Same color as up-card.
     *   3. Same color as the called color (overrides up-card color after a wild).
     *   4. Same action rank (SKIP on SKIP, REVERSE on REVERSE, DRAW_TWO on DRAW_TWO).
     *   5. Same number (NUMBER on NUMBER with equal face value).
     */
    public static boolean isLegal(Card card, Card upCard, String calledColor) {
        if (card.isWild()) return true;

        String cc = card.color();
        String uc = upCard.color();

        if (!calledColor.isEmpty() && cc.equals(calledColor)) return true;
        if (cc.equals(uc)) return true;

        String cr = card.rank();
        String ur = upCard.rank();

        if (cr.equals(ur) && !cr.equals(Card.NUMBER)) return true;
        if (cr.equals(Card.NUMBER) && ur.equals(Card.NUMBER)
                && card.number() == upCard.number()) return true;

        return false;
    }

    /** Convenience overload that accepts raw code strings. */
    public static boolean isLegal(String cardCode, String upCode, String calledColor) {
        return isLegal(new Card(cardCode), new Card(upCode), calledColor);
    }

    /**
     * Returns the point value of all cards in a collection.
     * Used to score a winning hand.
     */
    public static int scoreHand(Iterable<String> cardCodes) {
        int total = 0;
        for (String code : cardCodes) {
            total += new Card(code).points();
        }
        return total;
    }

    /** True once any score in the array has reached the target. */
    public static boolean targetReached(int[] scores, int target) {
        for (int s : scores) {
            if (s >= target) return true;
        }
        return false;
    }

    /** Index of the highest score; ties go to the lowest index. */
    public static int leaderIndex(int[] scores) {
        int leader = 0;
        for (int i = 1; i < scores.length; i++) {
            if (scores[i] > scores[leader]) leader = i;
        }
        return leader;
    }
}
