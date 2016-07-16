package jacz.peerengineservice.util.datatransfer.resource_accession;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;

/**
 * A resource writer that stores user and system dictionary in-memory, as it has no support for disk storage of
 * dictionaries
 */
public abstract class SingleSessionResourceWriter implements ResourceWriter {

    private final HashMap<String, Serializable> userDictionary;

    private final HashMap<String, Serializable> systemDictionary;


    public SingleSessionResourceWriter(HashMap<String, Serializable> userDictionary) {
        this.userDictionary = userDictionary;
        systemDictionary = new HashMap<>();
    }

    @Override
    public HashMap<String, Serializable> getUserDictionary() {
        return userDictionary;
    }

    @Override
    public HashMap<String, Serializable> getSystemDictionary() {
        return systemDictionary;
    }

    @Override
    public void setSystemField(String key, Serializable value) throws IOException {
        systemDictionary.put(key, value);
    }
}
