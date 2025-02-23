import Helper.Constants;
import Helper.KeyType;
import com.google.common.primitives.Bytes;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.*;
import java.util.Base64;
import java.util.Scanner;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionDecryptionHandler {
  public static SecretKey handleClientServerEncryptionSetup(PrintWriter printWriter, Socket socket)
      throws NoSuchAlgorithmException, IOException, NoSuchPaddingException, InvalidKeyException,
          IllegalBlockSizeException, BadPaddingException {
    final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KeyType.RSA.name());
    final KeyPair keyPair = keyPairGenerator.generateKeyPair();
    sendGeneratedAsymmetricKeyToClient(printWriter, keyPairGenerator, keyPair);
    return decryptSymmetricKeyFromClient(socket, keyPair);
  }

  private static void sendGeneratedAsymmetricKeyToClient(
      PrintWriter printWriter, KeyPairGenerator keyPairGenerator, KeyPair keyPair) {
    final SecureRandom secureRandom = new SecureRandom();
    keyPairGenerator.initialize(Constants.RSA_DEFAULT_ENCRYPTION_KEY_SIZE, secureRandom);
    final byte[] publicKeyMessage = Bytes.concat(keyPair.getPublic().getEncoded());
    printWriter.println(Base64.getEncoder().encodeToString(publicKeyMessage));
  }

  private static SecretKey decryptSymmetricKeyFromClient(Socket socket, KeyPair keyPair)
      throws IllegalBlockSizeException, BadPaddingException, IOException, InvalidKeyException,
          NoSuchPaddingException, NoSuchAlgorithmException {
    final Cipher cipher = Cipher.getInstance(KeyType.RSA.name());
    final Scanner scanner = new Scanner(socket.getInputStream());
    final String message = scanner.nextLine();
    cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
    final byte[] decryptedSymmetricKey = cipher.doFinal(Base64.getDecoder().decode(message));
    return new SecretKeySpec(
        decryptedSymmetricKey,
        0,
        // Number of bytes rather than bits
        Constants.AES_DEFAULT_ENCRYPTION_KEY_SIZE / 8,
        KeyType.AES.name());
  }

  public static byte[] decryptMessage(byte[] message, SecretKey secretKey)
      throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException,
          IllegalBlockSizeException, BadPaddingException {
    final Cipher cipher = Cipher.getInstance(KeyType.AES.name());
    cipher.init(Cipher.DECRYPT_MODE, secretKey);
    final byte[] decryptedMessage = cipher.doFinal(message);
    return decryptedMessage;
  }

  public static byte[] encryptMessage(byte[] message, SecretKey secretKey)
      throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException,
          IllegalBlockSizeException, BadPaddingException {
    final Cipher cipher = Cipher.getInstance(KeyType.AES.name());
    cipher.init(Cipher.ENCRYPT_MODE, secretKey);
    return cipher.doFinal(message);
  }

  public static String Base64EncodeMessage(byte[] encryptedMessage) {
    final String encodedClientData = Base64.getEncoder().encodeToString(encryptedMessage);
    return encodedClientData;
  }
}
