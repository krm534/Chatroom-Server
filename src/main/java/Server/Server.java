package Server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Server extends Thread  {
    private Socket socket;
    private ServerSocket serverSocket;
    private int port = 3000;
    private PrintWriter printWriter;
    private HashMap<Integer, Integer> existingClientPorts;
    private ArrayList<SendClient> sendClients;

    public Server() {
        try {
            sendClients = new ArrayList<>();
            existingClientPorts = new HashMap<>();
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                System.out.println("Waiting for connection on port " + port);
                socket = serverSocket.accept();
                System.out.println("Initiation message received from client " + socket.getInetAddress().getHostAddress());

                // Create new client listener and sender threads
                int newClientPort = generateClientListenPort();
                SendClient sendClient = new SendClient(this);
                sendClients.add(sendClient);
                ReceiveClient receiveClient = new ReceiveClient(newClientPort, this, sendClient);
                sendClient.start();
                receiveClient.start();

                // Send new client port info back to client
                printWriter = new PrintWriter(socket.getOutputStream(), true);
                printWriter.println(newClientPort);
                System.out.println("Port " + newClientPort + " sent to client!");

                // Close socket
                socket.close();
            }
        } catch (IOException e) {
            System.out.println("Error Received: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private int generateClientListenPort() {
        Random random = new Random();
        int randomPort = random.nextInt(65535) + 1024;
        System.out.println("Random port generated is " + randomPort);

        if (existingClientPorts.containsValue(randomPort) || randomPort > 65535 || randomPort < 1025) {
            generateClientListenPort();
        }

        existingClientPorts.put(randomPort, randomPort);
        return randomPort;
    }

    public void addToReceiveClientList(String message) {
        System.out.println("Number of client receivers: " + sendClients.size());
        for (SendClient clients : sendClients) {
            clients.appendToMessageQueue(message);
            System.out.println("Append to message queue sent out!");
        }
    }
}
