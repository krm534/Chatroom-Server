package Helper;

public class MessagesJO {
  private String message;
  private String attachedB64Image;
  private String userId;
  private MessageType messageType;
  private String timestamp;

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getAttachedB64Image() {
    return attachedB64Image;
  }

  public void setAttachedB64Image(String attachedB64Image) {
    this.attachedB64Image = attachedB64Image;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public MessageType getMessageType() {
    return messageType;
  }

  public void setMessageType(MessageType messageType) {
    this.messageType = messageType;
  }

  public String getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }
}
