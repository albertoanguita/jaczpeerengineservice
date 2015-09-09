package jacz.peerengine.server;

import jacz.util.network.IP4Port;
import jacz.peerengine.PeerID;

import java.io.Serializable;

/**
 * This class stores network data about a peer specifically needed by other peers to connect to it. This is the info of each peer that the server
 * returns when it receives a friend query
 */
public class PeerConnectionInfo implements Serializable {

    /**
     * PeerID of this peer
     */
    private PeerID peerID;

    /**
     * Public IP and port information
     */
    private IP4Port ip4Port;

    /**
     * IP of the peer in its local network
     */
    private String localIP;

    /**
     * Version of the client software used by this peer (this way other peers can discard incompatible versions)
     */
    private String version;

    public PeerConnectionInfo(PeerID peerID, IP4Port ip4Port, String localIP, String version) {
        this.peerID = peerID;
        this.ip4Port = ip4Port;
        this.localIP = localIP;
        this.version = version;
    }

    public PeerID getPeerID() {
        return peerID;
    }

    public IP4Port getIp4Port() {
        return ip4Port;
    }

    public String getLocalIP() {
        return localIP;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "PeerConnectionInfo (" + peerID + "): " + ip4Port + ", " + localIP;
    }
}
