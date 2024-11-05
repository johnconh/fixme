package com.jdasilva.router;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import com.jdasilva.database.databaseManager;
import java.util.Random;

public class Router {

    private Handler handler;
    private Map<Integer, Socket> brokers = new HashMap<Integer, Socket>();
    private Map<Integer, Socket> markets = new HashMap<Integer, Socket>();
    private ServerSocket brokerSocket;
    private ServerSocket marketSocket;
    private int brokerpot = 5000;
    private int marketport = 5001;
    private CountDownLatch latch = new CountDownLatch(1);
    private Random random = new Random();

    public Router()
    {
        new databaseManager();
        Handler checksumHandler = new ChecksumHandler();
        Handler fixMessageHandler = new FIXMessageHandler(this);

        checksumHandler.next(fixMessageHandler);
        this.handler = checksumHandler;
    }

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
        } finally {
            close();
        }
    }

    private void listenForConnections(ServerSocket serverSocket, Map<Integer, Socket> connections, String type) {
    
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                int idClient = 100000 + random.nextInt(900000);
                connections.put(idClient, socket);
                System.out.println(type + " id: " + idClient + " connected to the server");
                new Thread(() -> handleClient(socket, idClient, type)).start();
            } catch (IOException e) {
                System.out.println("Error: " + type + " connection failed");
                e.printStackTrace();
            } 
        }
    }

    private void handleClient(Socket socket, int clientId, String type) {
        try { 
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            writer.println(clientId);
            String message;
            while ((message = reader.readLine()) != null) {
                saveTransaction(clientId, type, message);
                handler.handle(message, socket, clientId, type);
            }
        } catch (Exception e) {
            System.out.println(type + " id: " + clientId + " disconnected from the server");
            e.printStackTrace();
        }
    }

    private void saveTransaction(int clientId, String type, String message) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        
        try {
            connection = DriverManager.getConnection(databaseManager.getURL() + databaseManager.getDB_NAME(), databaseManager.getUSER(), databaseManager.getPASSWORD());
            String query = "INSERT INTO transactions (client_id, type, message) VALUES (?, ?, ?)";
            statement = connection.prepareStatement(query); 
            statement.setInt(1, clientId);
            statement.setString(2, type);
            statement.setString(3, message);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (resultSet != null)
                    resultSet.close();
                if (statement != null)
                    statement.close();
                if (connection != null)
                    connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
      
    }

    public Map<Integer, Socket> getBrokers() {
        return brokers;
    }

    public Map<Integer, Socket> getMarkets() {
        return markets;
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
