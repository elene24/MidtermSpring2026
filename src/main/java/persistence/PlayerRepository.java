package persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.util.List;

public class PlayerRepository {

    /** Finds an existing player by name, or creates and persists a new one. */
    public PlayerEntity findOrCreate(String name) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            TypedQuery<PlayerEntity> q = em.createQuery(
                    "SELECT p FROM PlayerEntity p WHERE p.name = :name", PlayerEntity.class);
            q.setParameter("name", name);
            List<PlayerEntity> results = q.getResultList();
            if (!results.isEmpty()) {
                return results.get(0);
            }

            em.getTransaction().begin();
            PlayerEntity player = new PlayerEntity(name);
            em.persist(player);
            em.getTransaction().commit();
            return player;
        } finally {
            em.close();
        }
    }

    public List<PlayerEntity> findAll() {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery("SELECT p FROM PlayerEntity p", PlayerEntity.class).getResultList();
        } finally {
            em.close();
        }
    }
}