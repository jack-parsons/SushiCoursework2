package comp1206.sushi.server;

import comp1206.sushi.common.Comms;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ServerCommsController implements Runnable {
    private List<ServerComms> serverComms = new ArrayList<>();

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(Comms.PORT)) {
            while (true) {
                ServerComms newServerComm = new ServerComms(serverSocket.accept());
                serverComms.add(newServerComm);
            }
        } catch (IOException e) {
            e.printStackTrace(); // TODO sort out
        }
    }

    public void sendMessage(String message, String clientUsername) {
        for (ServerComms serverComms : this.serverComms) {
            if (serverComms.getUsername().equals(clientUsername)) {
                serverComms.sendMessage(message);
            }
        }
    }

    public String recieveMessage(String clientUsername) throws IOException {
        for (ServerComms serverComms : this.serverComms) {
            if (serverComms.getUsername().equals(clientUsername)) {
                return serverComms.receiveMessage();
            }
        }
        return null;
    }

    public String[] getUsernames () {
        String[] users = new String[serverComms.size()];
        for (int i = 0; i < users.length; i ++) {
            users[i] = serverComms.get(i).getUsername();
        }
        return users;
    }
}

class ServerComms extends Comms{
    private String username;

    ServerComms(Socket socket) throws IOException {
        this.socket = socket;
        connectionOutput = new PrintWriter(socket.getOutputStream(), true);
        connectionInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        System.out.println("User connected");

        new Thread(this::checkInput).start();
    }

    private void checkInput() {
        try {
            username=receiveMessageWait();
            sendMessage("NEW_USER|USERNAME=%s|ADDRESS=%s|POSTCODE=%s|");
        } catch (IOException e) {
            System.out.println("User connected");
        }
        System.out.println("User joined: " + username);
    }

    public String getUsername() {
        return username;
    }
}
