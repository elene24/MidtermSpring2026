package persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

/**
 * Single point of access to the JPA EntityManagerFactory.
 * Call init(unitName) once at startup (production unit or test unit),
 * then use getEntityManager() everywhere else.
 */
public final class JpaUtil {

    private static EntityManagerFactory factory;

    private JpaUtil() {}

    public static void init(String persistenceUnitName) {
        if (factory == null || !factory.isOpen()) {
            factory = Persistence.createEntityManagerFactory(persistenceUnitName);
        }
    }

    public static EntityManager getEntityManager() {
        if (factory == null) {
            throw new IllegalStateException("JpaUtil.init(unitName) must be called before use");
        }
        return factory.createEntityManager();
    }

    public static void close() {
        if (factory != null && factory.isOpen()) {
            factory.close();
        }
    }
}