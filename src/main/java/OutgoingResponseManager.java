import Helper.MessagesJO;
import Helper.SocketControllerJO;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class OutgoingResponseManager extends Thread {
  private ServerManager serverManager;
  private MessagesJO message;
  private Gson gson;
  private final Logger LOGGER = LogManager.getLogger(OutgoingResponseManager.class.getName());

  public OutgoingResponseManager(ServerManager serverManager, MessagesJO message) {
    this.serverManager = serverManager;
    this.message = message;
    this.gson = new Gson();
  }

  @Override
  public void run() {
    try {
      final List<SocketControllerJO> sockets = serverManager.getSockets();
      LOGGER.info(
          String.format("Total amount of sockets needing new message is %d", sockets.size()));
      for (SocketControllerJO socket : sockets) {
        sendMessage(socket);
      }
      this.gson = new Gson();
    } catch (Exception e) {
      LOGGER.error("Exception: " + e.getMessage());
    }
  }

  private void sendMessage(SocketControllerJO socket)
      throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException,
          BadPaddingException, InvalidKeyException, IOException {
    final SocketControllerJO socketControllerJO =
        serverManager.searchSocketsByPort(socket.getServerPort());
    final SecretKey secretKey = socketControllerJO.getSecretKey();
    final PrintWriter printWriter = new PrintWriter(socket.getSocket().getOutputStream(), true);
    final String messageString = gson.toJson(message);
    final String encodedEncryptedMessage =
        EncryptionDecryptionHandler.encryptMessage(messageString.getBytes(), secretKey);
    printWriter.println(encodedEncryptedMessage);
    LOGGER.info(
        String.format(
            "Message sent to client %s:%d",
            socket.getSocket().getInetAddress().getHostAddress(), socket.getSocket().getPort()));
  }
}
