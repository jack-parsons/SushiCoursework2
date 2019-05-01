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

//        new Thread(this::checkInput).start();
    }

//    private void checkInput() {
//        try {
//            String reply = receiveMessageWait();
//            System.out.println(reply);
//            switch (Comms.extractMessageType(reply)) {
//                case LOGIN:
//                case REGISTER:
//                    username=Comms.extractMessageAttribute(reply, MessageAttribute.USERNAME);
//                    address =
//            }
//            sendMessage(String.format("NEW_USER|USERNAME=%s|ADDRESS=%s|POSTCODE=%s|", username, address, postcode));
//        } catch (IOException e) {
//            System.out.println("User connected");
//        }
//        System.out.println("User joined: " + username);
//    }

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

    public void sendUser(User user) {

    }
}
