package com.jdasilva.market;
import java.io.PrintWriter;
import java.util.Map;

public abstract class BaseHandler implements Handler {
    private Handler next;

    @Override
    public void next(Handler handler) {
        this.next = handler;
    }

    @Override
    public void handle(String message, int clientId, PrintWriter out,  Map<String, Integer> inventory) {
        if (next != null) {
            System.out.println("Market id: " + clientId + " received: " + message);
            next.handle(message, clientId, out, inventory);
        }
    }
}
