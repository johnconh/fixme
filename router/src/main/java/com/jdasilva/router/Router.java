package com.jdasilva.router;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class Router {

    private Map<Integer, Socket> brokers = new HashMap<Integer, Socket>();
    private Map<Integer, Socket> markets = new HashMap<Integer, Socket>();
    private ServerSocket brokerSocket;
    private ServerSocket marketSocket;
    private int brokerpot = 5000;
    private int marketport = 5001;
    private CountDownLatch latch = new CountDownLatch(1);

    public void start() {
        try{
            brokerSocket = new ServerSocket(brokerpot);
            marketSocket = new ServerSocket(marketport);
            System.out.println("Router started on port " + brokerpot + " and " + marketport);
            new Thread(() -> listenForConnections(brokerSocket, brokers, "Broker")).start();
            new Thread(() -> listenForConnections(marketSocket, markets, "Market")).start();
            latch.await();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.out.println("Router interrupted");
            close();
        }
    }

    private void listenForConnections(ServerSocket serverSocket, Map<Integer, Socket> connections, String type) {
        int idCounter = 100000;
        while (true) {
            try {
                if(serverSocket.isClosed()){
                    System.out.println(type + " id: " + idCounter + " socket closed.");
                    break;
                }
                Socket socket = serverSocket.accept();
                int idClient = idCounter++;
                connections.put(idClient, socket);
                new Thread(() -> handleClient(socket, idClient, type)).start();
            } catch (IOException e) {
                System.out.println("Error: " + type + " connection failed");
                e.printStackTrace();
            } 
        }
    }

    private void handleClient(Socket socket, int clientId, String type) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)) {
            writer.println(clientId);
            System.out.println(type + " id: " + clientId + " connected to the server");
            String message;
            while ((message = reader.readLine()) != null) {
                System.out.println("Message received from " + type + " id " + clientId + ": " + message);
                if(!validateCheckshum(message)){
                    System.out.println("Error: Invalid checksum");
                    continue;
                }
                Map<String, String> fields = parseFIXMessage(message);
                String destinationIdString = fields.get("56");
                Socket destination;
                if("MARKET".equals(destinationIdString)){
                    destination = findMarketSocket();
                } else {
                    int destinationId = Integer.parseInt(destinationIdString);
                    destination = getDestination(destinationId);
                }

                if(destination != null){
                    fowardMessage(message, destination);
                    System.out.println("Message fowarded to " + destination + ": " + message);
                } else {
                    System.out.println("Error: Destination not found");
                }
            }
        } catch (Exception e) {
            System.out.println(type + " id: " + clientId + " disconnected from the server");
            e.printStackTrace();
        }
    }

    private Map<String, String> parseFIXMessage(String message) {
        Map<String, String> fields = new HashMap<>();
        String[] parts = message.split("\\|");
        for (String part : parts) {
            String[] field = part.split("=");
            fields.put(field[0], field[1]);
        }
        return fields;
    }
    private boolean validateCheckshum(String message) {
        String[] parts = message.split("\\|");
        String checkshumFiled = parts[parts.length - 1];
        if(checkshumFiled.startsWith("10=")){
            int checkshum = Integer.parseInt(checkshumFiled.substring(3));
            return calculateCheckshum(message) == checkshum;
        }
        return false;
    }

    private int calculateCheckshum(String message) {
        int checksumIndex = message.indexOf("10=");
        String messageWithoutChecksum = (checksumIndex != -1) ? message.substring(0, checksumIndex) : message;
        int sum = 0;
        for (char c : messageWithoutChecksum.toCharArray()) {
            sum += c;
        }
        return sum % 256;
    }

    private Socket findMarketSocket() {
        return markets.values().stream().findAny().orElse(null);
    }
    
    private Socket getDestination(int destinationId) {
        if (brokers.containsKey(destinationId)) {
            return brokers.get(destinationId);
        } else if (markets.containsKey(destinationId)) {
            return markets.get(destinationId);
        }
        return null;
    }

    private void fowardMessage(String message, Socket destination) {
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(destination.getOutputStream()), true)) {
            writer.println(message);
            System.out.println("Message fowarded: " + message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void close() {
        brokers.values().forEach(socket -> {
            try {
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        markets.values().forEach(socket -> {
            try {
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        try {
            if (brokerSocket != null && !brokerSocket.isClosed()) {
                brokerSocket.close();
            }
            if (marketSocket != null && !marketSocket.isClosed()) {
                marketSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
