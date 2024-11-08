package com.jdasilva.market;
import java.util.Map;
import java.util.function.Consumer;

public interface Handler {
    void next(Handler handler);
    void handle(String message, int clientId, Consumer<String> response, Map<String, Integer> inventory);
}
