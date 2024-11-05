package com.jdasilva.broker;

import java.text.SimpleDateFormat;
import java.util.Date;

public class FIXMessageHandler extends BaseHandler {

    private int brokerId;

    public FIXMessageHandler(int brokerId) {
        this.brokerId = brokerId;
    }

    @Override
    public void handle(StringBuilder message, String action, String instrument, int quantity, double price) {
        message.append("8=FIX.4.2|");
        message.append("35=").append(action.equals("Buy") ? "D" : "F").append("|");
        message.append("49=").append(brokerId).append("|");
        message.append("56=MARKET|");
        message.append("55=").append(instrument).append("|");
        message.append("38=").append(quantity).append("|");
        message.append("44=").append(price).append("|");
        String timestamp = new SimpleDateFormat("yyyyMMdd-HH:mm:ss.SSS").format(new Date());
        message.append("52=").append(timestamp).append("|");
        String checksum = calculateChecksum(message.toString());
        message.append("10=").append(checksum).append("|");
        super.handle(message, action, instrument, quantity, price);
    }
    
    private String calculateChecksum(String message){
        int startIndex = message.indexOf("|") + 1;
        String messageToCheck = message.substring(startIndex);
        int sum = 0;
        for(char c : messageToCheck.toCharArray()){
            sum += c;
        }
        return String.valueOf(sum % 256);
    }
}
