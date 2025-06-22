import static DatabaseHelper.DatabaseQuery.DatabaseQuery_GetRecentMessagesFromMessagesTable;
import static DatabaseHelper.DatabaseQuery.DatabaseQuery_RemoveSocketInfo;

import DatabaseHelper.DatabaseQuery;
import DatabaseHelper.MessagesDO;
import DatabaseHelper.SessionHandler;
import DatabaseHelper.UsersDO;
import Helper.MessagesJO;
import Helper.UsersJO;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.query.Query;

public class DatabaseHandler {
  private static Session session;

  private static final Logger LOGGER = LogManager.getLogger(DatabaseHandler.class.getName());

  public static Object handleQuery(String query, Object object) {
    session = SessionHandler.getInstance().openSession();
    session.beginTransaction();
    final Object returnObject = processQuery(query, object);
    session.getTransaction().commit();
    session.close();
    return returnObject;
  }

  private static Object processQuery(String query, Object object) {
    switch (query) {
      case DatabaseQuery.DatabaseQuery_AddUserToUsersTable:
        addUserToUsersTable((UsersJO) object);
        return null;
      case DatabaseQuery.DatabaseQuery_AddMessageToMessagesTable:
        return addMessageToMessagesTable((MessagesJO) object);
      case DatabaseQuery_GetRecentMessagesFromMessagesTable:
        return getRecentMessagesFromMessagesTable();
      case DatabaseQuery_RemoveSocketInfo:
        removeSocketInfo((String) object);
        return null;
      default:
        LOGGER.error("Error: Query not found");
        return null;
    }
  }

  private static void addUserToUsersTable(UsersJO user) {
    final Query<UsersDO> usersQuery =
        session.createQuery("FROM UsersDO WHERE userId = :userId", UsersDO.class);
    usersQuery.setParameter("userId", user.getUserId());
    final List<UsersDO> existingUserDO = usersQuery.getResultList();

    if (null != existingUserDO && existingUserDO.size() > 0) {
      user.setUserId(existingUserDO.get(0).getUserId() + "-" + UUID.randomUUID());
    }

    final UsersDO usersDO = new UsersDO();
    usersDO.setUserId(user.getUserId());
    usersDO.setPortNumber(user.getPortNumber());
    session.persist(usersDO);
  }

  private static MessagesDO addMessageToMessagesTable(MessagesJO message) {
    final Query<UsersDO> usersQuery =
        session.createQuery("FROM UsersDO WHERE userId = :userId", UsersDO.class);
    usersQuery.setParameter("userId", message.getUserId());
    final UsersDO usersDO = usersQuery.getResultList().get(0);

    final MessagesDO messagesDO = new MessagesDO();
    messagesDO.setMessage(message.getMessage());
    messagesDO.setTimestamp(LocalDateTime.now(ZoneOffset.UTC).toString());
    messagesDO.setAttachedB64Image(message.getAttachedB64Image());
    messagesDO.setUsersDO(usersDO);
    session.persist(messagesDO);
    return messagesDO;
  }

  private static List<MessagesDO> getRecentMessagesFromMessagesTable() {
    final Query<MessagesDO> messagesQuery =
        session.createQuery("FROM MessagesDO WHERE timestamp >= :timestamp", MessagesDO.class);
    messagesQuery.setParameter(
        "timestamp", LocalDateTime.now(ZoneOffset.UTC).minusHours(1).toString());
    return messagesQuery.getResultList();
  }

  private static void removeSocketInfo(String userId) {
    final Query<UsersDO> userQuery = session.createQuery("FROM UsersDO WHERE userId = :userId");
    userQuery.setParameter("userId", userId);
    final UsersDO usersDO = userQuery.getSingleResult();

    final Query deleteMessagesQuery =
        session.createQuery("DELETE FROM MessagesDO WHERE usersDO = :usersDO");
    deleteMessagesQuery.setParameter("usersDO", usersDO);
    deleteMessagesQuery.executeUpdate();

    final Query deleteUserQuery = session.createQuery("DELETE FROM UsersDO WHERE userId = :userId");
    deleteUserQuery.setParameter("userId", userId);
    deleteUserQuery.executeUpdate();
  }
}
