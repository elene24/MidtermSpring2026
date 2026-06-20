package persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.util.List;

/**
 * Read-only query/report features required by Assignment 5:
 *   - list recent games
 *   - player win count
 *   - highest scores
 *
 * All queries go through JPQL (object-oriented query language), never raw SQL.
 */
public class ReportRepository {

    /** Lists the N most recently completed rounds, most recent first. */
    public List<RoundEntity> listRecentRounds(int limit) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            TypedQuery<RoundEntity> q = em.createQuery(
                    "SELECT r FROM RoundEntity r ORDER BY r.completedAt DESC", RoundEntity.class);
            q.setMaxResults(limit);
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    /** Returns [playerName, winCount] pairs, ordered by win count descending. */
    public List<Object[]> playerWinCounts() {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            TypedQuery<Object[]> q = em.createQuery(
                    "SELECT r.winner.name, COUNT(r) " +
                            "FROM RoundEntity r " +
                            "WHERE r.winner IS NOT NULL " +
                            "GROUP BY r.winner.name " +
                            "ORDER BY COUNT(r) DESC", Object[].class);
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    /** Returns the top N individual scores across all rounds, as [playerName, points]. */
    public List<Object[]> highestScores(int limit) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            TypedQuery<Object[]> q = em.createQuery(
                    "SELECT s.player.name, s.points " +
                            "FROM ScoreEntity s " +
                            "ORDER BY s.points DESC", Object[].class);
            q.setMaxResults(limit);
            return q.getResultList();
        } finally {
            em.close();
        }
    }
}