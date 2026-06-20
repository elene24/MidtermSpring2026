import java.util.ArrayList;
import java.util.Scanner;

/**
 * All console input and output for the game lives here.
 *
 * The view knows nothing about game rules.  It reads and formats strings;
 * it does not decide whether a card is legal.
 *
 * Separating I/O here means game logic (GameState, Rules) can be tested
 * without touching System.in/out.
 */
public class ConsoleView {

    private final boolean quiet;
    private final Scanner scanner;

    public ConsoleView(boolean quiet, Scanner scanner) {
        this.quiet   = quiet;
        this.scanner = scanner;
    }

    // ── Display ──────────────────────────────────────────────────────────────

    public void showTurnHeader(String upCard, String calledColor, String playerName, ArrayList<String> hand) {
        if (quiet) return;
        System.out.println("\nUp card: " + upCard
                + (calledColor.isEmpty() ? "" : " called " + calledColor));
        System.out.println(playerName + " hand: " + formatHand(hand));
    }

    public void showDraw(String playerName, String card) {
        if (quiet) return;
        System.out.println(playerName + " draws " + card);
    }

    public void showPlay(String playerName, String card) {
        if (quiet) return;
        System.out.println(playerName + " plays " + card);
    }

    public void showCalledColor(String playerName, String color) {
        if (quiet) return;
        System.out.println(playerName + " calls " + color);
    }

    public void showUno(String playerName) {
        if (quiet) return;
        System.out.println(playerName + " says UNO!");
    }

    public void showWin(String playerName, int points) {
        if (quiet) return;
        System.out.println(playerName + " wins and scores " + points);
    }

    public void showPenalty(String playerName, String card) {
        if (quiet) return;
        System.out.println(playerName + " selected an invalid index and draws a penalty card.");
    }

    public void showIllegal(String playerName, String card) {
        if (quiet) return;
        System.out.println(playerName + " tried illegal card " + card + " and draws a penalty card.");
    }

    public void showDrawsTwo(String playerName) {
        if (quiet) return;
        System.out.println(playerName + " draws two.");
    }

    public void showDrawsFour(String playerName) {
        if (quiet) return;
        System.out.println(playerName + " draws four.");
    }

    public void showSafetyLimit() {
        if (quiet) return;
        System.out.println("Game stopped at safety limit.");
    }

    public void showGameHeader(int gameNumber) {
        if (quiet) return;
        System.out.println("\n=== Game " + gameNumber + " ===");
    }

    public void showFinalScores(java.util.List<String> names, int[] scores) {
        System.out.println("\nFinal scores:");
        for (int i = 0; i < names.size(); i++) {
            System.out.println(names.get(i) + ": " + scores[i]);
        }
    }

    // ── Human input ──────────────────────────────────────────────────────────

    /**
     * Prompts the human until they enter "draw", a valid hand index,
     * or a card code that is in their hand AND legal.
     *
     * Returns the index of the chosen card, or -1 for "draw".
     *
     * Note: if the player enters an integer index for a card that is in their
     * hand but illegal, this method returns that index without legality checking.
     * The game loop is responsible for detecting and penalizing illegal index plays.
     * This preserves the documented quirk: index-based play is not pre-validated.
     */
    public int askHumanCardChoice(ArrayList<String> hand, String upCard, String calledColor) {
        while (true) {
            System.out.print("Choose card index/code or draw: ");
            String input = scanner.nextLine().trim().toUpperCase();

            if (input.equals("DRAW")) return -1;

            // Integer index path — returned unconditionally (legality checked by game loop)
            try {
                int index = Integer.parseInt(input);
                if (index >= 0 && index < hand.size()) {
                    return index;
                }
                // index out of range: fall through to "Card not found"
            } catch (NumberFormatException ignored) {
            }

            // Card code path — checked for legality here before returning
            for (int i = 0; i < hand.size(); i++) {
                if (hand.get(i).equals(input)) {
                    if (Rules.isLegal(input, upCard, calledColor)) {
                        return i;
                    }
                    System.out.println("That card is not legal.");
                    break;
                }
            }
            System.out.println("Card not found.");
        }
    }

    /**
     * Prompts the human to choose whether to play the card they just drew.
     */
    public boolean askPlayDrawn(String card) {
        System.out.print("Play drawn card " + card + "? y/n: ");
        String answer = scanner.nextLine().trim();
        return answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("yes");
    }

    /**
     * Prompts the human to call a color after playing a wild.
     */
    public String askColor() {
        while (true) {
            System.out.print("Call color R/Y/G/B: ");
            String input = scanner.nextLine().trim().toUpperCase();
            if (input.equals("R") || input.equals("Y")
                    || input.equals("G") || input.equals("B")) {
                return input;
            }
            System.out.println("Bad color.");
        }
    }

    // ── Formatting helpers ───────────────────────────────────────────────────

    public static String formatHand(ArrayList<String> cards) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cards.size(); i++) {
            if (i > 0) sb.append(' ');
            sb.append(i).append(':').append(cards.get(i));
        }
        return sb.toString();
    }
}