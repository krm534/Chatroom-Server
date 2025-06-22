package DatabaseHelper;

import jakarta.persistence.*;

@Entity
@Table(name = "messages")
public class MessagesDO {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private int id;

  @Column(name = "message")
  private String message;

  @Lob
  @Column(name = "attachedB64Image")
  private String attachedB64Image;

  @ManyToOne(fetch = FetchType.EAGER)
  private UsersDO usersDO;

  @Column(name = "timestamp")
  private String timestamp;

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

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

  public String getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }

  public UsersDO getUsersDO() {
    return usersDO;
  }

  public void setUsersDO(UsersDO usersDO) {
    this.usersDO = usersDO;
  }
}
