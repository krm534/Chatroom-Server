import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ServerManager extends Thread {
  private Socket socket;
  private ServerSocket serverSocket;
  private int port = 3000;
  private HashMap<Integer, Integer> usedPorts;
  private HashMap<Integer, Socket> sockets;
  private final Logger LOGGER = LogManager.getLogger(ServerManager.class.getName());

  public ServerManager() {
    try {
      usedPorts = new HashMap<>();
      sockets = new HashMap<>();
      serverSocket = new ServerSocket(port);
    } catch (IOException e) {
      LOGGER.error("Server Exception: " + e.getMessage());
    }
  }

  @Override
  public void run() {
    try {
      while (true) {
        LOGGER.info("Listening for traffic on port " + port);
        socket = serverSocket.accept();
        LOGGER.info(
            String.format(
                "Initiation received from client %s:%d",
                socket.getInetAddress().getHostAddress(), port));

        final int newClientPort = generateClientListenPort();
        IncomingRequestManager incomingRequestManager =
            new IncomingRequestManager(newClientPort, this);
        incomingRequestManager.start();

        final String clientData =
            String.format("{userId: %s, port: %d}", UUID.randomUUID(), newClientPort);
        final PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        out.println(clientData);
        LOGGER.info(String.format("'%s' sent to client", clientData));
        socket.close();
      }
    } catch (IOException e) {
      LOGGER.error("Server Exception: " + e.getMessage());
    }
  }

  private int generateClientListenPort() {
    final Random random = new Random();
    final int randomPort = random.nextInt(65535) + 1024;
    LOGGER.info("Random port generated is " + randomPort);

    if (usedPorts.containsValue(randomPort) || randomPort > 65535 || randomPort < 1025) {
      generateClientListenPort();
    }

    usedPorts.put(randomPort, randomPort);
    return randomPort;
  }

  public void sendOutgoingMessage(String message) {
    final OutgoingResponseManager outgoingResponseManager =
        new OutgoingResponseManager(this, message);
    outgoingResponseManager.start();
  }

  public List<Socket> getSockets() {
    return new ArrayList<>(sockets.values());
  }

  public void addSocket(Socket socket) {
    if (!sockets.containsKey(socket.getPort())) {
      sockets.put(socket.getPort(), socket);
    }
  }

  public void removeSocket(int clientPort, int socketPort) {
    usedPorts.remove(clientPort);
    sockets.remove(socketPort);
    LOGGER.info(
        String.format("Port %d has been removed from list of existing client ports", clientPort));
    LOGGER.info(
        String.format(
            "Socket for port %d has been removed from list of existing client sockets",
            socketPort));
  }
}
