package persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "scores")
public class ScoreEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private RoundEntity round;

    @ManyToOne
    @JoinColumn(name = "player_id", nullable = false)
    private PlayerEntity player;

    @Column(nullable = false)
    private int points;

    public ScoreEntity() {}

    public ScoreEntity(PlayerEntity player, int points) {
        this.player = player;
        this.points = points;
    }

    public Long getId() { return id; }
    public RoundEntity getRound() { return round; }
    public void setRound(RoundEntity round) { this.round = round; }
    public PlayerEntity getPlayer() { return player; }
    public int getPoints() { return points; }
}