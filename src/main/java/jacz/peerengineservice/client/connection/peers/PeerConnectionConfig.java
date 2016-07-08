package jacz.peerengineservice.client.connection.peers;

import com.neovisionaries.i18n.CountryCode;
import org.aanguita.jacuzzi.io.serialization.localstorage.Updater;
import org.aanguita.jacuzzi.io.serialization.localstorage.VersionedLocalStorage;

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

    private static final double DEFAULT_PART_SELECTION_ACCURACY = 0.5d;





    /**** LOCAL STORAGE DATA AND KEYS ****/

    private static final String VERSION_0_1_0 = "0.1.0";

    private static final String CURRENT_VERSION = VERSION_0_1_0;

    private static final String MAX_REGULAR_CONNECTIONS = "maxRegularConnections";

    private static final String WISH_REGULAR_CONNECTIONS = "wishRegularConnections";

    private static final String MAIN_COUNTRY = "mainCountry";

    private static final String ADDITIONAL_COUNTRIES = "additionalCountries";

    private static final String MAX_REGULAR_CONNECTIONS_FOR_ADDITIONAL_COUNTRIES = "maxRegularConnectionsForAdditionalCountries";

    private static final String MAX_DOWNLOAD_SPEED = "maxDownloadSpeed";

    private static final String MAX_UPLOAD_SPEED = "maxUploadSpeed";

    /**
     * The accuracy employed in downloads for selecting the parts to assign to each resource provider
     * <p/>
     * The algorithm for assigning a segment to a slave is always approximate (to avoid excessive cpu utilization). It
     * can be however chosen how accurate this algorithm is, at the expense of higher cpu dependency. This attribute
     * indicates such accuracy. 1.0 means maximum accuracy, while 0.0 indicates minimum accuracy.
     * <p/>
     * Access to this field is synchronized to avoid inconsistencies
     */
    private static final String DOWNLOAD_PART_SELECTION_ACCURACY = "downloadPartSelectionAccuracy";

    private final VersionedLocalStorage localStorage;


    public PeerConnectionConfig(String localStoragePath, CountryCode mainCountry) throws IOException {
        localStorage = VersionedLocalStorage.createNew(localStoragePath, CURRENT_VERSION);
        setMaxRegularConnections(DEFAULT_MAX_REGULAR_CONNECTIONS);
        setWishRegularConnections(DEFAULT_REGULAR_WISH);
        setMainCountry(mainCountry);
        setAdditionalCountries(new ArrayList<>());
        setMaxRegularConnectionsForAdditionalCountries(DEFAULT_MAX_REGULAR_CONNECTIONS_ADDITIONAL_COUNTRY);
        setMaxDownloadSpeed(null);
        setMaxUploadSpeed(null);
        setDownloadPartSelectionAccuracy(DEFAULT_PART_SELECTION_ACCURACY);
    }

    public PeerConnectionConfig(String localStoragePath) throws IOException {
        localStorage = new VersionedLocalStorage(localStoragePath, this, CURRENT_VERSION);
    }

    public int getMaxRegularConnections() {
        return localStorage.getInteger(MAX_REGULAR_CONNECTIONS);
    }

    public boolean setMaxRegularConnections(int maxRegularConnections) {
        return localStorage.setInteger(MAX_REGULAR_CONNECTIONS, maxRegularConnections);
    }

    public boolean isWishRegularConnections() {
        return localStorage.getBoolean(WISH_REGULAR_CONNECTIONS);
    }

    public boolean setWishRegularConnections(boolean wishRegularConnections) {
        return localStorage.setBoolean(WISH_REGULAR_CONNECTIONS, wishRegularConnections);
    }

    public CountryCode getMainCountry() {
        return localStorage.getEnum(MAIN_COUNTRY, CountryCode.class);
    }

    public boolean setMainCountry(CountryCode mainCountry) {
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

    public void setAdditionalCountries(List<CountryCode> additionalCountries) {
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

    public boolean setMaxRegularConnectionsForAdditionalCountries(int maxRegularConnectionsForAdditionalLanguages) {
        return localStorage.setInteger(MAX_REGULAR_CONNECTIONS_FOR_ADDITIONAL_COUNTRIES, maxRegularConnectionsForAdditionalLanguages);
    }

    public Float getMaxDownloadSpeed() {
        return localStorage.getFloat(MAX_DOWNLOAD_SPEED);
    }

    public void setMaxDownloadSpeed(Float speed) {
        localStorage.setFloat(MAX_DOWNLOAD_SPEED, speed);
    }

    public Float getMaxUploadSpeed() {
        return localStorage.getFloat(MAX_UPLOAD_SPEED);
    }

    public void setMaxUploadSpeed(Float speed) {
        localStorage.setFloat(MAX_UPLOAD_SPEED, speed);
    }

    public double getDownloadPartSelectionAccuracy() {
        return localStorage.getDouble(DOWNLOAD_PART_SELECTION_ACCURACY);
    }

    public void setDownloadPartSelectionAccuracy(double accuracy) {
        localStorage.setDouble(DOWNLOAD_PART_SELECTION_ACCURACY, accuracy);
    }

    @Override
    public String update(VersionedLocalStorage versionedLocalStorage, String storedVersion) {
        // no versions yet, cannot be invoked
        return null;
    }
}
