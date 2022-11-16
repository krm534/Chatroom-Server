package Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReceiveClient extends Thread {
  private Server server;
  private ServerSocket serverSocket;
  private Socket socket;
  private Scanner scanner;
  private SendClient sendClient;

  private final Logger logger = Logger.getLogger(ReceiveClient.class.getName());

  public ReceiveClient(int port, Server server, SendClient sendClient) {
    try {
      this.serverSocket = new ServerSocket(port);
      this.server = server;
      this.sendClient = sendClient;
    } catch (IOException e) {
      logger.log(Level.SEVERE, "ReceiveClient Exception: " + e.getMessage());
    }
  }

  @Override
  public void run() {
    try {
      socket = serverSocket.accept();
      scanner = new Scanner(socket.getInputStream());
      sendClient.setSocket(socket);

      while (true) {
        logger.log(Level.INFO, "Listening for traffic on port " + serverSocket.getLocalPort());
        String clientMessage = scanner.nextLine();
        logger.log(
            Level.INFO,
            String.format(
                "Received message %s from %s:%d",
                clientMessage, socket.getInetAddress().getHostAddress(), socket.getPort()));

        if (!clientMessage.equals("Confirmation")) {
          logger.log(
              Level.INFO,
              String.format(
                  "Confirmation message received from %s:%d",
                  clientMessage, socket.getInetAddress().getHostAddress(), socket.getPort()));
          server.addToReceiveClientList(clientMessage);
        }
      }
    } catch (IOException e) {
      logger.log(Level.SEVERE, "ReceiveClient Exception: " + e.getMessage());
    }
  }
}
