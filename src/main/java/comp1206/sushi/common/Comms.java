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
        NEW_USER("NEW_USER"),
        CLEAR_POSTCODES("CLEAR_POSTCODES"),
        ADD_POSTCODE("ADD_POSTCODE"),
        CLEAR_DISHES("CLEAR_DISHES"),
        ADD_DISH("ADD_DISH"),
        ADD_RESTAURANT("ADD_RESTAURANT"),
        CLEAR_ORDERS("CLEAR_ORDERS"),
        CANCEL_ORDER("CANCEL_ORDER"),
        ADD_ORDER("ADD_ORDER"),
        LOGIN_REJECTED("LOGIN_REJECTED"),
        BASKET_UPDATE("BASKET_UPDATE"),
        FINISH_INIT("FINISH_INIT");

        String name;
        MessageType (String name) {
            this.name = name;
        }
    }

    public enum MessageAttribute {
        USERNAME("USERNAME"),
        PASSWORD("PASSWORD"),
        ADDRESS("ADDRESS"),
        POSTCODE("POSTCODE"),
        NAME("NAME"),
        DESCRIPTION("DESCRIPTION"),
        PRICE("PRICE"),
        DISHES("DISHES");

        String name;
        MessageAttribute (String name) {
            this.name = name;
        }
    }

    public void sendMessage(String message) {
        if (socket.isConnected()) {
            connectionOutput.println(message);
            System.out.println(message);
        } else {
            System.err.println("Not connected");
        }
    }

    public String receiveMessage() throws IOException {
        if (socket.isConnected() && !socket.isClosed())
            return connectionInput.readLine();
        return null;
    }

    public String receiveMessageWait() throws IOException {
        String in = null;
        while (socket.isConnected() && (in=connectionInput.readLine()) == null) {
            System.out.println("asd "+in);
        }
        System.out.println("skjhdsdkjfs" + in + "123213");
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

    public static String extractMessageAttribute(String message, MessageAttribute messageAttribute) {
        for (String part : message.split("\\|")) {
            if (part.matches("( *)" + messageAttribute.name + "( *)=.*")) {
                return part.substring(part.indexOf("=")+1);
            }
        }
        return null;
    }

    public void disconnect() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
