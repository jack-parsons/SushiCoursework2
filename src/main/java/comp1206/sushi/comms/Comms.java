package comp1206.sushi.comms;

import java.io.*;
import java.net.Socket;

public abstract class Comms {
    public final static int PORT = 1357;
    protected final static String HOST = "127.0.0.1";
    protected Socket socket;
    protected PrintWriter connectionOutput;
    protected BufferedReader connectionInput;

    public void sendMessage(String message) {
        if (socket.isConnected()) {
            connectionOutput.print(message);
        }
    }

    public String revieveMessage() throws IOException {
        if (socket.isConnected()) {
            return connectionInput.readLine();
        }
        return null;
    }
}
