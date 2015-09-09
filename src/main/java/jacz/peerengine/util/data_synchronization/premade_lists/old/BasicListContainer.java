package jacz.peerengine.util.data_synchronization.premade_lists.old;

import jacz.peerengine.PeerID;
import jacz.peerengine.util.data_synchronization.old.ListAccessor;
import jacz.peerengine.util.data_synchronization.old.ListContainer;
import jacz.peerengine.util.data_synchronization.old.ListNotFoundException;

import java.util.HashMap;
import java.util.Map;

/**
 * A simple implementation of the ListContainer interface. Peers are added or removed dynamically, but lists must
 * be given at peer addition. Some transmitting lists can be given at construction, so every peer has them always.
 * <p/>
 * An implementation for inner list naming is also given
 */
public class BasicListContainer implements ListContainer {

    private class PeerLists {

        private PeerLists(Map<String, ListAccessor> transmittingLists, Map<String, ListAccessor> receivingLists) {
            this.transmittingLists = transmittingLists;
            this.receivingLists = receivingLists;
        }

        private Map<String, ListAccessor> transmittingLists;

        private Map<String, ListAccessor> receivingLists;
    }

    //public static final String INNER_LIST_SEPARATOR = "@";

    //public static final String INNER_LIST_LEVEL_SEPARATOR = "#";

    private Map<PeerID, PeerLists> data;

    private final Map<String, ListAccessor> basicTransmittingLists;

    public BasicListContainer() {
        data = new HashMap<>();
        basicTransmittingLists = new HashMap<>(0, 1.0f);
    }

    public BasicListContainer(Map<String, ListAccessor> basicTransmittingLists) {
        data = new HashMap<>();
        this.basicTransmittingLists = new HashMap<>(basicTransmittingLists);
    }

    public synchronized void addPeer(PeerID peerID, Map<String, ListAccessor> transmittingLists, Map<String, ListAccessor> receivingLists) {
        if (data.containsKey(peerID)) {
            removePeer(peerID);
        }
        PeerLists peerLists = new PeerLists(transmittingLists, receivingLists);
        peerLists.transmittingLists.putAll(basicTransmittingLists);
        data.put(peerID, peerLists);
    }

    public synchronized void removePeer(PeerID peerID) {
        data.remove(peerID);
    }

    @Override
    public ListAccessor getListForTransmitting(PeerID peerID, String list) throws ListNotFoundException {
        PeerLists peerLists = checkPeerAndListExists(peerID);
        return getList(list, peerLists.transmittingLists);
    }

    @Override
    public ListAccessor getListForReceiving(PeerID peerID, String list) throws ListNotFoundException {
        PeerLists peerLists = checkPeerAndListExists(peerID);
        return getList(list, peerLists.receivingLists);
    }

    private ListAccessor getList(String list, Map<String, ListAccessor> listMap) throws ListNotFoundException {
        if (listMap.containsKey(list)) {
            return listMap.get(list);
        } else {
            throw new ListNotFoundException();
        }
    }

    /*private ListAccessor getList(String list, Map<String, ListAccessor> listMap) throws ListNotFoundException {
        if (listMap.containsKey(list)) {
            return listMap.get(list);
        } else {
            // try to obtain an inner list
            ListAccessor listAccessor = null;
            StringTokenizer strTok = new StringTokenizer(list, INNER_LIST_SEPARATOR);
            while (strTok.hasMoreTokens()) {
                String token = strTok.nextToken();
                if (listAccessor == null) {
                    // first iteration, the token should be an existing list
                    if (listMap.containsKey(token)) {
                        listAccessor = listMap.get(token);
                    } else {
                        throw new ListNotFoundException();
                    }
                } else {
                    StringTokenizer strTok2 = new StringTokenizer(token, INNER_LIST_LEVEL_SEPARATOR);
                    if (strTok2.countTokens() != 2) {
                        throw new ListNotFoundException();
                    }
                    try {
                        int level = Integer.parseInt(strTok2.nextToken());
                        String hash = strTok2.nextToken();
                        listAccessor = listAccessor.getInnerList(level, hash);
                    } catch (NumberFormatException e) {
                        throw new ListNotFoundException();
                    } catch (ElementNotFoundException e) {
                        throw new ListNotFoundException();
                    }

                }
            }
            if (listAccessor != null) {
                return listAccessor;
            } else {
                throw new ListNotFoundException();
            }
        }
    }*/

    /*public static String generateInnerListName(String parentListName, int level, String hash) {
        return parentListName + INNER_LIST_SEPARATOR + level + INNER_LIST_LEVEL_SEPARATOR + hash;
    }*/

    private synchronized PeerLists checkPeerAndListExists(PeerID peerID) throws ListNotFoundException {
        if (data.containsKey(peerID)) {
            return data.get(peerID);
        } else {
            throw new ListNotFoundException();
        }
    }
}
