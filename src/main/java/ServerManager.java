import DatabaseHelper.DatabaseQuery;
import DatabaseHelper.MessagesDO;
import Helper.MessagesJO;
import Helper.ServerSetupResponseJO;
import Helper.SocketControllerJO;
import com.google.gson.Gson;
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
  private HashMap<Integer, SocketControllerJO> sockets;
  private PrintWriter printWriter;
  private final Logger LOGGER = LogManager.getLogger(ServerManager.class.getName());
  private Gson gson;

  public ServerManager() {
    try {
      usedPorts = new HashMap<>();
      sockets = new HashMap<>();
      this.gson = new Gson();
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
        final int newClientPort = setupNewClientEncryption();
        returnNewClientSocketInfo(newClientPort);
        socket.close();
      }
    } catch (Exception e) {
      LOGGER.error("Server Exception: " + e.getMessage());
    }
  }

  private void returnNewClientSocketInfo(int newClientPort)
      throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException,
          BadPaddingException, InvalidKeyException {
    final List<MessagesJO> recentMessages = getRecentMessages();
    final ServerSetupResponseJO serverSetupResponseJO = new ServerSetupResponseJO();
    serverSetupResponseJO.setClientPort(newClientPort);
    serverSetupResponseJO.setMessages(recentMessages);
    final SocketControllerJO clientSocketControllerJO = searchSocketsByPort(newClientPort);
    final SecretKey clientSocketControllerSecretKey = clientSocketControllerJO.getSecretKey();
    final String encryptedEncodedClientData =
        EncryptionDecryptionHandler.encryptMessage(
            this.gson.toJson(serverSetupResponseJO).getBytes(), clientSocketControllerSecretKey);
    printWriter.println(encryptedEncodedClientData);
  }

  private List<MessagesJO> getRecentMessages() {
    final List<MessagesDO> messagesDO =
        (List<MessagesDO>)
            DatabaseHandler.handleQuery(
                DatabaseQuery.DatabaseQuery_GetRecentMessagesFromMessagesTable, null);
    final List<MessagesJO> messages = new ArrayList<>();
    for (final MessagesDO messageDO : messagesDO) {
      final MessagesJO message = new MessagesJO();
      message.setMessage(messageDO.getMessage());
      message.setUserId(messageDO.getUsersDO().getUserId());
      message.setAttachedB64Image(messageDO.getAttachedB64Image());
      message.setTimestamp(messageDO.getTimestamp());
      messages.add(message);
    }
    LOGGER.info(String.format("Recent messages size is: %d", messages.size()));
    return messages;
  }

  private int setupNewClientEncryption()
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
    final SocketControllerJO socketControllerJO = new SocketControllerJO();
    socketControllerJO.setSecretKey(secretKey);
    socketControllerJO.setServerPort(newClientPort);
    sockets.put(newClientPort, socketControllerJO);
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

  public void sendOutgoingMessage(MessagesJO message) {
    final OutgoingResponseManager outgoingResponseManager =
        new OutgoingResponseManager(this, message);
    outgoingResponseManager.start();
  }

  public List<SocketControllerJO> getSockets() {
    return new ArrayList<>(sockets.values());
  }

  public void addSocket(Socket socket, int serverPort) {
    final SocketControllerJO socketControllerJO = sockets.get(serverPort);
    socketControllerJO.setSocket(socket);
    sockets.put(serverPort, socketControllerJO);
  }

  public void removeSocket(int socketPort) {
    DatabaseHandler.handleQuery(
        DatabaseQuery.DatabaseQuery_RemoveSocketInfo, searchSocketsByPort(socketPort).getUserId());
    sockets.remove(socketPort);
    LOGGER.info(
        String.format(
            "Socket for port %d has been removed from list of existing client sockets",
            socketPort));
  }

  public SocketControllerJO searchSocketsByPort(int port) {
    return sockets.get(port);
  }
}
