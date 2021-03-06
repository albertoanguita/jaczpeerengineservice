package jacz.peerengineservice.client.connection;

import org.aanguita.jacuzzi.io.serialization.StrCast;
import org.aanguita.jacuzzi.network.IP4Port;

import java.io.IOException;
import java.io.Serializable;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

/**
 * Stores the inet address info of one peer. This info includes local and external addresses. Each includes ip and port
 *
 * The class includes code for serializing and de-serializing into a human-readable string
 */
public class PeerAddress implements Serializable {

    private static final String SEPARATOR = "/";

    private static final String NULL = "NULL";

    private final IP4Port externalAddress;

    private final IP4Port localAddress;

    public PeerAddress(IP4Port externalAddress, IP4Port localAddress) {
        this.externalAddress = externalAddress;
        this.localAddress = localAddress;
    }

    public PeerAddress(String externalAddress, String localAddress) {
        this.externalAddress = new IP4Port(externalAddress);
        this.localAddress = new IP4Port(localAddress);
    }

    public PeerAddress(String serialization) throws IOException {
        StringTokenizer strTok = new StringTokenizer(serialization, SEPARATOR);
        externalAddress = deserializeAddress(strTok);
        localAddress = deserializeAddress(strTok);
    }

    public static PeerAddress nullPeerAddress() {
        IP4Port nullAddress = null;
        //noinspection ConstantConditions
        return new PeerAddress(nullAddress, nullAddress);
    }

    private IP4Port deserializeAddress(StringTokenizer strTok) throws IOException {
        try {
            String ip = strTok.nextToken();
            if (ip.equals(NULL)) {
                return null;
            } else {
                int port = StrCast.asInteger(strTok.nextToken());
                return new IP4Port(ip, port);
            }
        } catch (NoSuchElementException e) {
            throw new IOException();
        }
    }

    public String serialize() {
        StringBuilder ser = new StringBuilder();
        serializeAddress(ser, externalAddress);
        ser.append(SEPARATOR);
        serializeAddress(ser, localAddress);
        return ser.toString();
    }

    private void serializeAddress(StringBuilder ser, IP4Port address) {
        if (address != null) {
            ser.append(address.getIp()).append(SEPARATOR).append(address.getPort());
        } else {
            ser.append(NULL);
        }
    }

    public boolean isNull() {
        return externalAddress == null && localAddress == null;
    }

    public IP4Port getExternalAddress() {
        return externalAddress;
    }

    public IP4Port getLocalAddress() {
        return localAddress;
    }

    @Override
    public String toString() {
        return "PeerAddress{" +
                "externalAddress=" + externalAddress +
                ", localAddress=" + localAddress +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PeerAddress that = (PeerAddress) o;
        return !(externalAddress != null ? !externalAddress.equals(that.externalAddress) : that.externalAddress != null) && !(localAddress != null ? !localAddress.equals(that.localAddress) : that.localAddress != null);
    }

    @Override
    public int hashCode() {
        int result = externalAddress != null ? externalAddress.hashCode() : 0;
        result = 31 * result + (localAddress != null ? localAddress.hashCode() : 0);
        return result;
    }
}
