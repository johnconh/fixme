package com.jdasilva.broker;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Broker {
    private SocketChannel socket;
    private int brokerId;
    private StringBuilder message;
    private Handler handler;
    private static final int port = 5000;
    private static final String host = "localhost";
    private Scanner scanner;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    public void start() {
        try{
            try {
                socket = SocketChannel.open(new InetSocketAddress(host, port));
                socket.configureBlocking(false);
                ByteBuffer buffer = ByteBuffer.allocate(256);
                while (socket.read(buffer) <= 0) {
                    Thread.sleep(1000);
                }
                socket.read(buffer);
                buffer.flip();
                brokerId = Integer.parseInt(StandardCharsets.UTF_8.decode(buffer).toString().trim());
                System.out.println("Broker " + brokerId + " connected to the server");

                sendOrder("Buy", "AAPL", 100, 150.0);
                sendOrder("Sell", "GOOGL", 50, 100.0);
                listenForResponses();
                // executorService.submit(this::listenForResponses);
                // processOrders();

            } catch (IOException e) {
                System.err.println("Broker failed to connect to the server");
            }
        }catch(Exception e){
            System.err.println("Broker failed to connect to the server");
        } finally {
            close();
        }
    }

    private void processOrders(){
        scanner = new Scanner(System.in);
        String action = "", instrument = "";
        int quantity = 0;
        double price = 0.0;
        while(true){
            try{
                while (true) {
                    System.out.println("Enter the action (Buy/Sell): ");
                    action = scanner.nextLine().trim();
                    if (action.equalsIgnoreCase("Buy") || action.equalsIgnoreCase("Sell")) {
                        break;
                    }
                    System.out.println("Invalid action. Please enter 'Buy' or 'Sell'.");
                }
    
                while (true) {
                    System.out.println("Enter the instrument: ");
                    instrument = scanner.nextLine().trim();
                    if (!instrument.isEmpty()) {
                        break;
                    }
                    System.out.println("Invalid instrument. Please enter a valid instrument.");
                }
    
                while (true) {
                    System.out.println("Enter the quantity: ");
                    try {
                        quantity = Integer.parseInt(scanner.nextLine().trim());
                        if (quantity > 0 && String.valueOf(quantity).length() <= 10) {
                            break;
                        }
                        System.out.println("Invalid quantity. Please enter a positive integer.");
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid input. Please enter a valid integer for quantity.");
                    }
                }
    
                while (true) {
                    System.out.println("Enter the price: ");
                    try {
                        price = Double.parseDouble(scanner.nextLine().trim());
                        if (price > 0 && String.valueOf(price).length() <= 10) {
                            break;
                        }
                        System.out.println("Invalid price. Please enter a positive number.");
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid input. Please enter a valid number for price.");
                    }
                }
                sendOrder(action, instrument, quantity, price);
            }catch(Exception e){
                System.err.println("An unexpected error occurred: " + e.getMessage());
            }
        }
    }
    
    private void  sendOrder(String action, String instrument, int quantity, double price){
        message = new StringBuilder();
        handler = new FIXMessageHandler(brokerId);
        handler.handle(message, action, instrument, quantity, price);
        try{
            ByteBuffer buffer = ByteBuffer.wrap(message.toString().getBytes(StandardCharsets.UTF_8));
            while (buffer.hasRemaining()) {
                socket.write(buffer);
            }
            System.out.println("Message sent: " + message);
        } catch (IOException e) {
            System.err.println("Broker failed to send the message");
            e.printStackTrace();
        }
    }

    private void listenForResponses(){
        ByteBuffer buffer = ByteBuffer.allocate(256);
        try{
            while(socket.read(buffer) != -1){
                buffer.flip();
                String response = StandardCharsets.UTF_8.decode(buffer).toString().trim();
                buffer.clear();
                if(!response.isEmpty()){
                    System.out.println("Message received: " + response);
                }
                //Thread.sleep(1000);
            }
        }catch(Exception e){
            System.err.println("Broker failed to receive the message");
            e.printStackTrace();
        }
    }

    public void close(){
        //scanner.close();
        try{
            socket.close();
            System.out.println("Broker " + brokerId + " disconnected from the server");
        }catch(IOException e){
            System.err.println("Broker failed to disconnect from the server");
            e.printStackTrace();
        }    
    }
}
