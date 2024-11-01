package com.jdasilva.router;

import java.net.Socket;

public interface Handler {
    void next(Handler handler);
    void handle(String message, Socket socket, int clientId, String type);
}
