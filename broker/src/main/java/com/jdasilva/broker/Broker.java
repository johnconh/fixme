package com.jdasilva.broker;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class Broker {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private int brokerId;

    private static final int port = 5000;
    private static final String host = "localhost";

    public void start() {
        try{
            socket = new Socket(host, port);
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            brokerId = Integer.parseInt(in.readLine());
            System.out.println("Broker " + brokerId + " connected to the server");

            sendOrder("Buy", "AAPL", 100, 150.0);
            //sendOrder("Sell", "GOOGL", 50, 100.0);

            listenForResponses();
        }catch(IOException e){
            System.err.println("Broker failed to connect to the server");
            e.printStackTrace();
        }
    }

    private void  sendOrder(String action, String instrument, int quantity, double price){
        String message = formatFIXMessage(action, instrument, quantity, price);
        out.println(message);
        System.out.println("Broker " + brokerId + " sent: " + message);
    }

    private String formatFIXMessage(String action, String instrument, int quantity, double price){
        StringBuilder sb = new StringBuilder();
        sb.append("8=FIX.4.2|");
        sb.append("35=").append(action.equals("Buy") ? "D" : "F").append("|");
        sb.append("49=").append(brokerId).append("|");
        sb.append("56=MARKET|");
        sb.append("55=").append(instrument).append("|");
        sb.append("38=").append(quantity).append("|");
        sb.append("44=").append(price).append("|");
        String message = sb.toString();
        sb.append("10=").append(calculateCheckSum(message)).append("|");
        return sb.toString();
    }

    private int calculateCheckSum(String message){
        int sum = 0;
        for (char c : message.toCharArray()) {
            sum += c;
        }
        return sum % 256;
    }

    private void listenForResponses(){
        try{
            String response;
            while((response = in.readLine()) != null){
                System.out.println("Market received: " + response);
            }
        }catch(Exception e){
            e.printStackTrace();
        }
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
