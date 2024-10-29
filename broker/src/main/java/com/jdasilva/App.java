package com.jdasilva;

import com.jdasilva.broker.Broker;

public class App 
{
    public static void main( String[] args )
    {
        Broker broker = new Broker();
        broker.start();

        Runtime.getRuntime().addShutdownHook(new Thread(broker::close));
    }
}
