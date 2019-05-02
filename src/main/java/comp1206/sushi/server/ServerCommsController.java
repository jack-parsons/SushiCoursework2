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
    private List<ClientConnection> serverComms = new ArrayList<>();

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(Comms.PORT)) {
            while (true) {
                ClientConnection newServerComm = new ClientConnection(serverSocket.accept());
                serverComms.add(newServerComm);
            }
        } catch (IOException e) {
            e.printStackTrace(); // TODO sort out
        }
    }

    public ClientConnection[] getClientConnections() {
        return serverComms.toArray(new ClientConnection[0]);
    }

    public void removeClientConnection(ClientConnection clientConnection) {
        serverComms.remove(clientConnection);
    }
}

