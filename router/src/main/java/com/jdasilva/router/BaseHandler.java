package com.jdasilva.router;
import java.net.Socket;

public abstract class BaseHandler implements Handler {
    private Handler next;

    @Override
    public void next(Handler handler) {
        this.next = handler;
    }

    @Override
    public void handle(String message, Socket socket, int clientId, String type) {
        if (next != null) {
            System.out.println(type + " id: " + clientId + " received: " + message);
            next.handle(message, socket, clientId, type);
        }
    }
}
