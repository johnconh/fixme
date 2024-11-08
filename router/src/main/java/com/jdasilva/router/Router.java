package com.jdasilva.router;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.jdasilva.database.databaseManager;
import java.util.Random;
import java.util.List;

public class Router {

    private Handler handler;
    private Map<Integer, SocketChannel> brokers = new HashMap<>();
    private Map<Integer, SocketChannel> markets = new HashMap<>();
    private ServerSocketChannel brokerSocket;
    private ServerSocketChannel marketSocket;
    private int brokerpot = 5000;
    private int marketport = 5001;
    private CountDownLatch latch = new CountDownLatch(1);
    private Random random = new Random();
    private ExecutorService executorService = Executors.newFixedThreadPool(10);

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
            brokerSocket = ServerSocketChannel.open();
            brokerSocket.bind(new InetSocketAddress(brokerpot));
            marketSocket = ServerSocketChannel.open();
            marketSocket.bind(new InetSocketAddress(marketport));
            System.out.println("Router started on port " + brokerpot + " and " + marketport);
            executorService.submit(() -> listenForConnections(brokerSocket, brokers, "Broker"));
            executorService.submit(() -> listenForConnections(marketSocket, markets, "Market"));
            latch.await();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.out.println("Router interrupted");
        } finally {
            executorService.shutdown();
            close();
        }
    }

    private void listenForConnections(ServerSocketChannel serverSocket, Map<Integer, SocketChannel> connections, String type) {
    
        while (true) {
            try {
                SocketChannel socket = serverSocket.accept();
                socket.configureBlocking(false);
                int idClient = 100000 + random.nextInt(900000);
                connections.put(idClient, socket);
                System.out.println(type + " id: " + idClient + " connected to the server");
                ByteBuffer buffer = ByteBuffer.allocate(256);
                buffer.put(String.valueOf(idClient).getBytes(StandardCharsets.UTF_8));
                buffer.flip();
                socket.write(buffer);
                executorService.submit(() -> retryFailedTransactions(socket, idClient, type));
                executorService.submit(() -> handleClient(socket, idClient, type));
            } catch (Exception e) {
                System.out.println("Error: " + type + " connection failed");
                e.printStackTrace();
            }
        }
    }

    private void handleClient(SocketChannel socket, int clientId, String type) {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(256);
            while (true) {
                if (!socket.isOpen()) {
                    break;
                }

                int read = socket.read(buffer);
                if (read == -1) {
                    System.out.println(type + " id: " + clientId + " disconnected from the server");
                    if("Broker".equals(type)){
                        brokers.remove(clientId);
                    } else {
                        markets.remove(clientId);
                    }
                    break;
                }

                if(read == 0) {
                    continue;
                }

                buffer.flip();
                String message = StandardCharsets.UTF_8.decode(buffer).toString();
                buffer.clear();

                if (message.isEmpty()) {
                    continue;
                }

                int  transactionID = saveTransaction(clientId, type, message, "IN_PROGRESS");
                
                try{
                    handler.handle(message, socket, clientId, type);
                    updateTransaction(transactionID, "COMPLETED");
                } catch (Exception e) {
                    System.out.println("Error: " + type + " id: " + clientId + " could not be processed");
                    updateTransaction(transactionID, "FAILED");
                }
            }
        } catch (IOException e) {
            System.out.println(type + " id: " + clientId + " disconnected from the server");
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private int saveTransaction(int clientId, String type, String message, String status) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        
        try {
            connection = DriverManager.getConnection(databaseManager.getURL() + databaseManager.getDB_NAME(), databaseManager.getUSER(), databaseManager.getPASSWORD());
            String query = "INSERT INTO transactions (client_id, type, message, status) VALUES (?, ?, ?, ?)";
            statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS); 
            statement.setInt(1, clientId);
            statement.setString(2, type);
            statement.setString(3, message);
            statement.setString(4, status);
            statement.executeUpdate();

            resultSet = statement.getGeneratedKeys();
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
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
      return -1;
    }

    public void updateTransaction (int transactionID, String status) {
        Connection connection = null;
        PreparedStatement statement = null;
        
        try {
            connection = DriverManager.getConnection(databaseManager.getURL() + databaseManager.getDB_NAME(), databaseManager.getUSER(), databaseManager.getPASSWORD());
            String query = "UPDATE transactions SET status = ? WHERE id = ?";
            statement = connection.prepareStatement(query);
            statement.setString(1, status);
            statement.setInt(2, transactionID);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (statement != null)
                    statement.close();
                if (connection != null)
                    connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void retryFailedTransactions(SocketChannel socket, int idClient, String type) {
        List <Map<String, Object>> faliedTransactions = databaseManager.getFailedTransactions(type);
        if (faliedTransactions.isEmpty()) {return;}
        System.out.println("Retrying failed transactions for " + type);
        for(Map<String, Object> transaction : faliedTransactions){
            int clientId = (int) transaction.get("client_id");
            String message = (String) transaction.get("message");
            try{
                handler.handle(message, socket, clientId, type);
                updateTransaction((int) transaction.get("id"), "COMPLETED");
            }catch (Exception e) {
                System.out.println("Error: " + type + " id: " + clientId + " could not be processed");
                updateTransaction((int) transaction.get("id"), "FAILED");
            }
        }
    }

    public Map<Integer, SocketChannel> getBrokers() {
        return brokers;
    }

    public Map<Integer, SocketChannel> getMarkets() {
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
            if (brokerSocket != null && !brokerSocket.isOpen()) {
                brokerSocket.close();
            }
            if (marketSocket != null && !marketSocket.isOpen()) {
                marketSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
