import Helper.Constants;
import Helper.KeyType;
import Helper.SocketController;
import com.google.common.primitives.Bytes;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
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
        LOGGER.info(
            String.format(
                "Initiation received from client %s:%d",
                socket.getInetAddress().getHostAddress(), port));

        printWriter = new PrintWriter(socket.getOutputStream(), true);

        final int newClientPort = generateClientListenPort();
        final SecretKey secretKey = handleEncryptionSetup();
        final SocketController socketController = new SocketController();
        socketController.setSecretKey(secretKey);
        socketController.setServerPort(newClientPort);
        sockets.put(newClientPort, socketController);

        final String clientData =
            String.format("{userId: %s, port: %d}", UUID.randomUUID(), newClientPort);
        LOGGER.info(String.format("'%s' sent to client", clientData));
        final String encryptedEncodedClientData = encryptMessage(clientData.getBytes(), secretKey);
        printWriter.println(encryptedEncodedClientData);

        IncomingRequestManager incomingRequestManager =
            new IncomingRequestManager(newClientPort, this);
        incomingRequestManager.start();
        socket.close();
      }
    } catch (IOException
        | NoSuchAlgorithmException
        | NoSuchPaddingException
        | InvalidKeyException
        | IllegalBlockSizeException
        | BadPaddingException e) {
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

  private SecretKey handleEncryptionSetup()
      throws NoSuchAlgorithmException, IOException, NoSuchPaddingException, InvalidKeyException,
          IllegalBlockSizeException, BadPaddingException {
    // Send Public Key to Client
    final SecureRandom secureRandom = new SecureRandom();
    final Cipher cipher = Cipher.getInstance(KeyType.RSA.name());
    final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KeyType.RSA.name());
    keyPairGenerator.initialize(Constants.RSA_DEFAULT_ENCRYPTION_KEY_SIZE, secureRandom);
    final KeyPair keyPair = keyPairGenerator.generateKeyPair();
    final byte[] publicKeyMessage = Bytes.concat(keyPair.getPublic().getEncoded());
    LOGGER.info(String.format("Public Key is %s", Arrays.toString(publicKeyMessage)));
    LOGGER.info(String.format("Public Key length is %d", publicKeyMessage.length));
    printWriter.println(Base64.getEncoder().encodeToString(publicKeyMessage));

    // Wait for and process incoming encrypted Symmetric Key from Client
    final Scanner scanner = new Scanner(socket.getInputStream());
    final String message = scanner.nextLine();
    cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
    final byte[] decryptedSymmetricKey = cipher.doFinal(Base64.getDecoder().decode(message));
    LOGGER.info(String.format("Symmetric Key is %s", Arrays.toString(decryptedSymmetricKey)));
    LOGGER.info(String.format("Symmetric Key length is %d", decryptedSymmetricKey.length));
    return new SecretKeySpec(
        decryptedSymmetricKey,
        0,
        // Number of bytes rather than bits
        Constants.AES_DEFAULT_ENCRYPTION_KEY_SIZE / 8,
        KeyType.AES.name());
  }

  public byte[] decryptMessage(byte[] message, SecretKey secretKey)
      throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException,
          IllegalBlockSizeException, BadPaddingException {
    LOGGER.info(String.format("Encrypted message is '%s'", Arrays.toString(message)));
    LOGGER.info(String.format("Encrypted message length is '%s'", message.length));
    final Cipher cipher = Cipher.getInstance(KeyType.AES.name());
    cipher.init(Cipher.DECRYPT_MODE, secretKey);
    final byte[] decryptedMessage = cipher.doFinal(message);
    LOGGER.info(String.format("Decrypted message is '%s'", Arrays.toString(decryptedMessage)));
    LOGGER.info(String.format("Decrypted message length is '%s'", decryptedMessage.length));
    return decryptedMessage;
  }

  public String encryptMessage(byte[] message, SecretKey secretKey)
      throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException,
          IllegalBlockSizeException, BadPaddingException {
    final Cipher cipher = Cipher.getInstance(KeyType.AES.name());
    cipher.init(Cipher.ENCRYPT_MODE, secretKey);
    final byte[] encryptedByteMessage = cipher.doFinal(message);
    LOGGER.info(String.format("Encrypted message is '%s'", Arrays.toString(encryptedByteMessage)));
    LOGGER.info(String.format("Encrypted message length is %d", encryptedByteMessage.length));
    final String encodedClientData = Base64.getEncoder().encodeToString(encryptedByteMessage);
    LOGGER.info(String.format("Encoded message is '%s'", encodedClientData));
    LOGGER.info(String.format("Encoded message length is %d", encodedClientData.length()));
    return encodedClientData;
  }

  public SocketController searchSocketsByPort(int port) {
    return sockets.get(port);
  }
}
