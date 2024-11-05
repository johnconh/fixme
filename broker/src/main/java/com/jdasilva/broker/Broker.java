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
    private StringBuilder message;
    private Handler handler;

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
            sendOrder("Sell", "GOOGL", 50, 100.0);

            listenForResponses();
        }catch(IOException e){
            System.err.println("Broker failed to connect to the server");
            e.printStackTrace();
        }
    }

    private void  sendOrder(String action, String instrument, int quantity, double price){
        message = new StringBuilder();
        handler = new FIXMessageHandler(brokerId);
        handler.handle(message, action, instrument, quantity, price);
        out.println(message.toString());
        System.out.println("Message sent: " + message.toString());
    }


    private void listenForResponses(){
        try{
            String response;
            while((response = in.readLine()) != null){
                System.out.println("Message received: " + response);
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
