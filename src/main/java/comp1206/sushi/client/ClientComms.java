package comp1206.sushi.client;

import comp1206.sushi.common.Comms;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientComms extends Comms {

    ClientComms() throws IOException {
        socket = new Socket(Comms.HOST, Comms.PORT);
        connectionOutput = new PrintWriter(socket.getOutputStream(), true);
        connectionInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }
}
