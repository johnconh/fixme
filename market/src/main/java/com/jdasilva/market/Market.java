package com.jdasilva.market;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Market {
    private SocketChannel socket;
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
            socket = SocketChannel.open(new InetSocketAddress(host, port));
            socket.configureBlocking(false);
            ByteBuffer buffer = ByteBuffer.allocate(256);
            while (socket.read(buffer) <= 0) {
                Thread.sleep(1000);
            }
            buffer.flip();
            marketId = Integer.parseInt(StandardCharsets.UTF_8.decode(buffer).toString().trim());
            System.out.println("Market " + marketId + " connected to the server");

            listenForOrders();
        }catch(Exception e){
            e.printStackTrace();
        }finally{
            close();
        }
    }

    private void listenForOrders(){
        ByteBuffer buffer = ByteBuffer.allocate(256);
        try{
             while(true){
                buffer.clear();
                int read = socket.read(buffer);
                if (read == -1) {
                    System.out.println("Connection closed by the server");
                    break;
                }
                if (read > 0) {
                    buffer.flip();
                    String order = StandardCharsets.UTF_8.decode(buffer).toString().trim();
                    if (!order.isEmpty() )
                    {
                        System.out.println("Order received: " + order);
                        handler.handle(order, marketId, this::sendResponse, inventory);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendResponse(String response){
        try{
            ByteBuffer buffer = ByteBuffer.wrap(response.getBytes(StandardCharsets.UTF_8));
            while (buffer.hasRemaining()) {
                socket.write(buffer);
            }
        } catch (Exception e) {
            e.printStackTrace();}    
    }

    public void close(){
        try{
            if(socket != null){
                socket.close();
            }
        }catch(Exception e){
            System.out.println("Error closing the socket");
            e.printStackTrace();
        }
    }
}
