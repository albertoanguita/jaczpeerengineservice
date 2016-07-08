package jacz.peerengineservice.test;

import org.aanguita.jacuzzi.concurrency.ThreadUtil;

/**
 * Created by Alberto on 13/10/2015.
 */
public class ShutdownHook extends Thread {

    @Override
    public void run() {
        System.out.println("hook end");
        System.runFinalization();
        ThreadUtil.safeSleep(1000);
    }
}
