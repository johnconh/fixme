package com.jdasilva.broker;

public abstract class BaseHandler implements Handler {
    private Handler next;

    @Override
    public void next(Handler handler) {
        this.next = handler;
    }

    @Override
    public void handle(StringBuilder message, String action, String instrument, int quantity, double price) {
        if (next != null) {
            next.handle(message, action, instrument, quantity, price);
        }
    }
}
