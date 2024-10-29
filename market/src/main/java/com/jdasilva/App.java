package com.jdasilva;

import com.jdasilva.market.Market;

public class App 
{
    public static void main( String[] args )
    {
        Market market = new Market();
        market.start();

        Runtime.getRuntime().addShutdownHook(new Thread(market::close));
    }
}
