package Server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server extends Thread {
  private Socket socket;
  private ServerSocket serverSocket;
  private int port = 3000;
  private PrintWriter printWriter;
  private HashMap<Integer, Integer> existingClientPorts;
  private ArrayList<SendClient> sendClients;

  private final Logger logger = Logger.getLogger(Server.class.getName());

  public Server() {
    try {
      sendClients = new ArrayList<>();
      existingClientPorts = new HashMap<>();
      serverSocket = new ServerSocket(port);
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Server Exception: " + e.getMessage());
    }
  }

  @Override
  public void run() {
    try {
      while (true) {
        logger.log(Level.INFO, "Listening for traffic on port " + port);
        socket = serverSocket.accept();
        logger.log(
            Level.INFO,
            String.format(
                "Initiation message received from client %s:%d",
                socket.getInetAddress().getHostAddress(), port));

        // Create new client listener and sender threads
        int newClientPort = generateClientListenPort();
        SendClient sendClient = new SendClient(this);
        sendClients.add(sendClient);
        ReceiveClient receiveClient = new ReceiveClient(newClientPort, this, sendClient);
        sendClient.start();
        receiveClient.start();

        // Send new client port info back to client
        printWriter = new PrintWriter(socket.getOutputStream(), true);
        printWriter.println(newClientPort);
        logger.log(Level.INFO, String.format("New port %d sent to client", newClientPort));

        // Close socket
        socket.close();
      }
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Server Exception: " + e.getMessage());
    }
  }

  private int generateClientListenPort() {
    Random random = new Random();
    int randomPort = random.nextInt(65535) + 1024;
    logger.log(Level.INFO, "Random port generated is " + randomPort);

    if (existingClientPorts.containsValue(randomPort) || randomPort > 65535 || randomPort < 1025) {
      generateClientListenPort();
    }

    existingClientPorts.put(randomPort, randomPort);
    return randomPort;
  }

  public void addToReceiveClientList(String message) {
    logger.log(Level.INFO, "Number of client receivers: " + sendClients.size());
    for (SendClient clients : sendClients) {
      clients.appendToMessageQueue(message);
    }
  }
}
