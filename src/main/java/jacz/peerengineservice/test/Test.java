package jacz.peerengineservice.test;

import jacz.util.concurrency.ThreadUtil;

/**
 * Created by Alberto on 13/10/2015.
 */
public class Test {

    private void run() {
        System.out.println("start");

        System.out.println("end");
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        System.out.println("finalize");
    }

    private static void staticRun() {
        System.out.println("main start");
        Test test = new Test();
        test.run();
        System.out.println("main end");
    }

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());
//                System.runFinalizersOnExit(true);
        staticRun();
        staticRun();
        Test test2 = new Test();
        System.runFinalization();
        System.gc();
        ThreadUtil.safeSleep(5000);
        throw new RuntimeException();

    }


}
