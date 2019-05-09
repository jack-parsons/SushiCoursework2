package comp1206.sushi.server;

import comp1206.sushi.common.Comms;
import comp1206.sushi.common.Order;
import comp1206.sushi.common.User;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientConnection extends Comms {
    private User user;
    private boolean updated = false;

    ClientConnection(Socket socket) throws IOException {
        this.socket = socket;
        connectionOutput = new PrintWriter(socket.getOutputStream(), true);
        connectionInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        System.out.println("Client connected");
    }

    public void setUser(User user) {
        this.user = user;
    }

    public boolean checkUpdated() {
        boolean t = updated;
        updated = true;
        return t;
    }

    public User getUser() {
        return user;
    }
}
