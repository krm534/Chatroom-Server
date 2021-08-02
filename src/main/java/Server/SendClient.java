package Server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;

public class SendClient extends Thread {
    private Server server;
    private PrintWriter printWriter;
    private Queue<String> messages;

    public SendClient(Server server) {
        this.messages = new LinkedList<>();
        this.server = server;
    }

    @Override
    public void run() {
        while (true) {
            try {
                if (messages.size() > 0) {
                    System.out.println("Message size is greater than 0!");
                    String clientMessage = messages.poll();
                    printWriter.println(clientMessage);
                    System.out.println("'" + clientMessage + "'" + " sent to clients!");
                }

                Thread.sleep(5000);
            } catch (Exception e) {
                System.out.println("Error Received: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void setSocket(Socket socket) {
        try {
            printWriter = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            System.out.println("Error Received: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void appendToMessageQueue(String message) {
        System.out.println(message + " placed in send queue!");
        messages.add(message);
    }
}
