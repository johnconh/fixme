package com.jdasilva.market;
import java.util.Map;
import java.util.function.Consumer;
import java.util.HashMap;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FIXMessageHandler extends BaseHandler {
    @Override
    public void handle(String message, int clientId, Consumer<String> out, Map<String, Integer> inventory, Map<String, Double> prices) {
        String response = processOrder(message, clientId, inventory, prices);
        out.accept(response);
        System.out.println("Message sent: " + response);
    }

    private String processOrder(String order, int clientId, Map<String, Integer> inventory, Map<String, Double> prices){
        Map <String, String> fields = parseFIXMessage(order);

        String action = fields.get("35");
        String instrument = fields.get("55");
        int quantity = Integer.parseInt(fields.get("38"));
        double brokerPrice = Double.parseDouble(fields.get("44"));
        int brokerId = Integer.parseInt(fields.get("49"));

        boolean executed = false;
        if("D".equals(action)){
            executed = executeBuyOrder(instrument, quantity, inventory, prices, brokerPrice);
        }else if("F".equals(action)){
            executed = executeSellOrder(instrument, quantity, inventory, prices, brokerPrice);
        }

        return createFIXMessage(executed, instrument, quantity, brokerPrice, clientId, brokerId);
    }

    private Map<String, String> parseFIXMessage(String order){
        Map<String, String> fields = new HashMap<>();
        String[] pairs = order.split("\\|");
        for(String pair : pairs){
            String[] keyValue = pair.split("=");
            fields.put(keyValue[0], keyValue[1]);
        }
        return fields;
    }

    private boolean executeBuyOrder(String instrument, int quantity, Map<String, Integer> inventory, Map<String, Double> prices, double brokerPrice){
        if(inventory.containsKey(instrument)){
            if(prices.get(instrument) > brokerPrice || inventory.get(instrument) < quantity){
                System.err.println("Price of " + instrument + " is higher than the broker price or insufficient quantity");
                return false;
            }
            inventory.put(instrument, inventory.get(instrument) - quantity);
            adjustprice(instrument, prices, true);
            return true;
        }
        return false;
    }

    private boolean executeSellOrder(String instrument, int quantity, Map<String, Integer> inventory, Map<String, Double> prices, double brokerPrice){
        if(prices.containsKey(instrument) && prices.get(instrument) > brokerPrice){
            System.err.println("Price of " + instrument + " is lower than the broker price");
            return false;
        }
        inventory.put(instrument, inventory.getOrDefault(instrument, 0) + quantity);
        prices.put(instrument, brokerPrice);
        adjustprice(instrument, prices, false);
        return true;
    }

    private void adjustprice(String instrument, Map<String, Double> prices, boolean increase){
        double price = prices.get(instrument);
        if(increase){
            prices.put(instrument, price * 1.1);
            System.out.println("==== Price of " + instrument + " increased to " + prices.get(instrument) + " ====");
        }else{
            prices.put(instrument, price * 0.9);
            System.out.println("=== Price of " + instrument + " decreased to " + prices.get(instrument) + " ====");
        }
    }
    private String createFIXMessage(boolean executed, String instrument, int quantity, double price, int clientId, int brokerId){
        StringBuilder response = new StringBuilder();
        response.append("8=FIX.4.2|");
        response.append("35=").append(executed ? "8" : "9").append("|");
        response.append("49=").append(clientId).append("|");
        response.append("56=").append(brokerId).append("|");
        response.append("55=").append(instrument).append("|");
        response.append("38=").append(quantity).append("|");
        response.append("44=").append(price).append("|");
        String timestamp = new SimpleDateFormat("yyyyMMdd-HH:mm:ss.SSS").format(new Date());
        response.append("52=").append(timestamp).append("|");
        String message = response.toString();
        response.append("10=").append(calculateCheckSum(message)).append("|");

        return response.toString();
    }

    private int calculateCheckSum(String message){
        int startIndex = message.indexOf("|") + 1;
        String messageToCheck = message.substring(startIndex);
        int sum = 0;
        for(char c : messageToCheck.toCharArray()){
            sum += c;
        }
        return sum % 256;
    }
}
