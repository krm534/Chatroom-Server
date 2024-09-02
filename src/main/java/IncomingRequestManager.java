import Helper.Constants;
import Helper.Message;
import Helper.SocketController;
import com.google.gson.Gson;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Scanner;
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
    createSocket(port);
    this.serverHandler = serverManager;
    this.serverSocketPort = port;
    this.gson = new Gson();
  }

  private void createSocket(int port) throws IOException {
    this.serverSocket = new ServerSocket(port);
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
        if (input.isEmpty()) {
          serverHandler.removeSocket(serverSocketPort, serverSocketPort);
          socket.close();
          break;
        }

        final SocketController socketController =
            serverHandler.searchSocketsByPort(serverSocketPort);
        final SecretKey secretKey = socketController.getSecretKey();

        final byte[] decodedMessage = Base64.getDecoder().decode(input);
        final byte[] decryptedMessage = serverHandler.decryptMessage(decodedMessage, secretKey);
        String decryptedMessageString = new String(decryptedMessage, StandardCharsets.UTF_8);
        decryptedMessageString = decryptedMessageString.replace(Constants.DELIMITER, "");
        LOGGER.info(String.format("Received message is %s", decryptedMessageString));

        final Message message = gson.fromJson(decryptedMessageString, Message.class);
        if (null == message.getMessage() || message.getMessage().isEmpty()) {
          continue;
        }

        serverHandler.sendOutgoingMessage(decryptedMessage);
      }
    } catch (Exception e) {
      LOGGER.error("Exception: " + e.getMessage());
    }
  }
}
