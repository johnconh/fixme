package com.jdasilva.market;
import java.io.PrintWriter;
import java.util.Map;
import java.util.HashMap;

public class InventoryHandler extends BaseHandler {

    @Override
    public void handle(String message, int clientId, PrintWriter out, Map<String, Integer> inventory) {
        Map <String, String> fields = parseFIXMessage(message);
        String instrument = fields.get("55");
        int quantity = Integer.parseInt(fields.get("38"));
        inventory.put(instrument, quantity);
        super.handle(message, clientId, out, inventory);
    }

    private Map<String, String> parseFIXMessage(String message){
        Map<String, String> fields = new HashMap<>();
        String[] pairs = message.split("\\|");
        for(String pair : pairs){
            String[] keyValue = pair.split("=");
            fields.put(keyValue[0], keyValue[1]);
        }
        return fields;
    }
}
