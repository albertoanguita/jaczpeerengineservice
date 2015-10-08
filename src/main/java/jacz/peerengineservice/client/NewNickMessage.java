package jacz.peerengineservice.client;

import java.io.Serializable;

/**
 * Message for broadcasting nick changes
 */
public class NewNickMessage implements Serializable {

    final String nick;

    public NewNickMessage(String nick) {
        this.nick = nick;
    }
}
