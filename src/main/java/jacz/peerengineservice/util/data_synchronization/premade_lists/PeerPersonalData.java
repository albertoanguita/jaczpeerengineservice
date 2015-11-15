package jacz.peerengineservice.util.data_synchronization.premade_lists;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.client.PeerClientAction;
import jacz.peerengineservice.util.data_synchronization.DataAccessException;
import jacz.peerengineservice.util.data_synchronization.DataAccessor;
import jacz.peerengineservice.util.data_synchronization.ServerSynchRequestAnswer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * This class was initially designed to change peers nicks. The process is now doen via direct object broadcasting,
 * so there is no need for this. We maintain it in case it is needed in the future, and as sample of simple DataAccessor
 */
public class PeerPersonalData implements DataAccessor {

    private String nick;

    private final PeerID peerID;

    private final PeerClientAction peerClientAction;

    /**
     * Class constructor, for storing our own peer (no notification upon changes)
     *
     * @param nick our nick
     */
    public PeerPersonalData(String nick) {
        this(nick, null, null);
    }

    /**
     * Class constructor, for storing other peers' nick
     *
     * @param nick             peer's nick
     * @param peerID           peer to which this nick belongs
     * @param peerClientAction notification of peer's nick change
     */
    public PeerPersonalData(String nick, PeerID peerID, PeerClientAction peerClientAction) {
        this.nick = nick;
        this.peerID = peerID;
        this.peerClientAction = peerClientAction;
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    @Override
    public void beginSynchProcess(Mode mode) {
        // ignore
    }

    @Override
    public String getDatabaseID() {
        return null;
    }

    @Override
    public void setDatabaseID(String databaseID) {
        // ignore, no database ID is used
    }

    @Override
    public Integer getLastTimestamp() throws DataAccessException {
        // all elements are requested
        return null;
    }

    @Override
    public List<Serializable> getElements(int latestClientTimestamp) throws DataAccessException {
        List<Serializable> elements = new ArrayList<>();
        elements.add(getNick());
        return elements;
    }

    @Override
    public int elementsPerMessage() {
        return 1;
    }

    @Override
    public int CRCBytes() {
        return 4;
    }

    @Override
    public void setElement(Object element) throws DataAccessException {
        setNick((String) element);
        if (peerClientAction != null) {
            peerClientAction.newPeerNick(peerID, nick);
        }
    }

    @Override
    public void endSynchProcess(Mode mode, boolean success) {
        // ignore
    }

    @Override
    public ServerSynchRequestAnswer initiateListSynchronizationAsServer(PeerID clientPeerID) {
        // always accept these requests, no progress monitored
        return new ServerSynchRequestAnswer(ServerSynchRequestAnswer.Type.OK, null);
    }
}
