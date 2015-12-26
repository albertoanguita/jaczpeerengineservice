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
    private static class LocalAddressChecker implements SimpleTimerAction {

        private final static long LOCAL_ADDRESS_TIMER_NORMAL = 60000L;

        private final static long LOCAL_ADDRESS_TIMER_SHORT = 10000L;

        private final NetworkTopologyManager networkTopologyManager;

        private final Timer timer;

        private LocalAddressChecker(NetworkTopologyManager networkTopologyManager) {
            this.networkTopologyManager = networkTopologyManager;
            timer = new Timer(LOCAL_ADDRESS_TIMER_NORMAL, this, true, "LocalAddressChecker");
        }

        synchronized void mustSearchSoon() {
            timer.reset(LOCAL_ADDRESS_TIMER_SHORT);
        }

        @Override
        public Long wakeUp(Timer timer) {
            networkTopologyManager.updatedState();
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

    /**
     * Daemon thread that helps keep the state in its desired values
     */
    private final Daemon stateDaemon;

    private final LocalAddressChecker localAddressChecker;


    public NetworkTopologyManager(
            PeerClientConnectionManager peerClientConnectionManager,
            ConnectionEventsBridge connectionEvents) {
        this.peerClientConnectionManager = peerClientConnectionManager;
        this.connectionEvents = connectionEvents;
        localAddress = null;
        externalAddress = null;
        stateDaemon = new Daemon(this);
        localAddressChecker = new LocalAddressChecker(this);
        networkTopologyState = State.NetworkTopologyState.NO_DATA;
        updatedState();
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

    synchronized boolean isInWishedState() {
        return networkTopologyState == State.NetworkTopologyState.ALL_FETCHED;
    }

    void stop() {
        localAddressChecker.stop();
    }

    synchronized void updatedState() {
        stateDaemon.stateChange();
        stateDaemon.interrupt();
    }

    @Override
    public synchronized boolean solveState() {
        switch (networkTopologyState) {

            case NO_DATA:
                // fetch the local address
                connectionEvents.initializingConnection();
                return fetchLocalAddress();

            case LOCAL_ADDRESS_FETCHED:
                // fetch the public address, infer existence of gateway
                return fetchExternalAddress();

            case ALL_FETCHED:
                // check local address
                return fetchLocalAddress();
        }
        // cannot happen
        return true;
    }

    private synchronized boolean fetchLocalAddress() {
        String newLocalAddress = LocalAddressChecker.detectLocalAddress();
        if (newLocalAddress != null && !newLocalAddress.equals(localAddress)) {
            // local address has changed
            localAddress = newLocalAddress;
            networkTopologyState = State.NetworkTopologyState.LOCAL_ADDRESS_FETCHED;
            connectionEvents.localAddressFetched(getLocalAddress(), networkTopologyState);
            return false;
        } else if (newLocalAddress == null) {
            // error fetching local address
            networkTopologyState = State.NetworkTopologyState.WAITING_FOR_NEXT_LOCAL_ADDRESS_FETCH;
            localAddressChecker.mustSearchSoon();
            connectionEvents.couldNotFetchLocalAddress(networkTopologyState);
            peerClientConnectionManager.networkNotAvailable();
            return true;
        } else {
            // local address has not changed, all ok
            return true;
        }
    }

    private synchronized boolean fetchExternalAddress() {
        connectionEvents.tryingToFetchExternalAddress(networkTopologyState);
        externalAddress = ExternalIPService.detectExternalAddress();
        if (externalAddress != null) {
            // external address successfully fetched
            networkTopologyState = State.NetworkTopologyState.ALL_FETCHED;
            connectionEvents.externalAddressFetched(getExternalAddress(), hasGateway(), networkTopologyState);
            return true;
        } else {
            // error fetching the external address (maybe there is no internet available)
            connectionEvents.couldNotFetchExternalAddress(networkTopologyState);
            return false;
        }
    }

    public static void main(String[] args) {
        System.out.println(ExternalIPService.detectExternalAddress());
    }
}
