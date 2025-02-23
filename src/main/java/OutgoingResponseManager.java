import Helper.SocketController;
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
  private byte[] message;
  private final Logger LOGGER = LogManager.getLogger(OutgoingResponseManager.class.getName());

  public OutgoingResponseManager(ServerManager serverManager, byte[] message) {
    this.serverManager = serverManager;
    this.message = message;
  }

  @Override
  public void run() {
    try {
      final List<SocketController> sockets = serverManager.getSockets();
      LOGGER.info(
          String.format("Total amount of sockets needing new message is %d", sockets.size()));
      for (SocketController socket : sockets) {
        sendMessage(socket);
      }
    } catch (Exception e) {
      LOGGER.error("Exception: " + e.getMessage());
    }
  }

  private void sendMessage(SocketController socket)
      throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException,
          BadPaddingException, InvalidKeyException, IOException {
    final SocketController socketController =
        serverManager.searchSocketsByPort(socket.getServerPort());
    final SecretKey secretKey = socketController.getSecretKey();
    final PrintWriter printWriter = new PrintWriter(socket.getSocket().getOutputStream(), true);
    final byte[] encryptedMessage = EncryptionDecryptionHandler.encryptMessage(message, secretKey);
    final String encodedEncryptedMessage =
        EncryptionDecryptionHandler.Base64EncodeMessage(encryptedMessage);
    printWriter.println(encodedEncryptedMessage);
    LOGGER.info(
        String.format(
            "Message sent to client %s:%d",
            socket.getSocket().getInetAddress().getHostAddress(), socket.getSocket().getPort()));
  }
}
