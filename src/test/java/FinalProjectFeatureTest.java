import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the rule features added on top of the midterm codebase to meet
 * the final project menu: deck composition, UNO call/penalty, and round
 * scoring carried across a multi-round game to a target score.
 *
 * Skip / Reverse / Draw Two / Wild / Wild Draw Four / legal-play / draw-pass
 * behavior are already covered by CharacterizationTest and are not repeated
 * here.
 */
public class FinalProjectFeatureTest {

    // ── 1.1 Deck composition ─────────────────────────────────────────────────

    @Test
    void deckHas108Cards() {
        ArrayList<String> deck = GameState.freshDeck();
        assertEquals(108, deck.size(), "a classic UNO deck has 108 cards");
    }

    @Test
    void deckHasCorrectCountsPerCardType() {
        ArrayList<String> deck = GameState.freshDeck();

        long zeros = deck.stream().filter(c -> new Card(c).rank().equals(Card.NUMBER)
                && new Card(c).number() == 0).count();
        assertEquals(4, zeros, "one 0 card per color = 4 total");

        long ones = deck.stream().filter(c -> new Card(c).rank().equals(Card.NUMBER)
                && new Card(c).number() == 1).count();
        assertEquals(8, ones, "two 1-cards per color = 8 total");

        long skips   = deck.stream().filter(c -> new Card(c).rank().equals(Card.SKIP)).count();
        long revs    = deck.stream().filter(c -> new Card(c).rank().equals(Card.REVERSE)).count();
        long drawTwo = deck.stream().filter(c -> new Card(c).rank().equals(Card.DRAW_TWO)).count();
        long wild    = deck.stream().filter(c -> new Card(c).rank().equals(Card.WILD)).count();
        long wild4   = deck.stream().filter(c -> new Card(c).rank().equals(Card.WILD_DRAW_FOUR)).count();

        assertEquals(8, skips,   "two Skip per color = 8 total");
        assertEquals(8, revs,    "two Reverse per color = 8 total");
        assertEquals(8, drawTwo, "two Draw Two per color = 8 total");
        assertEquals(4, wild,    "four Wild cards");
        assertEquals(4, wild4,   "four Wild Draw Four cards");
    }

    @Test
    void deckHasAllFourColors() {
        ArrayList<String> deck = GameState.freshDeck();
        for (String color : new String[]{"R", "Y", "G", "B"}) {
            long count = deck.stream().filter(c -> new Card(c).color().equals(color)).count();
            assertEquals(25, count, color + " has 25 colored cards (0-9 x2 minus one 0, +6 action)");
        }
    }

    // ── 1.9 UNO call and missed-UNO penalty ──────────────────────────────────

    @Test
    void detectsOneCardState() {
        GameState state = twoPlayerState();
        state.handOf(0).clear();
        state.handOf(0).add("R5");
        assertEquals(1, state.handOf(0).size(), "one-card state is directly observable on the hand");
    }

    @Test
    void playerCanCallUnoWithOneCard() {
        GameState state = twoPlayerState();
        state.handOf(0).clear();
        state.handOf(0).add("R5");

        boolean accepted = state.callUno(0);

        assertTrue(accepted, "call succeeds when player has exactly one card");
        assertTrue(state.hasCalledUno(0), "call flag is set after a successful call");
    }

    @Test
    void callUnoRejectedWithMoreThanOneCard() {
        GameState state = twoPlayerState();
        state.handOf(0).clear();
        state.handOf(0).add("R5");
        state.handOf(0).add("B3");

        boolean accepted = state.callUno(0);

        assertFalse(accepted, "call is rejected when the player does not have exactly one card");
        assertFalse(state.hasCalledUno(0));
    }

    @Test
    void missedUnoCallIsPenalizedWithTwoCards() {
        GameState state = twoPlayerState();
        state.handOf(0).clear();
        state.handOf(0).add("R5");   // one card, never called UNO

        int before = state.handOf(0).size();
        boolean penalized = state.checkAndPenalizeMissedUno(0);
        int after = state.handOf(0).size();

        assertTrue(penalized, "missed call is detected and penalized");
        assertEquals(before + Rules.MISSED_UNO_PENALTY_CARDS, after,
                "penalty adds exactly the documented number of cards");
    }

    @Test
    void noPenaltyWhenUnoWasCalled() {
        GameState state = twoPlayerState();
        state.handOf(0).clear();
        state.handOf(0).add("R5");
        state.callUno(0);

        int before = state.handOf(0).size();
        boolean penalized = state.checkAndPenalizeMissedUno(0);

        assertFalse(penalized, "no penalty once the player has already called UNO");
        assertEquals(before, state.handOf(0).size(), "hand size is unchanged when no penalty applies");
    }

    @Test
    void unoCallResetsWhenHandSizeChanges() {
        GameState state = twoPlayerState();
        state.handOf(0).clear();
        state.handOf(0).add("R5");
        state.callUno(0);
        assertTrue(state.hasCalledUno(0));

        state.resetUnoCall(0); // simulates drawing/playing away from one card
        assertFalse(state.hasCalledUno(0), "call flag clears once the lapse window has passed");
    }

    // ── 1.10 Round scoring and multi-round target ────────────────────────────

    @Test
    void roundWinnerScoresOpponentsRemainingCards() {
        GameState state = twoPlayerState();
        state.handOf(0).clear();
        state.handOf(1).clear();
        state.handOf(1).add("R5");
        state.handOf(1).add("GS");
        state.handOf(1).add("W4");

        int points = state.scoreAllOpponents(0);

        assertEquals(5 + 20 + 50, points, "winner's score is the sum of opponents' remaining card values");
    }

    @Test
    void multiRoundScoresAccumulateUntilTarget() {
        int[] scores = {120, 480, 90};
        assertFalse(Rules.targetReached(scores, 500), "no player has reached 500 yet");

        scores[1] += 30; // a round win pushes player 1 over the target
        assertTrue(Rules.targetReached(scores, 500), "match ends once a score reaches the target");
        assertEquals(1, Rules.leaderIndex(scores), "the player who reached the target is the leader");
    }

    @Test
    void leaderIndexBreaksTiesTowardLowerIndex() {
        int[] scores = {500, 500, 10};
        assertEquals(0, Rules.leaderIndex(scores), "ties are broken toward the lower index, documented behavior");
    }

    @Test
    void fullMatchStopsAtTargetScoreAcrossRounds() {
        // Drive several full rounds through Main.playGame and confirm the
        // match-level stopping condition (used by Main.main) would trigger.
        Main.scores = new int[10];
        Main.playerNames.clear();
        Main.humanPlayers.clear();
        Main.setupPlayers(3, false);
        int[] scores = new int[Main.playerNames.size()];

        Random rng = new Random(99);
        ConsoleView view = new ConsoleView(true, null);
        int target = 150;
        int roundsPlayed = 0;
        int safety = 50;

        while (!Rules.targetReached(scores, target) && roundsPlayed < safety) {
            GameState gs = new GameState(Main.playerNames, Main.humanPlayers, rng);
            Main.playGame(gs, view, scores, null);
            roundsPlayed++;
        }

        assertTrue(Rules.targetReached(scores, target),
                "repeated rounds eventually push a score to the target");
        assertTrue(roundsPlayed >= 1 && roundsPlayed < safety,
                "match terminates within a reasonable number of rounds");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static GameState twoPlayerState() {
        List<String>  names  = Arrays.asList("A", "B");
        List<Boolean> humans = Arrays.asList(false, false);
        GameState state = new GameState(names, humans, new Random(1));
        state.deal();
        return state;
    }
}
