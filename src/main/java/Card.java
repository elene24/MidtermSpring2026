/**
 * Immutable representation of a single UNO card from its short code.
 *
 * Code format:
 *   "<COLOR><RANK>"   e.g. "R5", "GS", "B+2", "YR"
 *   "W"               plain Wild (no color)
 *   "W4"              Wild Draw Four (no color)
 *
 * Colors: R, Y, G, B
 * Ranks:  0-9 (NUMBER), S (SKIP), R (REVERSE), +2 (DRAW_TWO), and the two
 *         colorless wilds above.
 *
 * This class has no game-state knowledge and no I/O. It only parses a code
 * string into queryable properties, which keeps Rules/GameState testable
 * without a console.
 */
public final class Card {

    public static final String NUMBER          = "NUMBER";
    public static final String SKIP            = "SKIP";
    public static final String REVERSE         = "REVERSE";
    public static final String DRAW_TWO        = "DRAW_TWO";
    public static final String WILD            = "WILD";
    public static final String WILD_DRAW_FOUR  = "WILD_DRAW_FOUR";

    private final String code;
    private final String color;
    private final String rank;
    private final int    number;

    public Card(String code) {
        this.code = code;

        if (code.equals("W")) {
            this.color  = "";
            this.rank   = WILD;
            this.number = -1;
        } else if (code.equals("W4")) {
            this.color  = "";
            this.rank   = WILD_DRAW_FOUR;
            this.number = -1;
        } else {
            this.color = code.substring(0, 1);
            String rest = code.substring(1);
            switch (rest) {
                case "S":
                    this.rank   = SKIP;
                    this.number = -1;
                    break;
                case "R":
                    this.rank   = REVERSE;
                    this.number = -1;
                    break;
                case "+2":
                    this.rank   = DRAW_TWO;
                    this.number = -1;
                    break;
                default:
                    this.rank   = NUMBER;
                    this.number = Integer.parseInt(rest);
            }
        }
    }

    public String code()   { return code; }
    public String color()  { return color; }
    public String rank()   { return rank; }
    public int    number() { return number; }

    public boolean isWild() {
        return rank.equals(WILD) || rank.equals(WILD_DRAW_FOUR);
    }

    public boolean isActionCard() {
        return rank.equals(SKIP) || rank.equals(REVERSE) || rank.equals(DRAW_TWO);
    }

    /** Point value used for end-of-round scoring. */
    public int points() {
        switch (rank) {
            case NUMBER:         return number;
            case SKIP:           return 20;
            case REVERSE:        return 20;
            case DRAW_TWO:       return 20;
            case WILD:           return 50;
            case WILD_DRAW_FOUR: return 50;
            default:             return 0;
        }
    }

    @Override
    public String toString() {
        return code;
    }
}
