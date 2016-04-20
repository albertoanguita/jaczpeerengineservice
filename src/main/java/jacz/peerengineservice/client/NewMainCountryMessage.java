package jacz.peerengineservice.client;

import com.neovisionaries.i18n.CountryCode;
import jacz.peerengineservice.client.connection.peers.kb.Management;

import java.io.Serializable;

/**
 * Modified own main country message for all connected peers
 */
public class NewMainCountryMessage implements Serializable {

    final CountryCode mainCountry;

    NewMainCountryMessage(CountryCode mainCountry) {
        this.mainCountry = mainCountry;
    }
}
