import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point and game orchestrator.
 *
 * Main is responsible only for:
 *   - parsing CLI arguments
 *   - running the requested number of games
 *   - printing final scores
 *
 * Rule logic lives in {@link Rules}.
 * Card representation lives in {@link Card}.
 * Mutable game data lives in {@link GameState}.
 * Console rendering and prompts live in {@link ConsoleView}.
 * Bot decisions live in {@link BotStrategy}.
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    static ArrayList<String>  playerNames  = new ArrayList<>();
    static ArrayList<Boolean> humanPlayers = new ArrayList<>();
    static int[] scores = new int[10];

    // These two remain accessible for the legacy selfTest path
    static String upCard     = "";
    static String calledColor = "";

    public static void main(String[] args) {
        int     bots  = 3;
        int     games = 1;
        boolean human = false;
        boolean quiet = false;
        long    seed  = System.currentTimeMillis();

        for (int i = 0; i < args.length; i++) {
            if      (args[i].equals("--bots")  && i + 1 < args.length) bots  = Integer.parseInt(args[++i]);
            else if (args[i].equals("--games") && i + 1 < args.length) games = Integer.parseInt(args[++i]);
            else if (args[i].equals("--human"))  human = true;
            else if (args[i].equals("--quiet"))  quiet = true;
            else if (args[i].equals("--seed")  && i + 1 < args.length) seed = Long.parseLong(args[++i]);
            else if (args[i].equals("--self-test")) { selfTest(); return; }
            else if (args[i].equals("--help")) {
                System.out.println("Usage: scripts/run.sh [--bots N] [--games N] [--human] [--quiet] [--seed N]");
                return;
            }
        }

        Random      random  = new Random(seed);
        Scanner     scanner = new Scanner(System.in);
        ConsoleView view    = new ConsoleView(quiet, scanner);

        setupPlayers(bots, human);

        if (playerNames.size() < 2 || playerNames.size() > 4) {
            System.out.println("UNO needs 2 to 4 players.");
            log.warn("Startup aborted: invalid player count {}", playerNames.size());
            return;
        }

        log.info("Game started with players={}, seed={}", playerNames, seed);

        for (int g = 1; g <= games; g++) {
            view.showGameHeader(g);
            GameState state = new GameState(playerNames, humanPlayers, random);
            playGame(state, view, scores, scanner);
        }

        view.showFinalScores(playerNames, scores);
        log.info("Session ended. Final scores: {}", java.util.Arrays.toString(scores));
    }

    // ── Player setup ─────────────────────────────────────────────────────────

    static void setupPlayers(int bots, boolean human) {
        playerNames.clear();
        humanPlayers.clear();
        if (human) {
            playerNames.add("You");
            humanPlayers.add(Boolean.TRUE);
        }
        for (int i = 1; i <= bots; i++) {
            playerNames.add("Bot" + i);
            humanPlayers.add(Boolean.FALSE);
        }
    }

    // ── Game loop ─────────────────────────────────────────────────────────────

    static void playGame(GameState state, ConsoleView view, int[] scores, Scanner scanner) {
        state.deal();

        for (int guard = 0; guard < 3000; guard++) {
            String          name  = state.currentPlayerName();
            ArrayList<String> hand  = state.currentHand();
            boolean         human = state.currentPlayerIsHuman();

            log.info("Player turn: {} (upCard={}, calledColor={})",
                    name, state.upCard(), state.calledColor());

            view.showTurnHeader(state.upCard(), state.calledColor(), name, hand);

            // ── Choose a card ────────────────────────────────────────────────
            int chosen;
            if (human) {
                chosen = view.askHumanCardChoice(hand, state.upCard(), state.calledColor());
            } else {
                chosen = BotStrategy.chooseCard(hand, state.upCard(), state.calledColor());
            }

            // ── Handle draw ──────────────────────────────────────────────────
            if (chosen == -1) {
                String drawn = state.drawCard();
                state.addDrawnCard(state.currentPlayer(), drawn);
                view.showDraw(name, drawn);
                log.info("Card drawn: {} by {}", drawn, name);

                if (Rules.isLegal(drawn, state.upCard(), state.calledColor())) {
                    if (!human) {
                        chosen = hand.size() - 1;   // bot plays drawn card automatically
                    } else if (view.askPlayDrawn(drawn)) {
                        chosen = hand.size() - 1;
                    }
                }
            }

            // ── Penalty for out-of-range index ───────────────────────────────
            if (chosen >= hand.size()) {
                log.warn("Invalid input: out-of-range index from {}", name);
                view.showPenalty(name, "");
                state.addPenaltyCard(state.currentPlayer());
                state.advancePlayer();
                continue;
            }

            // ── Nothing played this turn ─────────────────────────────────────
            if (chosen < 0) {
                state.advancePlayer();
                continue;
            }

            // ── Validate legality of the chosen card ─────────────────────────
            String card = hand.get(chosen);
            if (!Rules.isLegal(card, state.upCard(), state.calledColor())) {
                log.warn("Invalid input: illegal card {} attempted by {}", card, name);
                view.showIllegal(name, card);
                state.addPenaltyCard(state.currentPlayer());
                state.advancePlayer();
                continue;
            }

            // ── Play the card ────────────────────────────────────────────────
            state.playCard(chosen);
            view.showPlay(name, card);
            log.info("Card played: {} by {}", card, name);

            // ── Wild: choose color ────────────────────────────────────────────
            if (new Card(card).isWild()) {
                String color = human
                        ? view.askColor()
                        : BotStrategy.chooseColor(hand);
                state.setCalledColor(color);
                view.showCalledColor(name, color);
            }

            // ── UNO announcement ─────────────────────────────────────────────
            if (hand.size() == 1) view.showUno(name);

            // ── Win condition ─────────────────────────────────────────────────
            if (hand.isEmpty()) {
                int points = state.scoreAllOpponents(state.currentPlayer());
                scores[state.currentPlayer()] += points;
                view.showWin(name, points);
                log.info("Round ended: {} wins, scores {} points", name, points);
                return;
            }

            // ── Action card effects ───────────────────────────────────────────
            applyCardEffect(card, state, view);
        }

        view.showSafetyLimit();
        log.warn("Round ended: safety limit reached (3000 turns)");
    }

    /**
     * Applies the effect of the card just played and advances to the next player.
     * Number cards and plain wilds just advance once.
     */
    static void applyCardEffect(String card, GameState state, ConsoleView view) {
        String rank = new Card(card).rank();
        switch (rank) {
            case Card.SKIP:
                state.advancePlayer();   // skip the next player
                state.advancePlayer();
                break;

            case Card.REVERSE:
                state.reverseDirection();
                if (state.playerCount() == 2) {
                    state.advancePlayer();   // reverse acts as skip in 2-player
                    state.advancePlayer();
                } else {
                    state.advancePlayer();
                }
                break;

            case Card.DRAW_TWO:
                state.advancePlayer();
                state.addPenaltyCard(state.currentPlayer());
                state.addPenaltyCard(state.currentPlayer());
                view.showDrawsTwo(state.currentPlayerName());
                state.advancePlayer();
                break;

            case Card.WILD_DRAW_FOUR:
                state.advancePlayer();
                for (int i = 0; i < 4; i++) state.addPenaltyCard(state.currentPlayer());
                view.showDrawsFour(state.currentPlayerName());
                state.advancePlayer();
                break;

            default:   // NUMBER, WILD — just advance
                state.advancePlayer();
                break;
        }
    }

    // ── Legacy self-test (preserved exactly for scripts/test.sh) ─────────────

    static void selfTest() {
        int passed = 0;

        // Card parsing
        if (new Card("R5").color().equals("R"))          passed++; else fail("color R5");
        if (new Card("G+2").rank().equals(Card.DRAW_TWO)) passed++; else fail("rank +2");
        if (new Card("W4").points() == 50)                passed++; else fail("wild points");

        // Rules.isLegal
        if (Rules.isLegal("R2", "R9", ""))  passed++; else fail("same color");
        if (Rules.isLegal("G9", "R9", ""))  passed++; else fail("same number");
        if (Rules.isLegal("B3", "W",  "B")) passed++; else fail("called color");
        if (!Rules.isLegal("B3", "R9", "")) passed++; else fail("illegal mismatch");

        // BotStrategy.chooseCard
        ArrayList<String> h = new ArrayList<>();
        h.add("B3"); h.add("R4"); h.add("W");
        if (BotStrategy.chooseCard(h, "R9", "") == 1) passed++; else fail("bot normal before wild");

        // BotStrategy.chooseColor
        ArrayList<String> h2 = new ArrayList<>();
        h2.add("B1"); h2.add("B2"); h2.add("R3");
        if (BotStrategy.chooseColor(h2).equals("B")) passed++; else fail("bot color");

        System.out.println("Passed " + passed + " characterization checks.");
    }

    static void fail(String name) {
        throw new RuntimeException("Failed: " + name);
    }
}