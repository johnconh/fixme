package com.jdasilva.router;

import java.nio.channels.SocketChannel;

public interface Handler {
    void next(Handler handler);
    void handle(String message, SocketChannel socket, int clientId, String type) throws Exception;
}
