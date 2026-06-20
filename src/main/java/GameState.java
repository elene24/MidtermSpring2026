import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Mutable game state for one UNO game: deck, discard pile, player hands,
 * turn order, up-card, and called color.
 *
 * This class owns all mutations to cards in play.  It does not contain any
 * console I/O, bot strategy, or scoring across games.
 */
public class GameState {

    private final ArrayList<String> deck    = new ArrayList<>();
    private final ArrayList<String> discard = new ArrayList<>();

    private final List<String>            playerNames;
    private final List<Boolean>           humanFlags;
    private final List<ArrayList<String>> hands;

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
    }

    // ── Setup ────────────────────────────────────────────────────────────────

    /** Resets and deals a fresh game. */
    public void deal() {
        buildDeck();
        Collections.shuffle(deck, random);
        discard.clear();
        for (ArrayList<String> h : hands) h.clear();

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
}