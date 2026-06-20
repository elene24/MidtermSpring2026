import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import persistence.*;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the persistence layer (Assignment 5).
 *
 * Uses the "uno-test-pu" persistence unit, which points at an isolated
 * in-memory H2 database (jdbc:h2:mem:unotest). A fresh schema is created
 * before each test and dropped after, so these tests never touch the
 * real game.log-adjacent file database and never depend on any
 * developer's local machine state.
 */
public class PersistenceTest {

    @BeforeEach
    void setUp() {
        JpaUtil.init("uno-test-pu");
    }

    @AfterEach
    void tearDown() {
        JpaUtil.close();
    }

    @Test
    void playerRepositoryFindOrCreateIsIdempotent() {
        PlayerRepository repo = new PlayerRepository();

        PlayerEntity first  = repo.findOrCreate("Bot1");
        PlayerEntity second = repo.findOrCreate("Bot1");

        assertEquals(first.getId(), second.getId(), "same name should resolve to the same player row");
        assertEquals("Bot1", first.getName());
    }

    @Test
    void recordingARoundPersistsGamePlayersRoundsAndScores() {
        GameRecorder recorder = new GameRecorder();
        List<String> players = Arrays.asList("Bot1", "Bot2", "Bot3");

        recorder.startSession(players);
        recorder.recordRound(players, new int[]{0, 0, 87}, "Bot3");
        recorder.endSession();

        ReportRepository reports = new ReportRepository();

        List<RoundEntity> recent = reports.listRecentRounds(10);
        assertEquals(1, recent.size(), "exactly one round should have been recorded");
        assertEquals("Bot3", recent.get(0).getWinner().getName());
        assertEquals(1, recent.get(0).getRoundNumber());

        List<Object[]> winCounts = reports.playerWinCounts();
        assertEquals(1, winCounts.size(), "only Bot3 has a win recorded");
        assertEquals("Bot3", winCounts.get(0)[0]);
        assertEquals(1L, winCounts.get(0)[1]);

        List<Object[]> highest = reports.highestScores(10);
        assertEquals("Bot3", highest.get(0)[0], "Bot3's 87 points should be the highest score");
        assertEquals(87, highest.get(0)[1]);
    }

    @Test
    void multipleRoundsAccumulateSeparateWinCounts() {
        GameRecorder recorder = new GameRecorder();
        List<String> players = Arrays.asList("Bot1", "Bot2");

        recorder.startSession(players);
        recorder.recordRound(players, new int[]{0, 40}, "Bot2");
        recorder.recordRound(players, new int[]{55, 0}, "Bot1");
        recorder.recordRound(players, new int[]{0, 20}, "Bot2");
        recorder.endSession();

        ReportRepository reports = new ReportRepository();
        List<Object[]> winCounts = reports.playerWinCounts();

        // Expect Bot2 first with 2 wins, Bot1 second with 1 win (ordered DESC by count)
        assertEquals("Bot2", winCounts.get(0)[0]);
        assertEquals(2L, winCounts.get(0)[1]);
        assertEquals("Bot1", winCounts.get(1)[0]);
        assertEquals(1L, winCounts.get(1)[1]);

        List<RoundEntity> recent = reports.listRecentRounds(10);
        assertEquals(3, recent.size(), "all three rounds should be retrievable");
    }

    @Test
    void roundWithNoWinnerIsStoredWithoutCrashing() {
        // Simulates the safety-limit case where Main.playGame returns -1 (no winner)
        GameRecorder recorder = new GameRecorder();
        List<String> players = Arrays.asList("Bot1", "Bot2");

        recorder.startSession(players);
        recorder.recordRound(players, new int[]{0, 0}, null);
        recorder.endSession();

        ReportRepository reports = new ReportRepository();
        List<RoundEntity> recent = reports.listRecentRounds(10);

        assertEquals(1, recent.size());
        assertNull(recent.get(0).getWinner(), "round with no winner should store a null winner");
    }
}