import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Mutable game state for one UNO round: deck, discard pile, player hands,
 * turn order, up-card, called color, and UNO-call tracking.
 *
 * This class owns all mutations to cards in play. It does not contain any
 * console I/O, bot strategy, or scoring across rounds (that lives in Main
 * and Rules respectively).
 */
public class GameState {

    private final ArrayList<String> deck    = new ArrayList<>();
    private final ArrayList<String> discard = new ArrayList<>();

    private final List<String>            playerNames;
    private final List<Boolean>           humanFlags;
    private final List<ArrayList<String>> hands;

    /** True once a player has called UNO since last reaching exactly one card. */
    private final boolean[] unoCalled;

    private int    currentPlayer;
    private int    direction;
    private String upCard;
    private String calledColor;

    private final Random random;

    public GameState(List<String> playerNames, List<Boolean> humanFlags, Random random) {
        this.playerNames = playerNames;
        this.humanFlags  = humanFlags;
        this.random      = random;

        this.hands = new ArrayList<>();
        for (int i = 0; i < playerNames.size(); i++) {
            this.hands.add(new ArrayList<>());
        }
        this.unoCalled = new boolean[playerNames.size()];
    }

    // ── Setup ────────────────────────────────────────────────────────────────

    /** Resets and deals a fresh round. */
    public void deal() {
        buildDeck();
        Collections.shuffle(deck, random);
        discard.clear();
        for (ArrayList<String> h : hands) h.clear();
        for (int i = 0; i < unoCalled.length; i++) unoCalled[i] = false;

        for (int i = 0; i < playerNames.size(); i++) {
            for (int j = 0; j < 7; j++) {
                hands.get(i).add(drawCard());
            }
        }

        upCard = drawCard();
        while (upCard.startsWith("W")) {
            discard.add(upCard);
            upCard = drawCard();
        }

        calledColor   = "";
        direction     = 1;
        currentPlayer = random.nextInt(playerNames.size());
    }

    private void buildDeck() {
        deck.clear();
        String[] colors = {"R", "Y", "G", "B"};
        for (String col : colors) {
            deck.add(col + "0");
            for (int n = 1; n <= 9; n++) {
                deck.add(col + n);
                deck.add(col + n);
            }
            deck.add(col + "S");  deck.add(col + "S");
            deck.add(col + "R");  deck.add(col + "R");
            deck.add(col + "+2"); deck.add(col + "+2");
        }
        for (int i = 0; i < 4; i++) {
            deck.add("W");
            deck.add("W4");
        }
    }

    /** Exposes a freshly built, unshuffled deck — used by deck-composition tests. */
    public static ArrayList<String> freshDeck() {
        GameState probe = new GameState(java.util.Arrays.asList("A", "B"),
                java.util.Arrays.asList(false, false), new Random(0));
        probe.buildDeck();
        return new ArrayList<>(probe.deck);
    }

    // ── Draw ─────────────────────────────────────────────────────────────────

    /** Draws one card, reshuffling the discard into the deck if needed. */
    public String drawCard() {
        if (deck.isEmpty()) {
            deck.addAll(discard);
            discard.clear();
            Collections.shuffle(deck, random);
        }
        if (deck.isEmpty()) return "W";   // safety fallback
        return deck.remove(0);
    }

    // ── Turn navigation ──────────────────────────────────────────────────────

    public void advancePlayer() {
        currentPlayer += direction;
        if (currentPlayer >= playerNames.size()) currentPlayer = 0;
        if (currentPlayer < 0) currentPlayer = playerNames.size() - 1;
    }

    public void reverseDirection() {
        direction *= -1;
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public int    currentPlayer()         { return currentPlayer; }
    public String currentPlayerName()     { return playerNames.get(currentPlayer); }
    public boolean currentPlayerIsHuman() { return humanFlags.get(currentPlayer); }

    public ArrayList<String> handOf(int player) { return hands.get(player); }
    public ArrayList<String> currentHand()       { return hands.get(currentPlayer); }

    public String upCard()      { return upCard; }
    public String calledColor() { return calledColor; }
    public int    direction()   { return direction; }
    public int    playerCount() { return playerNames.size(); }
    public String playerName(int i) { return playerNames.get(i); }

    // ── Mutators ─────────────────────────────────────────────────────────────

    public void setUpCard(String card)      { this.upCard = card; }
    public void setCalledColor(String color){ this.calledColor = color; }

    public void playCard(int handIndex) {
        ArrayList<String> hand = currentHand();
        String card = hand.remove(handIndex);
        discard.add(upCard);
        upCard = card;
        calledColor = "";
    }

    public void addPenaltyCard(int player) {
        hands.get(player).add(drawCard());
    }

    /** Adds {@code count} penalty cards (used by Draw Two / Wild Draw Four / missed UNO). */
    public void addPenaltyCards(int player, int count) {
        for (int i = 0; i < count; i++) addPenaltyCard(player);
    }

    public void addDrawnCard(int player, String card) {
        hands.get(player).add(card);
    }

    /** Score all remaining cards held by players other than the winner. */
    public int scoreAllOpponents(int winner) {
        int total = 0;
        for (int i = 0; i < hands.size(); i++) {
            if (i != winner) {
                total += Rules.scoreHand(hands.get(i));
            }
        }
        return total;
    }

    // ── UNO call tracking ────────────────────────────────────────────────────

    /** A call is only valid while the player holds exactly one card. */
    public boolean callUno(int player) {
        if (hands.get(player).size() == 1) {
            unoCalled[player] = true;
            return true;
        }
        return false;
    }

    public boolean hasCalledUno(int player) {
        return unoCalled[player];
    }

    /** Clears the call flag — used whenever a player's hand size changes away from one. */
    public void resetUnoCall(int player) {
        unoCalled[player] = false;
    }

    /**
     * If {@code player} currently holds exactly one card and never called UNO,
     * applies the missed-call penalty and returns true. Otherwise does nothing
     * and returns false. Calling this also resets the call flag so the penalty
     * cannot be re-applied for the same lapse.
     */
    public boolean checkAndPenalizeMissedUno(int player) {
        if (hands.get(player).size() == 1 && !unoCalled[player]) {
            addPenaltyCards(player, Rules.MISSED_UNO_PENALTY_CARDS);
            unoCalled[player] = true; // already penalized, do not re-penalize the same lapse
            return true;
        }
        return false;
    }
}
