import Helper.Constants;
import Helper.Message;
import com.google.gson.Gson;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
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
      serverHandler.addSocket(socket);

      while (true) {
        final Scanner scanner = new Scanner(socket.getInputStream());
        final StringBuilder incomingMessage = new StringBuilder();

        while (scanner.hasNextLine()) {
          final String input = scanner.nextLine();
          incomingMessage.append(input);

          if (input.contains(Constants.DELIMITER)) {
            break;
          }
        }

        final String parsedMessage = incomingMessage.toString().replace(Constants.DELIMITER, "");

        LOGGER.info(String.format("Received message is %s", parsedMessage));

        if (parsedMessage.isEmpty()) {
          serverHandler.removeSocket(serverSocketPort, socket.getPort());
          socket.close();
          break;
        }

        final Message message = gson.fromJson(parsedMessage, Message.class);
        if (null == message.getMessage() || message.getMessage().isEmpty()) {
          continue;
        }

        serverHandler.sendOutgoingMessage(incomingMessage.toString());
      }
    } catch (Exception e) {
      LOGGER.error("Exception: " + e.getMessage());
    }
  }
}
