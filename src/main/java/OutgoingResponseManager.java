import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class OutgoingResponseManager extends Thread {
  private ServerManager serverManager;
  private String message;
  private final Logger LOGGER = LogManager.getLogger(OutgoingResponseManager.class.getName());

  public OutgoingResponseManager(ServerManager serverManager, String message) {
    this.serverManager = serverManager;
    this.message = message;
  }

  @Override
  public void run() {
    try {
      final List<Socket> sockets = serverManager.getSockets();
      LOGGER.info(
          String.format("Total amount of sockets needing new message is %d", sockets.size()));
      for (Socket socket : sockets) {
        final PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);
        printWriter.println(message);
        LOGGER.info(
            String.format(
                "Message %s sent to client %s:%d",
                message, socket.getInetAddress().getHostAddress(), socket.getPort()));
      }
    } catch (Exception e) {
      LOGGER.error("Exception: " + e.getMessage());
    }
  }
}
