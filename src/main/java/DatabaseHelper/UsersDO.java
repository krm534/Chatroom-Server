package DatabaseHelper;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class UsersDO {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int id;

  @OneToMany(mappedBy = "usersDO", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<MessagesDO> messages = new ArrayList<>();

  @Column(name = "userId")
  private String userId;

  @Column(name = "portNumber")
  private int portNumber;

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public int getPortNumber() {
    return portNumber;
  }

  public void setPortNumber(int portNumber) {
    this.portNumber = portNumber;
  }

  public List<MessagesDO> getMessages() {
    return messages;
  }

  public void setMessages(List<MessagesDO> messages) {
    this.messages = messages;
  }
}
