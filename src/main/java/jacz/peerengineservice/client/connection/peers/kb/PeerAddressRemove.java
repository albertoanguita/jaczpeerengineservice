package jacz.peerengineservice.client.connection.peers.kb;

import jacz.util.io.serialization.StrCast;
import jacz.util.network.IP4Port;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

/**
 * Stores the internet address(es) of a peer
 * todo
 */
public class PeerAddressRemove {

    private static final String SEPARATOR = "/";

    private static final String NULL = "NULL";

    private final IP4Port externalAddress;

    private final IP4Port localAddress;

    public PeerAddressRemove(IP4Port externalAddress, IP4Port localAddress) {
        this.externalAddress = externalAddress;
        this.localAddress = localAddress;
    }

    public PeerAddressRemove(String serialization) throws IOException {
        StringTokenizer strTok = new StringTokenizer(serialization, SEPARATOR);
        externalAddress = deserializeAddress(strTok);
        localAddress = deserializeAddress(strTok);
    }

    public PeerAddressRemove() throws IOException {
        externalAddress = null;
        localAddress = null;
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

    public IP4Port getExternalAddress() {
        return externalAddress;
    }

    public IP4Port getLocalAddress() {
        return localAddress;
    }
}
