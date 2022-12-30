import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientReceiver extends Thread {
  private ServerHandler serverHandler;
  private ServerSocket serverSocket;
  private final Logger LOGGER = Logger.getLogger(ClientReceiver.class.getName());

  public ClientReceiver(int port, ServerHandler serverHandler) {
    try {
      this.serverSocket = new ServerSocket(port);
      this.serverHandler = serverHandler;
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "ReceiveClient Exception: " + e.getMessage());
    }
  }

  @Override
  public void run() {
    try {
      LOGGER.log(
          Level.INFO, "Listening for ServerSocket traffic on port " + serverSocket.getLocalPort());
      final Socket socket = serverSocket.accept();
      serverHandler.addSocket(socket);

      while (true) {
        final String clientMessage =
            new BufferedReader(new InputStreamReader(socket.getInputStream())).readLine();

        if (null != clientMessage && !clientMessage.equals("")) {
          LOGGER.log(Level.INFO, String.format("Received message is %s", clientMessage));
          serverHandler.addToMessageQueue(clientMessage);
        }
      }
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "ReceiveClient Exception: " + e.getMessage());
    }
  }
}
