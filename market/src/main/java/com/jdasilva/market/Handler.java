package com.jdasilva.market;

import java.io.PrintWriter;
import java.util.Map;

public interface Handler {
    void next(Handler handler);
    void handle(String message, int clientId, PrintWriter out, Map<String, Integer> inventory);
}
