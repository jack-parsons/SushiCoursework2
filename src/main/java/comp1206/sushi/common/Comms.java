package comp1206.sushi.common;

import java.io.*;
import java.net.Socket;

public abstract class Comms {
    public final static int PORT = 1357;
    protected final static String HOST = "127.0.0.1";

    protected Socket socket;
    protected PrintWriter connectionOutput;
    protected BufferedReader connectionInput;

    public enum MessageType {
        LOGIN("LOGIN"),
        REGISTER("REGISTER"),
        NEW_USER("NEW_USER");

        String name;
        MessageType (String name) {
            this.name = name;
        }
    }

    public enum MessageAttribute {
        USERNAME("USERNAME"),
        PASSWORD("PASSWORD"),
        ADDRESS("ADDRESS"),
        POSTCODE("POSTCODE");

        String name;
        MessageAttribute (String name) {
            this.name = name;
        }
    }

    public void sendMessage(String message) {
        if (socket.isConnected()) {
            connectionOutput.println(message);
        } else {
            System.err.println("Not connected");
        }
    }

    public String receiveMessage() throws IOException {
        if (socket.isConnected())
            return connectionInput.readLine();
        return null;
    }

    public String receiveMessageWait() throws IOException {
        String in = null;
        while (socket.isConnected() && (in=connectionInput.readLine()) == null){}
        return in;
    }

    public static MessageType extractMessageType(String message) {
        if (message.split("\\|").length > 0) {
            String type = message.split("\\|")[0];
            for (MessageType messageType : MessageType.values()) {
                if (type.equals(messageType.name)) {
                    return messageType;
                }
            }
        }
        return null;
    }

    public static String extractMessageAttribute(String message, MessageAttribute messageType) {
        for (String part : message.split("\\|")) {
            if (message.equals(messageType.name)) {
                return message.substring(part.substring(messageType.name.length()).indexOf("="));
            }
        }
        return null;
    }
}
