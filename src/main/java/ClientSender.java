import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientSender extends Thread {
  private ServerHandler serverHandler;
  private String message;
  private final Logger LOGGER = Logger.getLogger(ClientSender.class.getName());

  public ClientSender(ServerHandler serverHandler, String message) {
    this.serverHandler = serverHandler;
    this.message = message;
  }

  @Override
  public void run() {
    try {
      final List<Socket> sockets = serverHandler.getSockets();
      for (Socket socket : sockets) {
        final PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);
        printWriter.println(message);
        LOGGER.log(
            Level.INFO,
            String.format(
                "Message %s sent to client %s:%d",
                message, socket.getInetAddress().getHostAddress(), socket.getPort()));
      }
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "SendClient Exception: " + e.getMessage());
    }
  }
}
