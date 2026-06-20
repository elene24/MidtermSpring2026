/**
 * Immutable value object representing a single UNO card.
 *
 * Cards are stored and compared by their code string (e.g. "R5", "GS", "W4").
 * All parsing logic lives here so the rest of the codebase never inspects
 * raw string suffixes directly.
 */
public final class Card {

    // ── Rank constants ───────────────────────────────────────────────────────
    public static final String NUMBER         = "NUMBER";
    public static final String SKIP           = "SKIP";
    public static final String REVERSE        = "REVERSE";
    public static final String DRAW_TWO       = "DRAW_TWO";
    public static final String WILD           = "WILD";
    public static final String WILD_DRAW_FOUR = "WILD_DRAW_FOUR";

    private final String code;

    public Card(String code) {
        this.code = code;
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public String code() { return code; }

    /** Returns "R", "Y", "G", "B", or "" for wilds. */
    public String color() {
        if (code.startsWith("R")) return "R";
        if (code.startsWith("Y")) return "Y";
        if (code.startsWith("G")) return "G";
        if (code.startsWith("B")) return "B";
        return "";
    }

    /** Returns one of the rank constants above. */
    public String rank() {
        if (code.equals("W"))  return WILD;
        if (code.equals("W4")) return WILD_DRAW_FOUR;
        if (code.endsWith("S"))  return SKIP;
        if (code.endsWith("R"))  return REVERSE;
        if (code.endsWith("+2")) return DRAW_TWO;
        return NUMBER;
    }

    /** Returns the face number for NUMBER cards, -1 otherwise. */
    public int number() {
        if (!rank().equals(NUMBER)) return -1;
        return Integer.parseInt(code.substring(1));
    }

    public boolean isWild() {
        return code.equals("W") || code.equals("W4");
    }

    // ── Scoring ──────────────────────────────────────────────────────────────

    public int points() {
        switch (rank()) {
            case NUMBER:         return number();
            case SKIP:
            case REVERSE:
            case DRAW_TWO:       return 20;
            case WILD:
            case WILD_DRAW_FOUR: return 50;
            default:             return 0;
        }
    }

    // ── Object boilerplate ───────────────────────────────────────────────────

    @Override public String toString() { return code; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Card)) return false;
        return code.equals(((Card) o).code);
    }

    @Override public int hashCode() { return code.hashCode(); }
}