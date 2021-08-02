package Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class ReceiveClient extends Thread {
    private Server server;
    private ServerSocket serverSocket;
    private Socket socket;
    private Scanner scanner;
    private SendClient sendClient;

    public ReceiveClient(int port, Server server, SendClient sendClient) {
        try {
            this.serverSocket = new ServerSocket(port);
            this.server = server;
            this.sendClient = sendClient;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            socket = serverSocket.accept();
            scanner = new Scanner(socket.getInputStream());
            sendClient.setSocket(socket);

            while (true) {
                System.out.println("Listening for traffic on port " + serverSocket.getLocalPort());
                String clientMessage = scanner.nextLine();
                System.out.println("Client Message: " + clientMessage);

                if (!clientMessage.equals("Confirmation")) {
                    server.addToReceiveClientList(clientMessage);
                }
            }
        } catch (IOException e) {
            System.out.println("Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
