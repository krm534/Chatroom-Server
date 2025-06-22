import DatabaseHelper.DatabaseQuery;
import DatabaseHelper.MessagesDO;
import Helper.MessagesJO;
import Helper.SocketControllerJO;
import Helper.UsersJO;
import com.google.gson.Gson;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.NoSuchElementException;
import java.util.Scanner;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class IncomingRequestManager extends Thread {
  private ServerManager serverHandler;
  private ServerSocket serverSocket;
  public int serverSocketPort;
  private final Logger LOGGER = LogManager.getLogger(IncomingRequestManager.class.getName());
  private Gson gson;

  public IncomingRequestManager(int port, ServerManager serverManager) throws IOException {
    this.serverSocket = new ServerSocket(port);
    this.serverHandler = serverManager;
    this.serverSocketPort = port;
    this.gson = new Gson();
  }

  @Override
  public void run() {
    try {
      LOGGER.info("Listening for ServerSocket traffic on port " + serverSocket.getLocalPort());
      final Socket socket = serverSocket.accept();
      serverHandler.addSocket(socket, serverSocketPort);
      while (true) {
        final Scanner scanner = new Scanner(socket.getInputStream());
        final String input = scanner.nextLine();
        final String decryptedMessage = receiveDecryptedMessage(input);
        final MessagesJO message = gson.fromJson(decryptedMessage, MessagesJO.class);
        LOGGER.info(String.format("Received message is %s", message));
        switch (message.getMessageType()) {
          case Setup:
            setupUserInfoMessage(message);
            break;
          case Normal:
            processNormalMessages(message);
            break;
          default:
            throw new Exception("Message type not found");
        }
      }
    } catch (NoSuchElementException e) {
      serverHandler.removeSocket(serverSocketPort);
    } catch (Exception e) {
      LOGGER.error("Exception: " + e.getMessage());
    }
  }

  private void processNormalMessages(MessagesJO message) {
    if (isReceivedMessageValid(message)) {
      final MessagesJO newMessage = storeMessageToDatabaseTable(message);
      serverHandler.sendOutgoingMessage(newMessage);
    }
  }

  private void setupUserInfoMessage(MessagesJO message) {
    final SocketControllerJO socketControllerJO =
        serverHandler.searchSocketsByPort(serverSocketPort);
    socketControllerJO.setUserId(message.getUserId());

    final UsersJO user = new UsersJO();
    user.setUserId(message.getUserId());
    user.setPortNumber(serverSocketPort);
    DatabaseHandler.handleQuery(DatabaseQuery.DatabaseQuery_AddUserToUsersTable, user);
  }

  private String receiveDecryptedMessage(String message)
      throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException,
          BadPaddingException, InvalidKeyException {
    final SocketControllerJO socketControllerJO =
        serverHandler.searchSocketsByPort(serverSocketPort);
    final SecretKey secretKey = socketControllerJO.getSecretKey();
    return EncryptionDecryptionHandler.decryptMessage(message.getBytes(), secretKey);
  }

  private boolean isReceivedMessageValid(MessagesJO message) {
    if (null == message.getMessage() || message.getMessage().isEmpty()) {
      return false;
    }
    return true;
  }

  private MessagesJO storeMessageToDatabaseTable(MessagesJO message) {
    final MessagesDO messageDO =
        (MessagesDO)
            DatabaseHandler.handleQuery(
                DatabaseQuery.DatabaseQuery_AddMessageToMessagesTable, message);

    final MessagesJO newMessage = new MessagesJO();
    newMessage.setTimestamp(messageDO.getTimestamp());
    newMessage.setMessage(messageDO.getMessage());
    newMessage.setAttachedB64Image(messageDO.getAttachedB64Image());
    newMessage.setUserId(messageDO.getUsersDO().getUserId());
    return newMessage;
  }
}
