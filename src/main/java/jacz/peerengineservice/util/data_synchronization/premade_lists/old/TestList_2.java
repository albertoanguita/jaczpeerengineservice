package jacz.peerengineservice.util.data_synchronization.premade_lists.old;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.test.list_synch.ListSynchProgress;
import jacz.peerengineservice.util.data_synchronization.old.*;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * Test for a non indexed list accessor
 */
public class TestList_2 implements NonIndexedListAccessor {

    private final List<String> values;

    public TestList_2(List<String> values) {
        this.values = values;
    }

    @Override
    public void beginSynchProcess(ListAccessor.Mode mode) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Collection<String> getHashList() throws DataAccessException {
        return values;
    }

    @Override
    public boolean hashEqualsElement() {
        return true;
    }

    @Override
    public boolean mustRequestElement(String index, int level, String hash) {
        return true;
    }

    @Override
    public TransmissionType getTransmissionType() {
        return TransmissionType.OBJECT;
    }

    @Override
    public Serializable getElementObject(String hash) throws ElementNotFoundException, DataAccessException {
        return hash;
    }

    @Override
    public byte[] getElementByteArray(String hash) throws ElementNotFoundException, DataAccessException {
        return new byte[0];
    }

    @Override
    public int getElementByteArrayLength(String hash) throws ElementNotFoundException, DataAccessException {
        return 0;
    }

    @Override
    public void addElementAsObject(Object element) throws DataAccessException {
        values.add((String) element);
    }

    @Override
    public void addElementAsByteArray(byte[] data) throws DataAccessException {
        // ignore
    }

    @Override
    public boolean mustEraseOldIndexes() {
        return true;
    }

    @Override
    public void eraseElements(Collection<String> hashes) throws DataAccessException {
        for (String valueToRemove : hashes) {
            values.remove(valueToRemove);
        }
    }

    @Override
    public void endSynchProcess(ListAccessor.Mode mode, boolean success) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ServerSynchRequestAnswer initiateListSynchronizationAsServer(PeerID clientPeerID, boolean singleElement) {
        return new ServerSynchRequestAnswer(ServerSynchRequestAnswer.Type.OK, new ListSynchProgress(clientPeerID, "TestList_2", false));
    }
}
