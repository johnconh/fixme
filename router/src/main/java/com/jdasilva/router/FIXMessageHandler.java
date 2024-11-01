package com.jdasilva.router;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;



public class FIXMessageHandler extends BaseHandler {
    private Router router;

    public FIXMessageHandler(Router router) {
        this.router = router;
    }

    @Override
    public void handle(String message, Socket socket, int clientId, String type) {
        Map<String, String> fields = parseFIXMessage(message);
        String destinationIdString = fields.get("56");
        Socket destination;
        if("MARKET".equals(destinationIdString)){
            destination = findMarketSocket();
        } else {
            int destinationId = Integer.parseInt(destinationIdString);
            destination = getDestination(destinationId);
        }

        if(destination != null){
            fowardMessage(message, destination);
        } else {
            System.out.println("Error: Destination not found");
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

    private Socket getDestination(int destinationId) {
        if (router.getBrokers().containsKey(destinationId)) {
            return router.getBrokers().get(destinationId);
        } else if (router.getMarkets().containsKey(destinationId)) {
            return router.getMarkets().get(destinationId);
        }
        return null;
    }

    private void fowardMessage(String message, Socket destination) {
        try { 
            PrintWriter writer = new PrintWriter((destination.getOutputStream()), true); 
            writer.println(message);
            System.out.println("Message fowarded: " + message);
        } catch (Exception e) {
            System.out.println("Error: Message could not be fowarded");
            e.printStackTrace();
        }
    }

    private Socket findMarketSocket() {
        return router.getMarkets().values().stream().findAny().orElse(null);
    }
    
}
