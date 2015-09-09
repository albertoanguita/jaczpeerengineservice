package jacz.peerengine.util.data_synchronization.old;

import jacz.peerengine.PeerID;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Bridge for non-indexed list accessor
 */
public class NonIndexedListAccessorBridge implements ListAccessor {

    private final NonIndexedListAccessor nonIndexedListAccessor;

    public NonIndexedListAccessorBridge(NonIndexedListAccessor nonIndexedListAccessor) {
        this.nonIndexedListAccessor = nonIndexedListAccessor;
    }

    @Override
    public int getLevelCount() {
        return 1;
    }

    @Override
    public void beginSynchProcess(Mode mode) {
        nonIndexedListAccessor.beginSynchProcess(mode);
    }

    @Override
    public Collection<IndexAndHash> getHashList(int level) throws DataAccessException {
        Collection<String> hashList = nonIndexedListAccessor.getHashList();
        List<IndexAndHash> list = new ArrayList<>();
        for (String value : hashList) {
            list.add(new IndexAndHash(value, null));
        }
        return list;
    }

    @Override
    public boolean hashEqualsElement(int level) {
        return nonIndexedListAccessor.hashEqualsElement();
    }

    @Override
    public TransmissionType getTransmissionType(int level) {
        switch (nonIndexedListAccessor.getTransmissionType()) {

            case OBJECT:
                return TransmissionType.OBJECT;

            case BYTE_ARRAY:
                return TransmissionType.BYTE_ARRAY;

            default:
                return null;
        }
    }

    @Override
    public List<Integer> getInnerListLevels(int level) {
        // cannot happen
        return null;
    }

    @Override
    public ListAccessor getInnerList(String index, int level, boolean buildElementIfNeeded) throws ElementNotFoundException, DataAccessException {
        // cannot happen
        return null;
    }

    @Override
    public boolean mustRequestElement(String index, int level, String hash) throws DataAccessException {
        return nonIndexedListAccessor.mustRequestElement(index, level, hash);
    }

    @Override
    public String getElementHash(String index, int requestLevel) throws ElementNotFoundException {
        return index;
    }

    @Override
    public Serializable getElementObject(String index, int level) throws ElementNotFoundException, DataAccessException {
        return nonIndexedListAccessor.getElementObject(index);
    }

    @Override
    public byte[] getElementByteArray(String index, int level) throws ElementNotFoundException, DataAccessException {
        return nonIndexedListAccessor.getElementByteArray(index);
    }

    @Override
    public int getElementByteArrayLength(String index, int level) throws ElementNotFoundException, DataAccessException {
        return nonIndexedListAccessor.getElementByteArrayLength(index);
    }

    @Override
    public void addElementAsObject(String index, int level, Object element) throws DataAccessException {
        if (hashEqualsElement(level)) {
            nonIndexedListAccessor.addElementAsObject(index);
        } else {
            nonIndexedListAccessor.addElementAsObject(element);
        }
    }

    @Override
    public void addElementAsByteArray(String index, int level, byte[] data) throws DataAccessException {
        nonIndexedListAccessor.addElementAsByteArray(data);
    }

    @Override
    public boolean mustEraseOldIndexes() {
        return nonIndexedListAccessor.mustEraseOldIndexes();
    }

    @Override
    public void eraseElements(Collection<String> indexes) throws DataAccessException {
        nonIndexedListAccessor.eraseElements(indexes);
    }

    @Override
    public void endSynchProcess(Mode mode, boolean success) {
        nonIndexedListAccessor.endSynchProcess(mode, success);
    }

    @Override
    public ServerSynchRequestAnswer initiateListSynchronizationAsServer(PeerID clientPeerID, int level, boolean singleElement) {
        return nonIndexedListAccessor.initiateListSynchronizationAsServer(clientPeerID, singleElement);
    }
}
