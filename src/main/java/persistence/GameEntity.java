package persistence;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents one program invocation/session of UNO (i.e. one run of Main,
 * which may play several rounds via --games N).
 */
@Entity
@Table(name = "games")
public class GameEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @ManyToMany
    @JoinTable(
            name = "game_players",
            joinColumns = @JoinColumn(name = "game_id"),
            inverseJoinColumns = @JoinColumn(name = "player_id")
    )
    private List<PlayerEntity> players = new ArrayList<>();

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoundEntity> rounds = new ArrayList<>();

    public GameEntity() {}

    public GameEntity(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public Long getId() { return id; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getEndedAt() { return endedAt; }
    public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }

    public List<PlayerEntity> getPlayers() { return players; }
    public void addPlayer(PlayerEntity p) { players.add(p); }

    public List<RoundEntity> getRounds() { return rounds; }
    public void addRound(RoundEntity r) {
        rounds.add(r);
        r.setGame(this);
    }
}