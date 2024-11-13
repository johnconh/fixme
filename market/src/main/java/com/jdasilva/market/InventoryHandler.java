package com.jdasilva.market;
import java.util.Map;
import java.util.function.Consumer;
import java.util.HashMap;

public class InventoryHandler extends BaseHandler {

    @Override
    public void handle(String message, int clientId, Consumer<String> out, Map<String, Integer> inventory, Map<String, Double> prices) {
        Map <String, String> fields = parseFIXMessage(message);
        String instrument = fields.get("55");
        int quantity = Integer.parseInt(fields.get("38"));
        double price = Double.parseDouble(fields.get("44"));
        inventory.put(instrument, quantity);
        prices.put(instrument, price);
        super.handle(message, clientId, out, inventory, prices);
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
