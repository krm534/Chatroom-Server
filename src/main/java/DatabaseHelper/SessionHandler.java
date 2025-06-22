package DatabaseHelper;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class SessionHandler {

  private static SessionFactory sessionFactory;

  public static SessionFactory getInstance() {
    if (sessionFactory == null) {
      buildSession();
    }
    return sessionFactory;
  }

  private static SessionFactory buildSession() {
    try {
      // Create the SessionFactory from hibernate.cfg.xml
      Configuration configuration = new Configuration().configure("hibernate.cfg.xml");
      sessionFactory =
          configuration
              .addAnnotatedClass(MessagesDO.class)
              .addAnnotatedClass(UsersDO.class)
              .buildSessionFactory();
      return sessionFactory;
    } catch (Throwable ex) {
      // Make sure you log the exception, as it might be swallowed
      System.err.println("Initial SessionFactory creation failed." + ex);
      throw new ExceptionInInitializerError(ex);
    }
  }

  public static void shutdown() {
    // Close caches and connection pools
    sessionFactory.close();
  }
}
