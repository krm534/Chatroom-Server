import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerHandler extends Thread {
  private Socket socket;
  private ServerSocket serverSocket;
  private int port = 3000;
  private HashMap<Integer, Integer> existingClientPorts;
  private HashMap<Integer, Socket> sockets;
  private final Logger LOGGER = Logger.getLogger(ServerHandler.class.getName());

  public ServerHandler() {
    try {
      existingClientPorts = new HashMap<>();
      sockets = new HashMap<>();
      serverSocket = new ServerSocket(port);
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Server Exception: " + e.getMessage());
    }
  }

  @Override
  public void run() {
    try {
      while (true) {
        LOGGER.log(Level.INFO, "Listening for traffic on port " + port);
        socket = serverSocket.accept();
        LOGGER.log(
            Level.INFO,
            String.format(
                "Initiation received from client %s:%d",
                socket.getInetAddress().getHostAddress(), port));

        int newClientPort = generateClientListenPort();
        ClientReceiver clientReceiver = new ClientReceiver(newClientPort, this);
        clientReceiver.start();

        String clientData =
            String.format("{userId: %s, port: %d}", UUID.randomUUID().toString(), newClientPort);
        final PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        out.println(clientData);
        LOGGER.log(Level.INFO, String.format("'%s' sent to client", clientData));
        socket.close();
      }
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Server Exception: " + e.getMessage());
    }
  }

  private int generateClientListenPort() {
    Random random = new Random();
    final int randomPort = random.nextInt(65535) + 1024;
    LOGGER.log(Level.INFO, "Random port generated is " + randomPort);

    if (existingClientPorts.containsValue(randomPort) || randomPort > 65535 || randomPort < 1025) {
      generateClientListenPort();
    }

    existingClientPorts.put(randomPort, randomPort);
    return randomPort;
  }

  public void addToMessageQueue(String message) {
    final ClientSender clientSender = new ClientSender(this, message);
    clientSender.start();
  }

  public List<Socket> getSockets() {
    return new ArrayList<>(sockets.values());
  }

  public void addSocket(Socket socket) {
    if (!sockets.containsKey(socket.getPort())) {
      sockets.put(socket.getPort(), socket);
    }
  }

  public void removeClientInfo(int clientPort, int socketPort) {
    existingClientPorts.remove(clientPort);
    sockets.remove(socketPort);
  }
}
