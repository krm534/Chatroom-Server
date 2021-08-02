package Server;

public class Clients {
    private String name;
    private int port;
    private String ipAddress;

    public Clients(String name, int port) {

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
