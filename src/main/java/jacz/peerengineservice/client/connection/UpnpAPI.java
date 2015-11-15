package jacz.peerengineservice.client.connection;

import org.bitlet.weupnp.GatewayDevice;
import org.bitlet.weupnp.GatewayDiscover;
import org.bitlet.weupnp.PortMappingEntry;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;

/**
 * Mediation API for accesing the UPnP service with weupnp
 */
public class UpnpAPI {

    public static class UpnpException extends Exception {

        public UpnpException(String message) {
            super(message);
        }
    }

    public static class NoGatewayException extends Exception {
    }

    public static class PortMapping {

        public final int internalPort;
        public final int externalPort;
        public final String internalClient;
        public final String protocol;
        public final String description;

        public PortMapping(int internalPort, int externalPort, String internalClient, String protocol, String description) {
            this.internalPort = internalPort;
            this.externalPort = externalPort;
            this.internalClient = internalClient;
            this.protocol = protocol;
            this.description = description;
        }

        @Override
        public String toString() {
            return "PortMapping{" +
                    "internalPort=" + internalPort +
                    ", externalPort=" + externalPort +
                    ", internalClient='" + internalClient + '\'' +
                    ", protocol='" + protocol + '\'' +
                    ", description='" + description + '\'' +
                    '}';
        }
    }


    private static final String PROTOCOL = "TCP";

    private static final int INITIAL_PORT = 1024;

    private static final int FINAL_PORT = 49151;

    public static GatewayDevice fetchGatewayDevice(String externalIPAddress) throws UpnpException, NoGatewayException {

        try {
            GatewayDiscover gatewayDiscover = new GatewayDiscover();
            Map<InetAddress, GatewayDevice> gateways = gatewayDiscover.discover();

            if (gateways.isEmpty()) {
                throw new NoGatewayException();
            } else {
                GatewayDevice activeGW = null;
                for (GatewayDevice gw : gateways.values()) {
                    if (gw.getExternalIPAddress().equals(externalIPAddress)) {
                        activeGW = gw;
                        break;
                    }
                }
                if (activeGW != null) {
                    return activeGW;
                } else {
                    throw new NoGatewayException();
                }
            }
        } catch (IOException | SAXException | ParserConfigurationException e) {
            throw new UpnpException(e.getMessage());
        }
    }

    /**
     * @param externalPort the port to fetch
     * @return true if port is free, false otherwise
     * @throws UpnpException
     * @throws NoGatewayException
     */
    public static PortMapping fetchPort(GatewayDevice activeGW, int externalPort) throws UpnpException, NoGatewayException {
        try {
            PortMappingEntry portMapping = new PortMappingEntry();
            boolean existsPort = activeGW.getSpecificPortMappingEntry(externalPort, PROTOCOL, portMapping);
            if (existsPort) {
                return new PortMapping(
                        portMapping.getInternalPort(),
                        portMapping.getExternalPort(),
                        portMapping.getInternalClient(),
                        portMapping.getProtocol(),
                        portMapping.getPortMappingDescription());
            } else {
                return null;
            }
        } catch (IOException | SAXException e) {
            throw new UpnpException(e.getMessage());
        }
    }

    public static void mapPort(GatewayDevice activeGW, String mappingDescription, int externalPort, int internalPort, boolean replace) throws UpnpException, NoGatewayException {
        PortMapping portMapping = fetchPort(activeGW, externalPort);
        if (portMapping != null) {
            if (replace && portMapping.description.equals(mappingDescription)) {
                unmapPort(activeGW, externalPort);
            } else {
                throw new UpnpException("port already mapped");
            }
        }
        generatePortMapping(activeGW, mappingDescription, externalPort, internalPort);
    }

    private static void generatePortMapping(GatewayDevice activeGW, String mappingDescription, int externalPort, int internalPort) throws UpnpException, NoGatewayException {
        try {
            InetAddress localAddress = activeGW.getLocalAddress();
            activeGW.addPortMapping(externalPort, internalPort, localAddress.getHostAddress(), PROTOCOL, mappingDescription);
        } catch (IOException | SAXException e) {
            throw new UpnpException(e.getMessage());
        }
    }

    public static int mapPortFrom(GatewayDevice activeGW, String mappingDescription, int initialExternalPort, int internalPort, boolean replace) throws UpnpException, NoGatewayException {
        // search for a free port
//        PortMapping portMapping = fetchPort(activeGW, externalPort);
//        while (externalPort <= FINAL_PORT && portMapping != null) {
        // search a suitable external port
        int externalPort = initialExternalPort;
        boolean finished = false;
        while (!finished) {
            PortMapping portMapping = fetchPort(activeGW, externalPort);
            if (portMapping != null && replace && portMapping.description.equals(mappingDescription)) {
                unmapPort(activeGW, externalPort);
                portMapping = null;
            }
            if (portMapping == null) {
                finished = true;
            } else {
                externalPort++;
                if (externalPort == initialExternalPort) {
                    externalPort = -1;
                    finished = true;
                }
                if (externalPort > FINAL_PORT) {
                    externalPort = INITIAL_PORT;
                }
            }
//            if (replace && portMapping.description.equals(mappingDescription)) {
//                unmapPort(activeGW, externalPort);
//                break;
//            } else {
//                externalPort++;
//                portMapping = fetchPort(activeGW, externalPort);
//            }
        }
        if (externalPort != -1) {
            generatePortMapping(activeGW, mappingDescription, externalPort, internalPort);
        }
        return externalPort;
    }

    public static void unmapPort(GatewayDevice activeGW, int externalPort) throws UpnpException, NoGatewayException {

        try {
            activeGW.deletePortMapping(externalPort, PROTOCOL);
        } catch (IOException | SAXException e) {
            throw new UpnpException(e.getMessage());
        }
    }


    public static void main(String[] args) {
        try {
            GatewayDevice activeGW = fetchGatewayDevice("77.231.224.163");
//        System.out.println(fetchPort(activeGW, 80));

//            int port = mapPortFrom(activeGW, "test2", 85, 70, true);
//            System.out.println(port);
//            ThreadUtil.safeSleep(5000);
            unmapPort(activeGW, 85);
            unmapPort(activeGW, 86);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
