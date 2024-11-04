package com.jdasilva.broker;

public interface Handler {
    void handle(StringBuilder message, String action, String instrument, int quantity, double price);
    void next(Handler handler);
}
