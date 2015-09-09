package jacz.peerengine.util.data_synchronization.premade_lists.old;

import jacz.peerengine.PeerID;
import jacz.peerengine.client.PeerClient;
import jacz.peerengine.util.data_synchronization.old.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A simple non indexed list accessor for sharing the peer alias (nick)
 */
public class SimplePersonalData implements NonIndexedListAccessor {

    private final PeerID peerID;

    private String nick;

    private final ForeignPeerDataAction foreignPeerDataAction;

    public SimplePersonalData(PeerID peerID, String nick, ForeignPeerDataAction foreignPeerDataAction) {
        this.peerID = peerID;
        this.nick = nick;
        this.foreignPeerDataAction = foreignPeerDataAction;
    }

    public static String getListName() {
        return PeerClient.OWN_CUSTOM_PREFIX + "SimplePersonalData";
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    @Override
    public void beginSynchProcess(ListAccessor.Mode mode) {
        // ignore
    }

    @Override
    public Collection<String> getHashList() throws DataAccessException {
        List<String> hashList = new ArrayList<>();
        hashList.add(nick);
        return hashList;
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
        // ignore
        return TransmissionType.OBJECT;
    }

    @Override
    public Serializable getElementObject(String hash) throws ElementNotFoundException, DataAccessException {
        // ignore
        return getNick();
    }

    @Override
    public byte[] getElementByteArray(String hash) throws ElementNotFoundException, DataAccessException {
        // ignore
        return new byte[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getElementByteArrayLength(String hash) throws ElementNotFoundException, DataAccessException {
        // ignore
        return 0;
    }

    @Override
    public void addElementAsObject(Object element) throws DataAccessException {
        nick = (String) element;
        foreignPeerDataAction.newPeerNick(peerID, nick);
    }

    @Override
    public void addElementAsByteArray(byte[] data) throws DataAccessException {
        // ignore
    }

    @Override
    public boolean mustEraseOldIndexes() {
        return false;
    }

    @Override
    public void eraseElements(Collection<String> hashes) throws DataAccessException {
        // ignore
    }

    @Override
    public void endSynchProcess(ListAccessor.Mode mode, boolean success) {
        // ignore
    }

    @Override
    public ServerSynchRequestAnswer initiateListSynchronizationAsServer(PeerID clientPeerID, boolean singleElement) {
        return new ServerSynchRequestAnswer(ServerSynchRequestAnswer.Type.OK, null);
    }
}
