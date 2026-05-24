import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;


public class Main {

    static ArrayList<String>  playerNames  = new ArrayList<>();
    static ArrayList<Boolean> humanPlayers = new ArrayList<>();
    static int[] scores = new int[10];

    //these two remain accessible for the legacy selfTest path
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
            return;
        }

        for (int g = 1; g <= games; g++) {
            view.showGameHeader(g);
            GameState state = new GameState(playerNames, humanPlayers, random);
            playGame(state, view, scores, scanner);
        }

        view.showFinalScores(playerNames, scores);
    }

    // player setup

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

    // game loop

    static void playGame(GameState state, ConsoleView view, int[] scores, Scanner scanner) {
        state.deal();

        for (int guard = 0; guard < 3000; guard++) {
            String          name  = state.currentPlayerName();
            ArrayList<String> hand  = state.currentHand();
            boolean         human = state.currentPlayerIsHuman();

            view.showTurnHeader(state.upCard(), state.calledColor(), name, hand);

            // choose a card
            int chosen;
            if (human) {
                chosen = view.askHumanCardChoice(hand, state.upCard(), state.calledColor());
            } else {
                chosen = BotStrategy.chooseCard(hand, state.upCard(), state.calledColor());
            }

            // handle draw
            if (chosen == -1) {
                String drawn = state.drawCard();
                state.addDrawnCard(state.currentPlayer(), drawn);
                view.showDraw(name, drawn);

                if (Rules.isLegal(drawn, state.upCard(), state.calledColor())) {
                    if (!human) {
                        chosen = hand.size() - 1;
                    } else if (view.askPlayDrawn(drawn)) {
                        chosen = hand.size() - 1;
                    }
                }
            }

            if (chosen >= hand.size()) {
                view.showPenalty(name, "");
                state.addPenaltyCard(state.currentPlayer());
                state.advancePlayer();
                continue;
            }

            if (chosen < 0) {
                state.advancePlayer();
                continue;
            }

            String card = hand.get(chosen);
            if (!Rules.isLegal(card, state.upCard(), state.calledColor())) {
                view.showIllegal(name, card);
                state.addPenaltyCard(state.currentPlayer());
                state.advancePlayer();
                continue;
            }

            state.playCard(chosen);
            view.showPlay(name, card);

            if (new Card(card).isWild()) {
                String color = human
                        ? view.askColor()
                        : BotStrategy.chooseColor(hand);
                state.setCalledColor(color);
                view.showCalledColor(name, color);
            }

            if (hand.size() == 1) view.showUno(name);

            if (hand.isEmpty()) {
                int points = state.scoreAllOpponents(state.currentPlayer());
                scores[state.currentPlayer()] += points;
                view.showWin(name, points);
                return;
            }

            applyCardEffect(card, state, view);
        }

        view.showSafetyLimit();
    }


    static void applyCardEffect(String card, GameState state, ConsoleView view) {
        String rank = new Card(card).rank();
        switch (rank) {
            case Card.SKIP:
                state.advancePlayer();
                state.advancePlayer();
                break;

            case Card.REVERSE:
                state.reverseDirection();
                if (state.playerCount() == 2) {
                    state.advancePlayer();
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

            default:
                state.advancePlayer();
                break;
        }
    }


    static void selfTest() {
        int passed = 0;

        //card parsing
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
        // upCard "R9", calledColor "" → R4 at index 1 is legal NUMBER match
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