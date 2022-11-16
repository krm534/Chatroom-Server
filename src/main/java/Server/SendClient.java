package Server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SendClient extends Thread {
  private Server server;
  private PrintWriter printWriter;
  private Queue<String> messages;

  private final Logger logger = Logger.getLogger(SendClient.class.getName());

  public SendClient(Server server) {
    this.messages = new LinkedList<>();
    this.server = server;
  }

  @Override
  public void run() {
    while (true) {
      try {
        if (messages.size() > 0) {
          logger.log(Level.INFO, "Message queue is greater than 0");
          String clientMessage = messages.poll();
          printWriter.println(clientMessage);
          logger.log(Level.INFO, String.format("Message %s sent to all clients", clientMessage));
        }

        Thread.sleep(5000);
      } catch (Exception e) {
        logger.log(Level.SEVERE, "SendClient Exception: " + e.getMessage());
      }
    }
  }

  public void setSocket(Socket socket) {
    try {
      printWriter = new PrintWriter(socket.getOutputStream(), true);
    } catch (IOException e) {
      logger.log(Level.SEVERE, "SendClient Exception: " + e.getMessage());
    }
  }

  public void appendToMessageQueue(String message) {
    messages.add(message);
    logger.log(Level.INFO, message + " placed in send queue!");
  }
}
