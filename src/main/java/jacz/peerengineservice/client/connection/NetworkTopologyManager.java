package jacz.peerengineservice.client.connection;

import jacz.util.concurrency.daemon.Daemon;
import jacz.util.concurrency.daemon.DaemonAction;
import jacz.util.concurrency.timer.SimpleTimerAction;
import jacz.util.concurrency.timer.Timer;
import jacz.util.io.http.HttpClient;
import jacz.util.lists.tuple.Duple;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * This class takes care of detecting local network topology (local ip, external ip, existence of gateway)
 * <p/>
 * It also detects changes in local address by periodically fetching it
 */
public class NetworkTopologyManager implements DaemonAction {

    /**
     * This class checks that the local internet address is not modified. In case of modification, it notifies the PeerClientConfigurationManager
     * <p/>
     * This class must be stopped after its use
     */
    private static class AddressChecker implements SimpleTimerAction {

        private final static long LOCAL_ADDRESS_TIMER_NORMAL = 60000L;

        private final static long LOCAL_ADDRESS_TIMER_SHORT = 10000L;

        private final NetworkTopologyManager networkTopologyManager;

        private final Timer timer;

        private AddressChecker(NetworkTopologyManager networkTopologyManager) {
            this.networkTopologyManager = networkTopologyManager;
            timer = new Timer(LOCAL_ADDRESS_TIMER_NORMAL, this, true, "addressChecker");
        }

        synchronized void mustSearchSoon() {
            timer.reset(LOCAL_ADDRESS_TIMER_SHORT);
        }

        @Override
        public Long wakeUp(Timer timer) {
            networkTopologyManager.updateState();
            return LOCAL_ADDRESS_TIMER_NORMAL;
        }

        /**
         * Detects the local address assigned to our machine
         *
         * @return the detected local address, or null if no detection
         */
        public static String detectLocalAddress() {
            try {
                InetAddress inetAddress = InetAddress.getLocalHost();
                return inetAddress != null ? inetAddress.getHostAddress() : null;
            } catch (UnknownHostException e) {
                return null;
            }
        }


        public void stop() {
            timer.kill();
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

    /**
     * Status of the network topology fetching
     */
    private State.NetworkTopologyState networkTopologyState;

    private boolean wishForConnect;

    /**
     * Daemon thread that helps keep the state in its desired values
     */
    private final Daemon stateDaemon;

    private final AddressChecker addressChecker;


    public NetworkTopologyManager(
            PeerClientConnectionManager peerClientConnectionManager,
            ConnectionEventsBridge connectionEvents) {
        this.peerClientConnectionManager = peerClientConnectionManager;
        this.connectionEvents = connectionEvents;
        localAddress = null;
        externalAddress = null;
        wishForConnect = false;
        stateDaemon = new Daemon(this);
        addressChecker = new AddressChecker(this);
        networkTopologyState = State.NetworkTopologyState.NO_DATA;
        updateState();
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

    synchronized void setWishForConnect(boolean wishForConnect) {
        boolean mustUpdateState = this.wishForConnect != wishForConnect;
        this.wishForConnect = wishForConnect;
        if (mustUpdateState) {
            updateState();
        }
    }

    synchronized boolean isInWishedState() {
        return  (!wishForConnect || networkTopologyState == State.NetworkTopologyState.ALL_FETCHED);
    }

    void stop() {
        addressChecker.stop();
        setWishForConnect(false);
        stateDaemon.blockUntilStateIsSolved();
    }

    synchronized void updateState() {
        stateDaemon.stateChange();
        stateDaemon.interrupt();
    }

    @Override
    public synchronized boolean solveState() {
        if (wishForConnect) {
            // only act for fetching the network topology if there is a wish for connecting
            switch (networkTopologyState) {

                case NO_DATA:
                    // fetch the local address
                    connectionEvents.initializingConnection();
                    return fetchLocalAddress();

                case LOCAL_ADDRESS_FETCHED:
                case WAITING_FOR_NEXT_EXTERNAL_ADDRESS_FETCH:
                    // fetch the public address, infer existence of gateway
                    return fetchExternalAddress();

                case ALL_FETCHED:
                case WAITING_FOR_NEXT_LOCAL_ADDRESS_FETCH:
                    // check local address
                    return fetchLocalAddress();
            }
        } else {
            networkTopologyState = State.NetworkTopologyState.NO_DATA;
            return true;
        }
        // cannot happen
        return true;
    }

    private boolean fetchLocalAddress() {
        String newLocalAddress = AddressChecker.detectLocalAddress();
        if (newLocalAddress != null && !newLocalAddress.equals(localAddress)) {
            // local address has changed
            localAddress = newLocalAddress;
            networkTopologyState = State.NetworkTopologyState.LOCAL_ADDRESS_FETCHED;
            connectionEvents.localAddressFetched(getLocalAddress(), networkTopologyState);
            return false;
        } else if (newLocalAddress == null) {
            // error fetching local address
            networkTopologyState = State.NetworkTopologyState.WAITING_FOR_NEXT_LOCAL_ADDRESS_FETCH;
            addressChecker.mustSearchSoon();
            connectionEvents.couldNotFetchLocalAddress(networkTopologyState);
            peerClientConnectionManager.networkProblem();
            return true;
        } else {
            // local address has not changed, all ok
            return true;
        }
    }

    private boolean fetchExternalAddress() {
        connectionEvents.tryingToFetchExternalAddress(networkTopologyState);
        externalAddress = ExternalIPService.detectExternalAddress();
        if (externalAddress != null) {
            // external address successfully fetched
            networkTopologyState = State.NetworkTopologyState.ALL_FETCHED;
            connectionEvents.externalAddressFetched(getExternalAddress(), hasGateway(), networkTopologyState);
            return true;
        } else {
            // error fetching the external address (maybe there is no internet available). Retry in some time
            networkTopologyState = State.NetworkTopologyState.WAITING_FOR_NEXT_EXTERNAL_ADDRESS_FETCH;
            addressChecker.mustSearchSoon();
            connectionEvents.couldNotFetchExternalAddress(networkTopologyState);
            peerClientConnectionManager.networkProblem();
            return true;
        }
    }
}
