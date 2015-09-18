package jacz.peerengineservice.util;

/**
 * Constant definitions for serveral channels used in the connections with the server and other peers
 */
public class ChannelConstants {

    /**
     * The RequestDispatcher works in a fixed channel
     */
    public static final byte REQUEST_DISPATCHER_CHANNEL = 0;

    /**
     * Working channel for the data streaming manager package (channel 0 is reserved for the request processor)
     */
    public static final byte RESOURCE_STREAMING_MANAGER_CHANNEL = 1;

    /**
     * Channel for establishing connection with other peers
     */
    public static final byte CONNECTION_ESTABLISHMENT_CHANNEL = 2;



    /***************************************************************/
    // COMMUNICATION WITH THE PEER SERVER

    /**
     * Channel employed to receive the response from PeerServers (only used when we connect to the PeerServer)
     */
    public static final byte PEER_SERVER_CONNECTION_CHANNEL = 0;

    /**
     * Channel for accepting requests from the peer server
     */
    public static final byte REQUESTS_CHANNEL = 1;



}
