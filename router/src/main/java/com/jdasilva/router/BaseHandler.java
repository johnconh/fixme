package com.jdasilva.router;
import java.nio.channels.SocketChannel;

public abstract class BaseHandler implements Handler {
    private Handler next;

    @Override
    public void next(Handler handler) {
        this.next = handler;
    }

    @Override
    public void handle(String message, SocketChannel socket, int clientId, String type) throws Exception {
        if (next != null) {
            System.out.println(type + " id: " + clientId + " received: " + message);
            next.handle(message, socket, clientId, type);
        }
    }
}
