package jacz.peerengineservice.server;

import jacz.util.identifier.UniqueIdentifier;
import jacz.util.network.IP4Port;
import jacz.commengine.channel.ChannelConnectionPoint;
import jacz.peerengineservice.PeerID;

/**
 * Data about one connected client. These must be gathered when the client connects to the server, with a specific connection protocol
 */
public class ConnectedClientData {

    private UniqueIdentifier clientID;

    private PeerID peerID;

    private ChannelConnectionPoint ccp;

    private IP4Port ip4Port;

    /**
     * Local ip of the client (if equal to the public ip, this value is null)
     */
    private String localIP;

    private String version;

    public ConnectedClientData(UniqueIdentifier clientID, PeerID peerID, ChannelConnectionPoint ccp, IP4Port ip4Port, String localIP) {
        this.clientID = clientID;
        this.peerID = peerID;
        this.ccp = ccp;
        this.ip4Port = ip4Port;
        if (!localIP.equals(ip4Port.getIp())) {
            this.localIP = localIP;
        } else {
            this.localIP = null;
        }
        version = null;
    }

    public UniqueIdentifier getClientID() {
        return clientID;
    }

    public PeerID getPeerID() {
        return peerID;
    }

    public ChannelConnectionPoint getCcp() {
        return ccp;
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

    public PeerConnectionInfo getPeerConnectionInfo() {
        return new PeerConnectionInfo(peerID, ip4Port, localIP, version);
    }

    public String veryShortDescription() {
        return "ID: " + clientID.toString();
    }

    public String shortDescription() {
        return veryShortDescription() + ", " + peerID;
    }

    public String longDescription() {
        return shortDescription() + " (address: " + ip4Port + ", local ip: " + localIP + ")";
    }
}
