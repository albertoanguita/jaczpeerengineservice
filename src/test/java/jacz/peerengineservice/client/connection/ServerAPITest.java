package jacz.peerengineservice.client.connection;


import com.neovisionaries.i18n.CountryCode;
import jacz.peerengineservice.PeerId;
import org.junit.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Server tests
 */
public class ServerAPITest {

    private class PeerInfo {

        private final PeerId peerId;

        private final String localIPAddress;

        private final int localMainServerPort;

        private final int externalMainServerPort;

        private CountryCode mainCountry;

        private boolean wishRegularConnections;

        private String sessionId;

        public PeerInfo(String peerId, CountryCode mainCountry, boolean wishRegularConnections) {
            this.peerId = PeerId.buildTestPeerId(peerId);
            this.localIPAddress = "192.168.1." + peerId;
            this.localMainServerPort = Integer.parseInt(peerId);
            this.externalMainServerPort = Integer.parseInt(peerId) + 1000;
            this.mainCountry = mainCountry;
            this.wishRegularConnections = wishRegularConnections;
            this.sessionId = null;
        }
    }

    private static final String serverURL = "https://jaczserver.appspot.com/_ah/api/server/v1/";

    @org.junit.Test
    public void testRegistering() throws IOException, ServerAccessException {

        // first, create a batch of peers to run the tests. The server must NOT have these peers registered
        // also, authentication must be disabled in server, since we don't have the public keys for these peer ids
        // the peers are stored in a list, and have an associated config
        List<PeerInfo> peers = buildPeers();

        System.out.println("Connecting peers...");
        for (PeerInfo peerInfo : peers) {
            ServerAPI.ConnectionResponse connectionResponse = ServerAPI.connect(
                    serverURL,
                    new ServerAPI.ConnectionRequest(
                            peerInfo.peerId,
                            peerInfo.localIPAddress,
                            peerInfo.localMainServerPort,
                            peerInfo.externalMainServerPort,
                            peerInfo.mainCountry,
                            peerInfo.wishRegularConnections
                    )
            );
            Assert.assertEquals(ServerAPI.ConnectionResponseType.UNREGISTERED_PEER, connectionResponse.getResponse());
            System.out.println("Peer " + peerInfo.peerId.toString() + " is not registered!");

            System.out.println("Registering peer...");
            ServerAPI.RegistrationResponse registrationResponse = ServerAPI.register(serverURL, new ServerAPI.RegistrationRequest(peerInfo.peerId));
            Assert.assertEquals(ServerAPI.RegistrationResponse.OK, registrationResponse);
            System.out.println("Peer " + peerInfo.peerId.toString() + " is registered!");

            System.out.println("Trying to connect again...");
            connectionResponse = ServerAPI.connect(
                    serverURL,
                    new ServerAPI.ConnectionRequest(
                            peerInfo.peerId,
                            peerInfo.localIPAddress,
                            peerInfo.localMainServerPort,
                            peerInfo.externalMainServerPort,
                            peerInfo.mainCountry,
                            peerInfo.wishRegularConnections
                    )
            );
            Assert.assertEquals(ServerAPI.ConnectionResponseType.OK, connectionResponse.getResponse());
            System.out.println("Peer " + peerInfo.peerId.toString() + " is connected!");
            System.out.println("Session id is: " + connectionResponse.getSessionID());

            System.out.println("Disconnecting peer...");
            ServerAPI.DisconnectResponse disconnectResponse = ServerAPI.disconnect(serverURL, new ServerAPI.UpdateRequest(connectionResponse.getSessionID()));
            Assert.assertEquals(ServerAPI.DisconnectResponse.OK, disconnectResponse);
            System.out.println("Peer disconnected!");
        }
    }

    @org.junit.Test
    public void testServer() throws IOException, ServerAccessException {

        // first, create a batch of peers to run the tests. The server must have these peers registered
        // the peers are stored in a list, and have an associated config
        List<PeerInfo> peers = buildPeers();

        System.out.println("Connecting peers...");
        for (PeerInfo peerInfo : peers) {
            ServerAPI.ConnectionResponse connectionResponse = ServerAPI.connect(
                    serverURL,
                    new ServerAPI.ConnectionRequest(
                            peerInfo.peerId,
                            peerInfo.localIPAddress,
                            peerInfo.localMainServerPort,
                            peerInfo.externalMainServerPort,
                            peerInfo.mainCountry,
                            peerInfo.wishRegularConnections
                    )
            );
            Assert.assertEquals(ServerAPI.ConnectionResponseType.OK, connectionResponse.getResponse());
            System.out.println("Peer " + peerInfo.peerId.toString() + " is connected!");
            System.out.println("Session id is: " + connectionResponse.getSessionID());
            peerInfo.sessionId = connectionResponse.getSessionID();
        }

        // ask for specific peers
        System.out.println("Asking for info about peers 90, 91 and 92...");
        List<PeerId> peerIds = new ArrayList<>();
        peerIds.add(peers.get(0).peerId);
        peerIds.add(peers.get(2).peerId);
        peerIds.add(peers.get(6).peerId);
        ServerAPI.InfoResponse infoResponse = ServerAPI.info(serverURL, new ServerAPI.InfoRequest(peerIds));
        System.out.println("Info received: " + infoResponse);
        Assert.assertEquals(3, infoResponse.getPeerIdInfoList().size());
        for (ServerAPI.PeerIdInfo peerIdInfo : infoResponse.getPeerIdInfoList()) {
            if (peerIdInfo.getPeerId().equals(peers.get(0).peerId)) {
                Assert.assertEquals("192.168.1.90", peerIdInfo.getLocalIPAddress());
                Assert.assertEquals(90, peerIdInfo.getLocalMainServerPort());
                Assert.assertEquals(1090, peerIdInfo.getExternalMainServerPort());
                Assert.assertEquals(-1, peerIdInfo.getLocalRESTServerPort());
                Assert.assertEquals(-1, peerIdInfo.getExternalRESTServerPort());
                Assert.assertEquals(CountryCode.ES, peerIdInfo.getMainCountry());
                Assert.assertEquals(true, peerIdInfo.isWishRegularConnections());
            } else if (peerIdInfo.getPeerId().equals(peers.get(2).peerId)) {
                Assert.assertEquals("192.168.1.92", peerIdInfo.getLocalIPAddress());
                Assert.assertEquals(92, peerIdInfo.getLocalMainServerPort());
                Assert.assertEquals(1092, peerIdInfo.getExternalMainServerPort());
                Assert.assertEquals(-1, peerIdInfo.getLocalRESTServerPort());
                Assert.assertEquals(-1, peerIdInfo.getExternalRESTServerPort());
                Assert.assertEquals(CountryCode.AR, peerIdInfo.getMainCountry());
                Assert.assertEquals(true, peerIdInfo.isWishRegularConnections());
            } else if (peerIdInfo.getPeerId().equals(peers.get(6).peerId)) {
                Assert.assertEquals("192.168.1.96", peerIdInfo.getLocalIPAddress());
                Assert.assertEquals(96, peerIdInfo.getLocalMainServerPort());
                Assert.assertEquals(1096, peerIdInfo.getExternalMainServerPort());
                Assert.assertEquals(-1, peerIdInfo.getLocalRESTServerPort());
                Assert.assertEquals(-1, peerIdInfo.getExternalRESTServerPort());
                Assert.assertEquals(CountryCode.US, peerIdInfo.getMainCountry());
                Assert.assertEquals(false, peerIdInfo.isWishRegularConnections());
            }
        }


        System.out.println("Asking for peers with country 'ES'...");
        infoResponse = ServerAPI.regularPeersRequest(serverURL, new ServerAPI.RegularPeersRequest(CountryCode.ES));
        System.out.println("Info received: " + infoResponse);
        Assert.assertEquals(2, infoResponse.getPeerIdInfoList().size());
        for (ServerAPI.PeerIdInfo peerIdInfo : infoResponse.getPeerIdInfoList()) {
            PeerId peerId = peerIdInfo.getPeerId();
            if (!peerId.equals(peers.get(0).peerId) && !peerId.equals(peers.get(1).peerId)) {
                Assert.assertFalse(true);
            }
        }

//        System.out.println("Asking for peers with language 'es'...");
//        infoResponse = ServerAPI.regularPeersRequest(serverURL, new ServerAPI.RegularPeersRequest(LanguageCode.es));
//        System.out.println("Info received: " + infoResponse);
//        Assert.assertEquals(4, infoResponse.getPeerIdInfoList().size());
//        for (ServerAPI.PeerIdInfo peerIdInfo : infoResponse.getPeerIdInfoList()) {
//            PeerId peerId = peerIdInfo.getPeerId();
//            if (!peerId.equals(peers.get(0).peerId) && !peerId.equals(peers.get(1).peerId) && !peerId.equals(peers.get(2).peerId) && !peerId.equals(peers.get(3).peerId)) {
//                Assert.assertFalse(true);
//            }
//        }


        System.out.println("Disconnecting peers...");
        for (PeerInfo peerInfo : peers) {
            ServerAPI.DisconnectResponse disconnectResponse = ServerAPI.disconnect(serverURL, new ServerAPI.UpdateRequest(peerInfo.sessionId));
            Assert.assertEquals(ServerAPI.DisconnectResponse.OK, disconnectResponse);
            System.out.println("Peer " + peerInfo.peerId.toString() + "  disconnected!");
        }


        Assert.assertTrue(true);
    }

    private List<PeerInfo> buildPeers() {
        List<PeerInfo> peers = new ArrayList<>();
        peers.add(new PeerInfo("90", CountryCode.ES, true));
        peers.add(new PeerInfo("91", CountryCode.ES, true));
        peers.add(new PeerInfo("92", CountryCode.AR, true));
        peers.add(new PeerInfo("93", CountryCode.ME, true));
        peers.add(new PeerInfo("94", CountryCode.US, true));
        peers.add(new PeerInfo("95", CountryCode.US, true));
        peers.add(new PeerInfo("96", CountryCode.US, false));
        peers.add(new PeerInfo("97", CountryCode.UK, true));
        peers.add(new PeerInfo("98", CountryCode.UK, false));
        peers.add(new PeerInfo("99", CountryCode.AU, true));
        return peers;
    }
}