package com.jdasilva.market;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;


public class Market {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private int marketId;

    private static final int port = 5001;
    private static final String host = "localhost";

    private Map<String, Integer> inventory = new HashMap<>();

    public Market(){
        inventory.put("AAPL", 1000);
        inventory.put("GOOGL", 500);
        inventory.put("AMZN", 200);
        inventory.put("MSFT", 300);
    }

    public void start() {
        try{
            socket = new Socket(host, port);
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            marketId = Integer.parseInt(in.readLine());
            System.out.println("Market " + marketId + " connected to the server");

            listenForOrders();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private void listenForOrders(){
        try{
            String order;
            while((order = in.readLine()) != null){
                System.out.println("Market " + marketId + " received: " + order);
                String response = processOrder(order);
                out.println(response);
                System.out.println("Market " + marketId + " sent: " + response);
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    private String processOrder(String order){
        Map <String, String> fields = parseFIXMessage(order);

        String action = fields.get("35");
        String instrument = fields.get("55");
        int quantity = Integer.parseInt(fields.get("38"));

        boolean executed = false;
        if("D".equals(action)){
            executed = executeBuyOrder(instrument, quantity);
        } else if("F".equals(action)){
            executed = executeSellOrder(instrument, quantity);
        }

        StringBuilder response = new StringBuilder();
        response.append("8=FIX.4.2|");
        response.append("35=").append(executed ? "8" : "9").append("|");
        response.append("49=").append(marketId).append("|");
        response.append("56=").append(fields.get("49")).append("|");
        response.append("55=").append(instrument).append("|");
        response.append("38=").append(quantity).append("|");
        response.append("10=").append(calculateCheckSum(response.toString())).append("|");

        return response.toString();
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

    private boolean executeBuyOrder(String instrument, int quantity){
        if(inventory.containsKey(instrument) && inventory.get(instrument) >= quantity){
            inventory.put(instrument, inventory.get(instrument) - quantity);
            return true;
        }
        return false;
    }

    private boolean executeSellOrder(String instrument, int quantity){
        inventory.put(instrument, inventory.getOrDefault(instrument, 0) + quantity);
        return true;
    }

    private int calculateCheckSum(String message){
        int sum = 0;
        for(int i = 0; i < message.length(); i++){
            sum += message.charAt(i);
        }
        return sum % 256;
    }

    public void close(){
        try{
            if(out != null) out.close();
            if(in != null) in.close();
            if (socket != null) socket.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
