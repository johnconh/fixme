package com.jdasilva.router;
import java.nio.channels.SocketChannel;

public class ChecksumHandler  extends BaseHandler {
    @Override
    public void handle(String message, SocketChannel socket, int clientId, String type) throws Exception {
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
        String messageWithoutHeader = message.startsWith("8=FIX.4.2|") ? message.substring(10) : message;
        int checksumIndex = messageWithoutHeader.indexOf("10=");
        String messageWithoutChecksum = (checksumIndex != -1) ? messageWithoutHeader.substring(0, checksumIndex) : messageWithoutHeader;
        int sum = 0;
        for (char c : messageWithoutChecksum.toCharArray()) {
            sum += c;
        }
        return sum % 256;
    }
}
