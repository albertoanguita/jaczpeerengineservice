package jacz.peerengineservice.test;

import jacz.peerengineservice.server.PeerServer;
import jacz.peerengineservice.server.PeerServerConfig;

/**
 * Created by IntelliJ IDEA.
 * User: Alberto
 * Date: 13-may-2010
 * Time: 11:54:35
 * To change this template use File | Settings | File Templates.
 */
public class Server {

    private PeerServer peerServer;

    public Server() {
        peerServer = new PeerServer(new PeerServerConfig(50000, 1000), new PeerServerActionImpl());
    }

    public void startServer() {
        peerServer.startServer();
    }
}
