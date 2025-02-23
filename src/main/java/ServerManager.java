import Helper.SocketController;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.*;
import java.util.*;
import javax.crypto.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ServerManager extends Thread {
  private Socket socket;
  private ServerSocket serverSocket;
  private int port = 3000;
  private HashMap<Integer, Integer> usedPorts;
  private HashMap<Integer, SocketController> sockets;
  private PrintWriter printWriter;
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
        final int newClientPort = setupNewClientEncryption(socket);
        returnNewClientUserIdAndPort(newClientPort);
        socket.close();
      }
    } catch (Exception e) {
      LOGGER.error("Server Exception: " + e.getMessage());
    }
  }

  private void returnNewClientUserIdAndPort(int newClientPort)
      throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException,
          BadPaddingException, InvalidKeyException {
    final String clientData =
        String.format("{userId: %s, port: %d}", UUID.randomUUID(), newClientPort);
    LOGGER.info(String.format("'%s' sent to client", clientData));
    final SocketController clientSocketController = searchSocketsByPort(newClientPort);
    final SecretKey clientSocketControllerSecretKey = clientSocketController.getSecretKey();
    final byte[] encryptedClientData =
        EncryptionDecryptionHandler.encryptMessage(
            clientData.getBytes(), clientSocketControllerSecretKey);
    final String encryptedEncodedClientData =
        EncryptionDecryptionHandler.Base64EncodeMessage(encryptedClientData);
    printWriter.println(encryptedEncodedClientData);
  }

  private int setupNewClientEncryption(Socket socket)
      throws IOException, NoSuchPaddingException, IllegalBlockSizeException,
          NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
    LOGGER.info(
        String.format(
            "Initiation received from client %s:%d",
            socket.getInetAddress().getHostAddress(), port));
    printWriter = new PrintWriter(socket.getOutputStream(), true);
    final int newClientPort = generateClientListenPort();
    final SecretKey secretKey =
        EncryptionDecryptionHandler.handleClientServerEncryptionSetup(printWriter, socket);
    final SocketController socketController = new SocketController();
    socketController.setSecretKey(secretKey);
    socketController.setServerPort(newClientPort);
    sockets.put(newClientPort, socketController);
    final IncomingRequestManager incomingRequestManager =
        new IncomingRequestManager(newClientPort, this);
    incomingRequestManager.start();
    return newClientPort;
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

  public void sendOutgoingMessage(byte[] message) {
    final OutgoingResponseManager outgoingResponseManager =
        new OutgoingResponseManager(this, message);
    outgoingResponseManager.start();
  }

  public List<SocketController> getSockets() {
    return new ArrayList<>(sockets.values());
  }

  public void addSocket(Socket socket, int serverPort) {
    final SocketController socketController = sockets.get(serverPort);
    socketController.setSocket(socket);
    sockets.put(serverPort, socketController);
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

  public SocketController searchSocketsByPort(int port) {
    return sockets.get(port);
  }
}
