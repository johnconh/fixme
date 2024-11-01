package com.jdasilva.market;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;


public class Market {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private int marketId;
    private Handler handler;
    private static final int port = 5001;
    private static final String host = "localhost";

    private Map<String, Integer> inventory = new HashMap<>();

    public Market(){
        inventory.put("AAPL", 1000);
        inventory.put("GOOGL", 500);
        inventory.put("AMZN", 200);
        inventory.put("MSFT", 300);
        buildChain();
    }

    private void buildChain(){
        handler = new FIXMessageHandler();
        Handler inventoryHandler = new InventoryHandler();
        handler.next(inventoryHandler);
    }

    public void start() {
        try{
            socket = new Socket(host, port);
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            marketId = Integer.parseInt(in.readLine());
            System.out.println("Market " + marketId + " connected to the server");

            String order;
            while((order = in.readLine()) != null){
                handler.handle(order, marketId, out, inventory);
            }
        }catch(Exception e){
            e.printStackTrace();
        }finally{
            close();
        }
    }

    public void close(){
        try{
            if(out != null) out.close();
            if(in != null) in.close();
            if (socket != null) socket.close();
        }catch(Exception e){
            System.out.println("Error closing the socket");
            e.printStackTrace();
        }
    }
}
