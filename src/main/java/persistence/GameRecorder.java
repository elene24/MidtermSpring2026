package persistence;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Bridges the existing UNO game loop (Main/GameState) to the persistence layer.
 * Main calls startSession() once, recordRound() after every completed round,
 * and endSession() when the program is about to exit.
 *
 * This keeps persistence concerns out of the game rules and CLI rendering code.
 */
public class GameRecorder {

    private final PlayerRepository playerRepo = new PlayerRepository();
    private final GameRepository gameRepo = new GameRepository();

    private GameEntity currentGame;
    private int roundCounter = 0;

    /** Call once at program start, after player names are known. */
    public void startSession(List<String> playerNames) {
        currentGame = new GameEntity(LocalDateTime.now());
        for (String name : playerNames) {
            PlayerEntity player = playerRepo.findOrCreate(name);
            currentGame.addPlayer(player);
        }
        currentGame = gameRepo.save(currentGame);
        roundCounter = 0;
    }

    /**
     * Call once after each completed round (each call to Main.playGame()).
     *
     * @param playerNames     names in the same order as their score in playerScores
     * @param playerScores    points scored by each player in this round (cumulative this round only)
     * @param winnerName      name of the round winner, or null if the safety limit was hit with no winner
     */
    public void recordRound(List<String> playerNames, int[] playerScores, String winnerName) {
        if (currentGame == null) {
            throw new IllegalStateException("startSession() must be called before recordRound()");
        }
        roundCounter++;

        PlayerEntity winner = (winnerName != null) ? playerRepo.findOrCreate(winnerName) : null;

        RoundEntity round = new RoundEntity(roundCounter, LocalDateTime.now(), winner);
        for (int i = 0; i < playerNames.size(); i++) {
            PlayerEntity player = playerRepo.findOrCreate(playerNames.get(i));
            round.addScore(new ScoreEntity(player, playerScores[i]));
        }

        currentGame.addRound(round);
        currentGame = gameRepo.save(currentGame);
    }

    /** Call once at program end. */
    public void endSession() {
        if (currentGame == null) return;
        currentGame.setEndedAt(LocalDateTime.now());
        gameRepo.save(currentGame);
    }
}