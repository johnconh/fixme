package com.jdasilva;

import com.jdasilva.router.Router;

public class App 
{
    public static void main( String[] args )
    {
        Router router = new Router();
        router.start();

        Runtime.getRuntime().addShutdownHook(new Thread(router::close));
    }
}
