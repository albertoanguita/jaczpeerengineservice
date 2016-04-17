package jacz.peerengineservice.client.connection.peers;

import com.neovisionaries.i18n.CountryCode;
import jacz.util.event.notification.NotificationEmitter;
import jacz.util.event.notification.NotificationProcessor;
import jacz.util.event.notification.NotificationReceiver;

import java.util.ArrayList;
import java.util.List;

/**
 * todo synchronize, remove public from set methods
 * todo use local storage
 */
public class PeerConnectionConfig implements NotificationEmitter {

    private static final int MIN_REGULAR_CONNECTIONS_FOR_OTHER_COUNTRIES = 5;

    public enum  Event {
        LOWER_MAX_CONNECTIONS,
        MODIFIED_WISH_REGULAR_CONNECTIONS,
        MODIFIED_MAIN_LANGUAGE,
        MODIFIED_MAIN_COUNTRY,
        MODIFIED_ADDITIONAL_LANGUAGES
    }

    private static final int DEFAULT_MAX_REGULAR_CONNECTIONS = 100;

    private static final boolean DEFAULT_REGULAR_WISH = true;

    private static final int DEFAULT_MAX_REGULAR_CONNECTIONS_ADDITIONAL_COUNTRY = 5;

    private int maxRegularConnections;

    private boolean wishRegularConnections;

    private CountryCode mainCountry;

    private List<CountryCode> additionalCountries;

    private int maxRegularConnectionsForAdditionalCountries;

    /**
     * For submitting config changes to some specific classes
     *
     * This notification processor does not need to be stopped, since no class will use the timed functions
     */
    private final NotificationProcessor notificationProcessor;


    public PeerConnectionConfig(CountryCode mainCountry) {
        this(DEFAULT_MAX_REGULAR_CONNECTIONS, DEFAULT_REGULAR_WISH, mainCountry, new ArrayList<>(), DEFAULT_MAX_REGULAR_CONNECTIONS_ADDITIONAL_COUNTRY);
    }

    public PeerConnectionConfig(
            int maxRegularConnections,
            boolean wishRegularConnections,
            CountryCode mainCountry,
            List<CountryCode> additionalCountries,
            int maxRegularConnectionsForAdditionalCountries) {
        this.maxRegularConnections = maxRegularConnections;
        this.wishRegularConnections = wishRegularConnections;
        this.mainCountry = mainCountry;
        this.additionalCountries = additionalCountries;
        this.maxRegularConnectionsForAdditionalCountries = maxRegularConnectionsForAdditionalCountries;
        notificationProcessor = new NotificationProcessor();
    }

    public int getMaxRegularConnections() {
        return maxRegularConnections;
    }

    public void setMaxRegularConnections(int maxRegularConnections) {
        this.maxRegularConnections = maxRegularConnections;
    }

    public boolean isWishRegularConnections() {
        return wishRegularConnections;
    }

    void setWishRegularConnections(boolean wishRegularConnections) {
        this.wishRegularConnections = wishRegularConnections;
    }

    public CountryCode getMainCountry() {
        return mainCountry;
    }

    public void setMainCountry(CountryCode mainCountry) {
        this.mainCountry = mainCountry;
    }

    public List<CountryCode> getAdditionalCountries() {
        return new ArrayList<>(additionalCountries);
    }

    public boolean isAdditionalCountry(CountryCode country) {
        return additionalCountries.contains(country);
    }

    public List<CountryCode> getAllCountries() {
        List<CountryCode> allCountries = getAdditionalCountries();
        allCountries.add(mainCountry);
        return allCountries;
    }

    public void setAdditionalCountries(List<CountryCode> additionalCountries) {
        this.additionalCountries = additionalCountries;
    }

    public int getMaxRegularConnectionsForAdditionalCountries() {
        return maxRegularConnectionsForAdditionalCountries;
    }

    public int getMaxRegularConnectionsForOtherCountries() {
        return Math.max(
                additionalCountries.size() * maxRegularConnectionsForAdditionalCountries + 1,
                MIN_REGULAR_CONNECTIONS_FOR_OTHER_COUNTRIES);
    }

    public void setMaxRegularConnectionsForAdditionalCountries(int maxRegularConnectionsForAdditionalLanguages) {
        this.maxRegularConnectionsForAdditionalCountries = maxRegularConnectionsForAdditionalLanguages;
    }

    @Override
    public String subscribe(String receiverID, NotificationReceiver notificationReceiver) throws IllegalArgumentException {
        return notificationProcessor.subscribeReceiver(receiverID, notificationReceiver);
    }

    @Override
    public String subscribe(String receiverID, NotificationReceiver notificationReceiver, long millis, double timeFactorAtEachEvent, int limit) throws IllegalArgumentException {
        // todo error, timed functions are not available here
        return null;
    }

    @Override
    public void unsubscribe(String receiverID) {
        notificationProcessor.unsubscribeReceiver(receiverID);
    }
}
