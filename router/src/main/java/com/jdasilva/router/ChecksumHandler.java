package com.jdasilva.router;
import java.net.Socket;

public class ChecksumHandler  extends BaseHandler {
    @Override
    public void handle(String message, Socket socket, int clientId, String type) {
        if(!validateCheckshum(message)){
            System.out.println("Error: Invalid checksum");
            return;
        }
        super.handle(message, socket, clientId, type);
    }

    private boolean validateCheckshum(String message){
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
}
