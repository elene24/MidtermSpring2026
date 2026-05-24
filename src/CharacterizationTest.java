import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;


// characterization tests for the uno cli game.
//
//  These tests document the current behavior of the implementation, including
//  quirks.  Their purpose is to protect existing behavior during refactoring.
//

public class CharacterizationTest {

    static int passed = 0;
    static int failed = 0;



    static void testCardParsing() {
        assertEqual("R", new Card("R5").color(),   "color of R5 is R");
        assertEqual("Y", new Card("Y0").color(),   "color of Y0 is Y");
        assertEqual("G", new Card("GS").color(),   "color of GS is G");
        assertEqual("B", new Card("B+2").color(),  "color of B+2 is B");
        assertEqual("",  new Card("W").color(),    "color of W is empty");
        assertEqual("",  new Card("W4").color(),   "color of W4 is empty");

        assertEqual(Card.NUMBER,         new Card("R5").rank(),  "rank of R5 is NUMBER");
        assertEqual(Card.NUMBER,         new Card("B0").rank(),  "rank of B0 is NUMBER");
        assertEqual(Card.SKIP,           new Card("YS").rank(),  "rank of YS is SKIP");
        assertEqual(Card.REVERSE,        new Card("GR").rank(),  "rank of GR is REVERSE");
        assertEqual(Card.DRAW_TWO,       new Card("R+2").rank(), "rank of R+2 is DRAW_TWO");
        assertEqual(Card.WILD,           new Card("W").rank(),   "rank of W is WILD");
        assertEqual(Card.WILD_DRAW_FOUR, new Card("W4").rank(),  "rank of W4 is WILD_DRAW_FOUR");

        assertEqual(5,  new Card("R5").number(), "number of R5 is 5");
        assertEqual(0,  new Card("G0").number(), "number of G0 is 0");
        assertEqual(9,  new Card("B9").number(), "number of B9 is 9");
        assertEqual(-1, new Card("RS").number(), "number of RS (action) is -1");

        assertEqual(5,  new Card("R5").points(),   "points R5 = 5");
        assertEqual(0,  new Card("Y0").points(),   "points Y0 = 0");
        assertEqual(9,  new Card("G9").points(),   "points G9 = 9");
        assertEqual(20, new Card("BS").points(),   "points skip = 20");
        assertEqual(20, new Card("RR").points(),   "points reverse = 20");
        assertEqual(20, new Card("G+2").points(),  "points draw-two = 20");
        assertEqual(50, new Card("W").points(),    "points wild = 50");
        assertEqual(50, new Card("W4").points(),   "points wild-draw-four = 50");
    }



    static void testMatchByColor() {
        assertTrue(Rules.isLegal("R2", "R9", ""),  "R2 on R9: same color");
        assertTrue(Rules.isLegal("RS", "R5", ""),  "RS on R5: same color (action vs number)");
        assertTrue(Rules.isLegal("RR", "R3", ""),  "RR on R3: same color");
        assertTrue(Rules.isLegal("R+2","R7", ""),  "R+2 on R7: same color");
        assertFalse(Rules.isLegal("B2","R9",  ""), "B2 on R9: color mismatch, no called color");
    }

    static void testMatchByNumber() {
        assertTrue(Rules.isLegal("G5", "R5", ""),  "G5 on R5: same number");
        assertTrue(Rules.isLegal("B0", "G0", ""),  "B0 on G0: same number (zero)");
        assertTrue(Rules.isLegal("Y9", "B9", ""),  "Y9 on B9: same number");
        assertFalse(Rules.isLegal("G4","R5",  ""), "G4 on R5: different number");
    }

    static void testMatchByActionType() {
        assertTrue(Rules.isLegal("YS",  "RS",  ""), "YS on RS: skip on skip");
        assertTrue(Rules.isLegal("BR",  "GR",  ""), "BR on GR: reverse on reverse");
        assertTrue(Rules.isLegal("G+2", "R+2", ""), "G+2 on R+2: draw-two on draw-two");
        assertFalse(Rules.isLegal("YS","R+2",  ""), "YS on R+2: skip vs draw-two not matched");
        assertFalse(Rules.isLegal("BR","RS",   ""), "BR on RS: reverse vs skip not matched");
    }

    static void testWildAlwaysLegal() {
        assertTrue(Rules.isLegal("W",  "R5", ""),  "W on R5: wild always legal");
        assertTrue(Rules.isLegal("W4", "G3", ""),  "W4 on G3: W4 always legal");
        assertTrue(Rules.isLegal("W",  "BS", ""),  "W on BS: wild always legal");
        assertTrue(Rules.isLegal("W4", "W",  "B"), "W4 on W with called color: still legal");
    }

    static void testCalledColorAfterWild() {
        // after a wild is played and "B" is called, any blue card is now legal
        // even if it doesn't match the up-card's original color
        assertTrue(Rules.isLegal("B3",  "W",  "B"), "B3 on W called B: matches called color");
        assertTrue(Rules.isLegal("BS",  "W4", "B"), "BS on W4 called B: matches called color");
        assertFalse(Rules.isLegal("R3", "W",  "B"), "R3 on W called B: wrong color");
        assertFalse(Rules.isLegal("G3", "W4", "B"), "G3 on W4 called B: wrong color");
    }



    static void testScoring() {
        List<String> hand = Arrays.asList("R5", "B9", "GS", "W");
        assertEqual(84, Rules.scoreHand(hand), "scoring example from rules: 84");

        // empty hand = 0
        assertEqual(0, Rules.scoreHand(new ArrayList<>()), "empty hand scores 0");
        // all wildcards
        List<String> wilds = Arrays.asList("W", "W4", "W");
        assertEqual(150, Rules.scoreHand(wilds), "three wildcards score 150");
    }



    static void testBotPriority() {
        ArrayList<String> hand = new ArrayList<>(Arrays.asList("R2", "RS", "R+2", "W"));
        int choice = BotStrategy.chooseCard(hand, "R5", "");
        assertEqual("R+2", hand.get(choice), "bot prefers DRAW_TWO over SKIP and NUMBER");

        ArrayList<String> hand2 = new ArrayList<>(Arrays.asList("R2", "RS", "W"));
        int choice2 = BotStrategy.chooseCard(hand2, "R5", "");
        assertEqual("RS", hand2.get(choice2), "bot prefers SKIP over NUMBER");

        ArrayList<String> hand3 = new ArrayList<>(Arrays.asList("R2", "W"));
        int choice3 = BotStrategy.chooseCard(hand3, "R5", "");
        assertEqual("R2", hand3.get(choice3), "bot prefers NUMBER over WILD");

        ArrayList<String> hand4 = new ArrayList<>(Arrays.asList("G2", "W"));
        int choice4 = BotStrategy.chooseCard(hand4, "R5", "");
        assertEqual("W", hand4.get(choice4), "bot uses wild as last resort");
    }

    static void testBotDrawsWhenNoLegalCard() {
        ArrayList<String> hand = new ArrayList<>(Arrays.asList("B2", "B3"));
        int choice = BotStrategy.chooseCard(hand, "R5", "");
        assertEqual(-1, choice, "bot returns -1 (draw) when no legal card and no wild");
    }

    static void testBotChoosesColor() {
        ArrayList<String> hand = new ArrayList<>(Arrays.asList("B1", "B2", "R3"));
        assertEqual("B", BotStrategy.chooseColor(hand), "bot picks most frequent color B");

        ArrayList<String> all = new ArrayList<>(Arrays.asList("R1", "Y1", "G1", "B1"));
        assertEqual("R", BotStrategy.chooseColor(all), "bot breaks tie toward R");

        ArrayList<String> wilds = new ArrayList<>(Arrays.asList("W", "W4"));
        assertEqual("R", BotStrategy.chooseColor(wilds), "bot with only wilds chooses R");
    }



    static void testDealHandSizes() {
        List<String>  names  = Arrays.asList("A", "B", "C");
        List<Boolean> humans = Arrays.asList(false, false, false);
        GameState     state  = new GameState(names, humans, new Random(42));
        state.deal();

        assertEqual(7, state.handOf(0).size(), "player 0 starts with 7 cards");
        assertEqual(7, state.handOf(1).size(), "player 1 starts with 7 cards");
        assertEqual(7, state.handOf(2).size(), "player 2 starts with 7 cards");
    }

    static void testUpCardIsNotWild() {
        for (int seed = 0; seed < 30; seed++) {
            List<String>  names  = Arrays.asList("A", "B");
            List<Boolean> humans = Arrays.asList(false, false);
            GameState     state  = new GameState(names, humans, new Random(seed));
            state.deal();
            assertFalse(new Card(state.upCard()).isWild(),
                    "seed " + seed + ": initial up-card is not a wild");
        }
    }

    static void testDrawReshufflesDiscard() {
        List<String>  names  = Arrays.asList("A", "B");
        List<Boolean> humans = Arrays.asList(false, false);
        GameState     state  = new GameState(names, humans, new Random(7));
        state.deal();
        boolean ok = true;
        try {
            for (int i = 0; i < 80; i++) state.drawCard();
        } catch (Exception e) {
            ok = false;
        }
        assertTrue(ok, "drawing 80 cards does not throw (reshuffle works)");
    }

    static void testTurnAdvance() {
        List<String>  names  = Arrays.asList("A", "B", "C");
        List<Boolean> humans = Arrays.asList(false, false, false);
        GameState     state  = new GameState(names, humans, new Random(1));
        state.deal();

        // force to player 0 by setting up manually isn't exposed, so we test that advance wraps around correctly regardless of start
        int start = state.currentPlayer();
        state.advancePlayer();
        int next  = state.currentPlayer();
        assertFalse(start == next, "advancing changes current player");

        // reverse then advance should go backward
        state.reverseDirection();
        state.advancePlayer();
        assertEqual(start, state.currentPlayer(), "reverse + advance returns to start (3 players)");
    }

    static void testScoreAllOpponents() {
        List<String>  names  = Arrays.asList("A", "B");
        List<Boolean> humans = Arrays.asList(false, false);
        GameState     state  = new GameState(names, humans, new Random(1));
        state.deal();

        // manually clear and set known hands
        state.handOf(0).clear();
        state.handOf(1).clear();
        state.handOf(1).add("R5");
        state.handOf(1).add("W");

        // If player 0 wins, score is sum of player 1's hand = 55
        assertEqual(55, state.scoreAllOpponents(0), "scoreAllOpponents sums remaining hands correctly");
    }



    static void testFormatHand() {
        ArrayList<String> hand = new ArrayList<>(Arrays.asList("R5", "GS", "W"));
        assertEqual("0:R5 1:GS 2:W", ConsoleView.formatHand(hand), "hand format matches original");

        ArrayList<String> empty = new ArrayList<>();
        assertEqual("", ConsoleView.formatHand(empty), "empty hand formats to empty string");

        ArrayList<String> single = new ArrayList<>(Arrays.asList("W4"));
        assertEqual("0:W4", ConsoleView.formatHand(single), "single card formats correctly");
    }



    static void testDeterministicGameScore() {
        // with seed 42 and 3 bots, the winner and score must not change after refactoring.
        Main.scores = new int[10];
        Main.playerNames.clear(); Main.humanPlayers.clear();
        Main.setupPlayers(3, false);

        Random      rng  = new Random(42);
        ConsoleView view = new ConsoleView(true, null);   // quiet
        GameState   gs   = new GameState(Main.playerNames, Main.humanPlayers, rng);
        Main.playGame(gs, view, Main.scores, null);

        assertEqual(87, Main.scores[2], "seed-42 deterministic score: Bot3 wins 87");
    }

    static void testSafetyLimitStopsGame() {

        Main.scores = new int[10];
        Main.playerNames.clear(); Main.humanPlayers.clear();
        Main.setupPlayers(2, false);

        for (int seed = 0; seed < 10; seed++) {
            Random      rng  = new Random(seed);
            ConsoleView view = new ConsoleView(true, null);
            GameState   gs   = new GameState(Main.playerNames, Main.humanPlayers, rng);
            Main.playGame(gs, view, Main.scores, null);
        }
        assertTrue(true, "10 two-player games all terminate without hanging");
    }



    static void testReverseActsAsSkipInTwoPlayerGame() {

        List<String>  names  = Arrays.asList("A", "B");
        List<Boolean> humans = Arrays.asList(false, false);
        GameState     state  = new GameState(names, humans, new Random(1));
        state.deal();

        // force player 0
        while (state.currentPlayer() != 0) state.advancePlayer();

        // apply reverse effect manually
        state.reverseDirection();
        state.advancePlayer();
        state.advancePlayer();

        assertEqual(0, state.currentPlayer(),
                "REVERSE in 2-player game lands back on same player (acts as skip)");
    }

    static void testWildW4IsAlwaysLegal() {
        // W4 should be legal even when the player has legal colored cards.
        assertTrue(Rules.isLegal("W4", "R5", ""),  "W4 legal even when colors available (no challenge rule)");
        assertTrue(Rules.isLegal("W4", "G3", "G"), "W4 legal even with called color");
    }

    static void testBotUsesCalledColorAfterWild() {
        // Bot should be able to play a card matching the called color
        ArrayList<String> hand = new ArrayList<>(Arrays.asList("B5", "R3"));
        int choice = BotStrategy.chooseCard(hand, "W4", "B");
        assertEqual("B5", hand.get(choice), "bot plays card matching called color after wild");
    }

    static void testNumberZeroScoresZeroPoints() {
        // Edge: 0-cards score 0, not face-value-of-character '0'
        assertEqual(0, new Card("R0").points(), "R0 scores 0 points, not ASCII 48");
        assertEqual(0, new Card("B0").points(), "B0 scores 0 points");
    }

    static void testAllFourColorsSameRankMatch() {
        // every color of Skip can be played on any other color of Skip
        String[] colors = {"R", "Y", "G", "B"};
        for (String c1 : colors) {
            for (String c2 : colors) {
                if (!c1.equals(c2)) {
                    assertTrue(Rules.isLegal(c1 + "S", c2 + "S", ""),
                            c1 + "S on " + c2 + "S: skip on skip");
                    assertTrue(Rules.isLegal(c1 + "R", c2 + "R", ""),
                            c1 + "R on " + c2 + "R: reverse on reverse");
                    assertTrue(Rules.isLegal(c1 + "+2", c2 + "+2", ""),
                            c1 + "+2 on " + c2 + "+2: draw-two on draw-two");
                }
            }
        }
    }



    public static void main(String[] args) {
        run("Card parsing",              CharacterizationTest::testCardParsing);
        run("Match by color",            CharacterizationTest::testMatchByColor);
        run("Match by number",           CharacterizationTest::testMatchByNumber);
        run("Match by action type",      CharacterizationTest::testMatchByActionType);
        run("Wild always legal",         CharacterizationTest::testWildAlwaysLegal);
        run("Called color after wild",   CharacterizationTest::testCalledColorAfterWild);
        run("Scoring",                   CharacterizationTest::testScoring);
        run("Bot priority",              CharacterizationTest::testBotPriority);
        run("Bot draws when stuck",      CharacterizationTest::testBotDrawsWhenNoLegalCard);
        run("Bot chooses color",         CharacterizationTest::testBotChoosesColor);
        run("Deal hand sizes",           CharacterizationTest::testDealHandSizes);
        run("Up card not wild",          CharacterizationTest::testUpCardIsNotWild);
        run("Draw reshuffles discard",   CharacterizationTest::testDrawReshufflesDiscard);
        run("Turn advance",              CharacterizationTest::testTurnAdvance);
        run("Score opponents",           CharacterizationTest::testScoreAllOpponents);
        run("Format hand",               CharacterizationTest::testFormatHand);
        run("Deterministic game score",  CharacterizationTest::testDeterministicGameScore);
        run("Safety limit stops game",   CharacterizationTest::testSafetyLimitStopsGame);
        run("Reverse as skip (2p)",      CharacterizationTest::testReverseActsAsSkipInTwoPlayerGame);
        run("W4 always legal (no challenge)", CharacterizationTest::testWildW4IsAlwaysLegal);
        run("Bot uses called color",     CharacterizationTest::testBotUsesCalledColorAfterWild);
        run("Zero scores 0 points",      CharacterizationTest::testNumberZeroScoresZeroPoints);
        run("All colors same rank",      CharacterizationTest::testAllFourColorsSameRankMatch);

        System.out.println("\n─────────────────────────────────────────");
        System.out.println("Results: " + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }


    static void run(String name, Runnable test) {
        System.out.print("  " + name + " ... ");
        int before = failed;
        try {
            test.run();
            if (failed == before) System.out.println("OK");
        } catch (Exception e) {
            failed++;
            System.out.println("EXCEPTION: " + e.getMessage());
        }
    }

    static void assertTrue(boolean condition, String message) {
        if (condition) { passed++; }
        else           { failed++; System.out.println("\n    FAIL: " + message); }
    }

    static void assertFalse(boolean condition, String message) {
        assertTrue(!condition, message);
    }

    static <T> void assertEqual(T expected, T actual, String message) {
        if (expected.equals(actual)) { passed++; }
        else {
            failed++;
            System.out.println("\n    FAIL: " + message
                    + " (expected=" + expected + ", actual=" + actual + ")");
        }
    }
}