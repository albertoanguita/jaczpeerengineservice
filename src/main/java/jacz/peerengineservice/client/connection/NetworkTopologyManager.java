package jacz.peerengineservice.client.connection;

import org.aanguita.jacuzzi.AI.evolve.EvolvingState;
import org.aanguita.jacuzzi.AI.evolve.EvolvingStateController;
import org.aanguita.jacuzzi.io.http.HttpClient;
import org.aanguita.jacuzzi.lists.tuple.Duple;
import org.aanguita.jacuzzi.numeric.NumericUtil;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Handles the detection of local and external ip addresses. It also performs regular checks to see if those values
 * change
 */
public class NetworkTopologyManager {

    /**
     * This class checks that the local internet address is not modified. In case of modification, it notifies the PeerClientConfigurationManager
     * <p/>
     * This class must be stopped after its use
     */
    private static class AddressChecker {

        /**
         * Detects the local address assigned to our machine
         *
         * @return the detected local address, or null if no detection
         */
        public static String detectLocalAddress() throws IOException {
            try {
                Socket s = new Socket("www.google.com", 80);
                String localAddress = s.getLocalAddress().getHostAddress();
                s.close();
                return localAddress;
            } catch (UnknownHostException e) {
                return null;
            }
        }
    }

    private static class ExternalIPService {

        /**
         * Detects the external address assigned to our machine
         *
         * @return the value has changed from the last detection
         */
        public static String detectExternalAddress() {
            try {
                Duple<Integer, String> result = HttpClient.httpRequest(
                        "https://api.ipify.org",
                        HttpClient.Verb.GET,
                        HttpClient.ContentType.PLAIN);
                if (result.element1 == 200) {
                    return result.element2.trim();
                } else {
                    return null;
                }
            } catch (Exception e) {
                return null;
            }
        }
    }


    private final static long REPEAT_ADDRESS_FETCH = 10000L;

    private final static long REGULAR_ADDRESS_FETCH = 10L * 600L;

    private final static long GENERAL_REMINDER = NumericUtil.max(REPEAT_ADDRESS_FETCH, REGULAR_ADDRESS_FETCH) + 5000L;


    /**
     * The PeerClientConnectionManager that created us
     */
    private PeerClientConnectionManager peerClientConnectionManager;

    /**
     * Actions to invoke upon certain events
     */
    private final ConnectionEventsBridge connectionEvents;

    private String localAddress;

    private String externalAddress;

    private final EvolvingState<ConnectionState.NetworkTopologyState, Boolean> dynamicState;

    public NetworkTopologyManager(
            PeerClientConnectionManager peerClientConnectionManager,
            final ConnectionEventsBridge connectionEvents) {
        this.peerClientConnectionManager = peerClientConnectionManager;
        this.connectionEvents = connectionEvents;
        localAddress = null;
        externalAddress = null;
        dynamicState = new EvolvingState<>(ConnectionState.NetworkTopologyState.NO_DATA, false, new EvolvingState.Transitions<ConnectionState.NetworkTopologyState, Boolean>() {
            @Override
            public boolean runTransition(ConnectionState.NetworkTopologyState state, Boolean goal, EvolvingStateController<ConnectionState.NetworkTopologyState, Boolean> controller) {
                if (goal) {
                    // trying to fetch the local and external address
                    switch (state) {

                        case NO_DATA:
                        case WAITING_FOR_NEXT_LOCAL_ADDRESS_FETCH:
                            // fetch/check the local address
                            return fetchLocalAddress(state, controller);

                        case LOCAL_ADDRESS_FETCHED:
                        case WAITING_FOR_NEXT_EXTERNAL_ADDRESS_FETCH:
                            // fetch the public address, infer existence of gateway
                            return fetchExternalAddress(state, controller);
                    }
                }
                // else, no need for further changes
                return true;
            }

            @Override
            public boolean hasReachedGoal(ConnectionState.NetworkTopologyState state, Boolean goal) {
                return goal && state == ConnectionState.NetworkTopologyState.ALL_FETCHED || !goal;
            }

        }, "NetworkTopologyManager");
        dynamicState.setEnterStateHook(ConnectionState.NetworkTopologyState.LOCAL_ADDRESS_FETCHED, new Runnable() {
            @Override
            public void run() {
                connectionEvents.localAddressFetched(getLocalAddress(), ConnectionState.NetworkTopologyState.LOCAL_ADDRESS_FETCHED);
            }
        });
        dynamicState.setEnterStateHook(ConnectionState.NetworkTopologyState.ALL_FETCHED, new Runnable() {
            @Override
            public void run() {
                connectionEvents.externalAddressFetched(getExternalAddress(), hasGateway(), ConnectionState.NetworkTopologyState.ALL_FETCHED);
            }
        });
        dynamicState.setEvolveStateTimer(ConnectionState.NetworkTopologyState.NO_DATA, REPEAT_ADDRESS_FETCH);
        dynamicState.setEvolveStateTimer(ConnectionState.NetworkTopologyState.WAITING_FOR_NEXT_LOCAL_ADDRESS_FETCH, REPEAT_ADDRESS_FETCH);
        dynamicState.setEvolveStateTimer(ConnectionState.NetworkTopologyState.WAITING_FOR_NEXT_EXTERNAL_ADDRESS_FETCH, REPEAT_ADDRESS_FETCH);
        dynamicState.setRunnableStateTimer(ConnectionState.NetworkTopologyState.ALL_FETCHED, REGULAR_ADDRESS_FETCH, new Runnable() {
            @Override
            public void run() {
                fetchLocalAddress(dynamicState.state(), dynamicState);
            }
        });
        dynamicState.setEvolveStateTimer(state -> true, GENERAL_REMINDER);
    }

    private boolean fetchLocalAddress(ConnectionState.NetworkTopologyState state, EvolvingStateController<ConnectionState.NetworkTopologyState, Boolean> controller) {
        if (state == ConnectionState.NetworkTopologyState.NO_DATA) {
            // initial fetch
            connectionEvents.initializingConnection(ConnectionState.NetworkTopologyState.FETCHING_DATA);
        }
        try {
            boolean fetched = tryToFetchLocalAddress();
            if (fetched) {
                controller.setState(ConnectionState.NetworkTopologyState.LOCAL_ADDRESS_FETCHED);
                return false;
            } else {
                return true;
            }
        } catch (IOException e) {
            // error fetching local address
            controller.setState(ConnectionState.NetworkTopologyState.WAITING_FOR_NEXT_LOCAL_ADDRESS_FETCH);
            connectionEvents.couldNotFetchLocalAddress(state);
        }
        // else -> local address has not changed, all ok
        return true;
    }

    private boolean tryToFetchLocalAddress() throws IOException {
        String newLocalAddress = AddressChecker.detectLocalAddress();
        if (newLocalAddress == null) {
            throw new IOException("Could not fetch local address");
        }
        if (!newLocalAddress.equals(localAddress)) {
            // local address has changed
            localAddress = newLocalAddress;
            return true;
        } else {
            // local address has not changed, all ok
            return false;
        }
    }

    private boolean fetchExternalAddress(ConnectionState.NetworkTopologyState state, EvolvingStateController<ConnectionState.NetworkTopologyState, Boolean> controller) {
        connectionEvents.tryingToFetchExternalAddress(state);
        externalAddress = ExternalIPService.detectExternalAddress();
        if (externalAddress != null) {
            // external address successfully fetched
            controller.setState(ConnectionState.NetworkTopologyState.ALL_FETCHED);
        } else {
            // error fetching the external address (maybe there is no internet available). Retry in some time
            controller.setState(ConnectionState.NetworkTopologyState.WAITING_FOR_NEXT_EXTERNAL_ADDRESS_FETCH);
            connectionEvents.couldNotFetchExternalAddress(state);
            peerClientConnectionManager.networkProblem();
        }
        return true;
    }

    public String getLocalAddress() {
        return localAddress;
    }

    public String getExternalAddress() {
        return externalAddress;
    }

    public boolean hasGateway() {
        return isInWishedState() && !getLocalAddress().equals(getExternalAddress());
    }

    void setWishForConnect(boolean wishForConnect) {
        dynamicState.setGoal(wishForConnect, true);
    }

    boolean isInWishedState() {
        return dynamicState.hasReachedGoal();
    }

    void stop() {
        setWishForConnect(false);
        dynamicState.blockUntilGoalReached(500);
        dynamicState.stop();
    }
}
