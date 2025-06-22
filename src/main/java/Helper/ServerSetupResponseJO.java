package Helper;

import java.util.List;

public class ServerSetupResponseJO {
  private int clientPort;

  private List<MessagesJO> messages;

  public int getClientPort() {
    return clientPort;
  }

  public void setClientPort(int clientPort) {
    this.clientPort = clientPort;
  }

  public List<MessagesJO> getMessages() {
    return messages;
  }

  public void setMessages(List<MessagesJO> messages) {
    this.messages = messages;
  }
}
