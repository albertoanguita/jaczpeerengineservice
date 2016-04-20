package jacz.peerengineservice.client.connection.peers;

import com.neovisionaries.i18n.CountryCode;
import jacz.util.io.serialization.localstorage.Updater;
import jacz.util.io.serialization.localstorage.VersionedLocalStorage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Connection configuration
 */
public class PeerConnectionConfig implements Updater {

    private static final int MIN_REGULAR_CONNECTIONS_FOR_OTHER_COUNTRIES = 5;

    private static final int DEFAULT_MAX_REGULAR_CONNECTIONS = 100;

    private static final boolean DEFAULT_REGULAR_WISH = true;

    private static final int DEFAULT_MAX_REGULAR_CONNECTIONS_ADDITIONAL_COUNTRY = 5;



    /**** LOCAL STORAGE DATA AND KEYS ****/

    private static final String VERSION_0_1_0 = "0.1.0";

    private static final String CURRENT_VERSION = VERSION_0_1_0;

    private static final String MAX_REGULAR_CONNECTIONS = "maxRegularConnections";

    private static final String WISH_REGULAR_CONNECTIONS = "wishRegularConnections";

    private static final String MAIN_COUNTRY = "mainCountry";

    private static final String ADDITIONAL_COUNTRIES = "additionalCountries";

    private static final String MAX_REGULAR_CONNECTIONS_FOR_ADDITIONAL_COUNTRIES = "maxRegularConnectionsForAdditionalCountries";

    private final VersionedLocalStorage localStorage;


    public PeerConnectionConfig(String localStoragePath, CountryCode mainCountry) throws IOException {
        localStorage = VersionedLocalStorage.createNew(localStoragePath, CURRENT_VERSION);
        setMaxRegularConnections(DEFAULT_MAX_REGULAR_CONNECTIONS);
        setWishRegularConnections(DEFAULT_REGULAR_WISH);
        setMainCountry(mainCountry);
        setAdditionalCountries(new ArrayList<>());
        setMaxRegularConnectionsForAdditionalCountries(DEFAULT_MAX_REGULAR_CONNECTIONS_ADDITIONAL_COUNTRY);
    }

    public PeerConnectionConfig(String localStoragePath) throws IOException {
        localStorage = new VersionedLocalStorage(localStoragePath, this, CURRENT_VERSION);
    }

    public PeerConnectionConfig(
            String localStoragePath,
            int maxRegularConnections,
            boolean wishRegularConnections,
            CountryCode mainCountry,
            List<CountryCode> additionalCountries,
            int maxRegularConnectionsForAdditionalCountries) throws IOException {
        this(localStoragePath, mainCountry);
        setMaxRegularConnections(maxRegularConnections);
        setWishRegularConnections(wishRegularConnections);
        setAdditionalCountries(additionalCountries);
        setMaxRegularConnectionsForAdditionalCountries(maxRegularConnectionsForAdditionalCountries);
    }

    public int getMaxRegularConnections() {
        return localStorage.getInteger(MAX_REGULAR_CONNECTIONS);
    }

    boolean setMaxRegularConnections(int maxRegularConnections) {
        return localStorage.setInteger(MAX_REGULAR_CONNECTIONS, maxRegularConnections);
    }

    public boolean isWishRegularConnections() {
        return localStorage.getBoolean(WISH_REGULAR_CONNECTIONS);
    }

    boolean setWishRegularConnections(boolean wishRegularConnections) {
        return localStorage.setBoolean(WISH_REGULAR_CONNECTIONS, wishRegularConnections);
    }

    public CountryCode getMainCountry() {
        return localStorage.getEnum(MAIN_COUNTRY, CountryCode.class);
    }

    boolean setMainCountry(CountryCode mainCountry) {
        return localStorage.setEnum(MAIN_COUNTRY, CountryCode.class, mainCountry);
    }

    public List<CountryCode> getAdditionalCountries() {
        return localStorage.getEnumList(ADDITIONAL_COUNTRIES, CountryCode.class);
    }

    public boolean isAdditionalCountry(CountryCode country) {
        return getAdditionalCountries().contains(country);
    }

    public List<CountryCode> getAllCountries() {
        List<CountryCode> allCountries = getAdditionalCountries();
        allCountries.add(getMainCountry());
        return allCountries;
    }

    void setAdditionalCountries(List<CountryCode> additionalCountries) {
        localStorage.setEnumList(ADDITIONAL_COUNTRIES, CountryCode.class, additionalCountries);
    }

    public int getMaxRegularConnectionsForAdditionalCountries() {
        return localStorage.getInteger(MAX_REGULAR_CONNECTIONS_FOR_ADDITIONAL_COUNTRIES);
    }

    public int getMaxRegularConnectionsForOtherCountries() {
        return Math.max(
                getAdditionalCountries().size() * getMaxRegularConnectionsForAdditionalCountries() + 1,
                MIN_REGULAR_CONNECTIONS_FOR_OTHER_COUNTRIES);
    }

    boolean setMaxRegularConnectionsForAdditionalCountries(int maxRegularConnectionsForAdditionalLanguages) {
        return localStorage.setInteger(MAX_REGULAR_CONNECTIONS_FOR_ADDITIONAL_COUNTRIES, maxRegularConnectionsForAdditionalLanguages);
    }

    @Override
    public String update(VersionedLocalStorage versionedLocalStorage, String storedVersion) {
        // no versions yet, cannot be invoked
        return null;
    }
}
