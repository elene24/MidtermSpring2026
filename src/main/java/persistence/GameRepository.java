package persistence;

import jakarta.persistence.EntityManager;

public class GameRepository {

    public GameEntity save(GameEntity game) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            GameEntity merged = em.merge(game);
            em.getTransaction().commit();
            return merged;
        } finally {
            em.close();
        }
    }

    public GameEntity findById(Long id) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.find(GameEntity.class, id);
        } finally {
            em.close();
        }
    }
}