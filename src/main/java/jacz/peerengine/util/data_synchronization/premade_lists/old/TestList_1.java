package jacz.peerengine.util.data_synchronization.premade_lists.old;

import jacz.peerengine.PeerID;
import jacz.peerengine.test.personal_data_lists.ListSynchProgress;
import jacz.peerengine.util.data_synchronization.old.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * A ListAccessor with one single level, which is provided in the index itself
 */
public class TestList_1 implements ListAccessor {

    private final Set<String> values;

    public TestList_1(Set<String> values) {
        this.values = values;
    }

    @Override
    public int getLevelCount() {
        return 1;
    }

    @Override
    public void beginSynchProcess(Mode mode) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Collection<IndexAndHash> getHashList(int level) throws DataAccessException {
        List<IndexAndHash> list = new ArrayList<>();
        for (String value : values) {
            list.add(new IndexAndHash(value, null));
        }
        return list;
    }

    @Override
    public boolean hashEqualsElement(int level) {
        return true;
    }

    @Override
    public TransmissionType getTransmissionType(int level) {
        return null;
    }

    @Override
    public List<Integer> getInnerListLevels(int level) {
        return null;
    }

    @Override
    public ListAccessor getInnerList(String index, int level, boolean buildElementIfNeeded) throws ElementNotFoundException, DataAccessException {
        return null;
    }

    @Override
    public boolean mustRequestElement(String index, int level, String hash) {
        return true;
    }

    @Override
    public String getElementHash(String index, int requestLevel) throws ElementNotFoundException {
        return null;
    }

    @Override
    public Serializable getElementObject(String index, int level) throws ElementNotFoundException {
        return null;
    }

    @Override
    public byte[] getElementByteArray(String index, int level) throws ElementNotFoundException {
        return new byte[0];
    }

    @Override
    public int getElementByteArrayLength(String index, int level) throws ElementNotFoundException {
        return 0;
    }

    @Override
    public void addElementAsObject(String index, int level, Object element) {
        values.add(index);
    }

    @Override
    public void addElementAsByteArray(String index, int level, byte[] data) {
        // ignore
    }

    @Override
    public boolean mustEraseOldIndexes() {
        return true;
    }

    @Override
    public void eraseElements(Collection<String> indexes) {
        for (String valueToRemove : indexes) {
            values.remove(valueToRemove);
        }
    }

    @Override
    public void endSynchProcess(Mode mode, boolean success) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ServerSynchRequestAnswer initiateListSynchronizationAsServer(PeerID clientPeerID, int level, boolean singleElement) {
        return new ServerSynchRequestAnswer(ServerSynchRequestAnswer.Type.OK, new ListSynchProgress(clientPeerID, "TestList_1", level, false));
    }
}
