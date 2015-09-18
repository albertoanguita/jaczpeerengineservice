package jacz.peerengineservice.client;

import jacz.util.network.IP4Port;

/**
 * Data about a peer server. This includes connection data (ip, port) and last known metadata (server name, server capacity, status)
 */
public class PeerServerData {

    private IP4Port ip4Port;

    private String name;

    private int capacity;

    public PeerServerData(IP4Port ip4Port) {
        this.ip4Port = ip4Port;
    }

    public PeerServerData(PeerServerData peerServerData) {
        this.ip4Port = peerServerData.ip4Port;
        this.name = peerServerData.name;
        this.capacity = peerServerData.capacity;
        this.currentUsers = peerServerData.currentUsers;
    }

    /**
     * -1 indicates unknown
     */
    private int currentUsers;

    public IP4Port getIp4Port() {
        return ip4Port;
    }

    public void setIp4Port(IP4Port ip4Port) {
        this.ip4Port = ip4Port;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getCurrentUsers() {
        return currentUsers;
    }

    public void setCurrentUsers(int currentUsers) {
        this.currentUsers = currentUsers;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof PeerServerData) && ((PeerServerData) obj).ip4Port.equals(ip4Port);
    }

    @Override
    public String toString() {
        return "PeerServerData{" +
                "ip4Port=" + ip4Port +
                '}';
    }
}
