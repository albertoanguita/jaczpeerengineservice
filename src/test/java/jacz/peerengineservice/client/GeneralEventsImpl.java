package jacz.peerengineservice.client;

import jacz.commengine.communication.CommError;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.util.PeerRelationship;

/**
 * Created by Alberto on 13/04/2016.
 */
public class GeneralEventsImpl implements GeneralEvents {

    @Override
    public void newOwnNick(String newNick) {
        System.out.println("New own nick: " + newNick);
    }

    @Override
    public void newObjectMessage(PeerId peerId, Object message) {
        System.out.println("New object message from " + formatPeer(peerId) + ": " + message);
    }

    @Override
    public void stop() {
        System.out.println("Stop");
    }

    private String formatPeer(PeerId peerId) {
        return "{" + peerId.toString().substring(40) + "}";
    }

}
