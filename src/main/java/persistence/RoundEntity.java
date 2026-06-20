package persistence;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents one completed hand of UNO (one call to Main.playGame()).
 */
@Entity
@Table(name = "rounds")
public class RoundEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private GameEntity game;

    @Column(name = "round_number", nullable = false)
    private int roundNumber;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    @ManyToOne
    @JoinColumn(name = "winner_player_id")
    private PlayerEntity winner;

    @OneToMany(mappedBy = "round", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ScoreEntity> scores = new ArrayList<>();

    public RoundEntity() {}

    public RoundEntity(int roundNumber, LocalDateTime completedAt, PlayerEntity winner) {
        this.roundNumber = roundNumber;
        this.completedAt = completedAt;
        this.winner = winner;
    }

    public Long getId() { return id; }
    public GameEntity getGame() { return game; }
    public void setGame(GameEntity game) { this.game = game; }
    public int getRoundNumber() { return roundNumber; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public PlayerEntity getWinner() { return winner; }

    public List<ScoreEntity> getScores() { return scores; }
    public void addScore(ScoreEntity s) {
        scores.add(s);
        s.setRound(this);
    }
}