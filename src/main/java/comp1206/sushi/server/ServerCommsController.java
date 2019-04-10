package comp1206.sushi.server;

import comp1206.sushi.comms.Comms;

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
                serverComms.add(new ServerComms(serverSocket.accept()));
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
                return serverComms.revieveMessage();
            }
        }
        return null;
    }
}

class ServerComms extends Comms{
    private String username;

    ServerComms(Socket socket) throws IOException {
        connectionOutput = new PrintWriter(socket.getOutputStream(), true);
        connectionInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public String getUsername() {
        return username;
    }
}
