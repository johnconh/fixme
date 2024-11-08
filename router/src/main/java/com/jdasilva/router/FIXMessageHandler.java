package com.jdasilva.router;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;


public class FIXMessageHandler extends BaseHandler {
    private Router router;

    public FIXMessageHandler(Router router) {
        this.router = router;
    }

    @Override
    public void handle(String message, SocketChannel socket, int clientId, String type) throws Exception {
        Map<String, String> fields = parseFIXMessage(message);
        String destinationIdString = fields.get("56");
        SocketChannel destination;
        if("MARKET".equals(destinationIdString)){
            destination = findMarketSocket();
        } else {
            int destinationId = Integer.parseInt(destinationIdString);
            destination = getDestination(destinationId);
        }

        if(destination != null){
            fowardMessage(message, destination, clientId);
        } else {
            throw new IOException("Destination not found");
        }
        super.handle(message, socket, clientId, type);
    }

    private Map<String, String> parseFIXMessage(String message) {
        Map<String, String> fields = new HashMap<>();
        String[] parts = message.split("\\|");
        for (String part : parts) {
            String[] field = part.split("=");
            fields.put(field[0], field[1]);
        }
        return fields;
    }

    private SocketChannel getDestination(int destinationId) {
        if (router.getBrokers().containsKey(destinationId)) {
            return router.getBrokers().get(destinationId);
        } else if (router.getMarkets().containsKey(destinationId)) {
            return router.getMarkets().get(destinationId);
        }
        return null;
    }

    private void fowardMessage(String message, SocketChannel destination, int clientId) throws Exception  {
        ByteBuffer buffer = ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8)); 
        while (buffer.hasRemaining()) {
            destination.write(buffer);
        }
        System.out.println("Message fowarded: " + message);
    }

    private SocketChannel findMarketSocket() {
        return router.getMarkets().values().stream().findAny().orElse(null);
    }
    
}
