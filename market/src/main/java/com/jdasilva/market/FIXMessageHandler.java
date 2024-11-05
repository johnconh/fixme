package com.jdasilva.market;
import java.io.PrintWriter;
import java.util.Map;
import java.util.HashMap;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FIXMessageHandler extends BaseHandler {
    @Override
    public void handle(String message, int clientId, PrintWriter out, Map<String, Integer> inventory) {
        System.out.println("Message received: " + message);
        String response = processOrder(message, clientId, inventory);
        out.println(response);
        System.out.println("Message sent: " + response);
    }

    private String processOrder(String order, int clientId, Map<String, Integer> inventory){
        Map <String, String> fields = parseFIXMessage(order);

        String action = fields.get("35");
        String instrument = fields.get("55");
        int quantity = Integer.parseInt(fields.get("38"));
        double price = Double.parseDouble(fields.get("44"));
        int brokerId = Integer.parseInt(fields.get("49"));

        boolean executed = false;
        if("D".equals(action)){
            executed = executeBuyOrder(instrument, quantity, inventory);
        }else if("F".equals(action)){
            executed = executeSellOrder(instrument, quantity, inventory);
        }

        return createFIXMessage(executed, instrument, quantity, price, clientId, brokerId);
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

    private boolean executeBuyOrder(String instrument, int quantity, Map<String, Integer> inventory){
        if(inventory.containsKey(instrument) && inventory.get(instrument) >= quantity){
            inventory.put(instrument, inventory.get(instrument) - quantity);
            return true;
        }
        return false;
    }

    private boolean executeSellOrder(String instrument, int quantity, Map<String, Integer> inventory){
        inventory.put(instrument, inventory.getOrDefault(instrument, 0) + quantity);
        return true;
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
