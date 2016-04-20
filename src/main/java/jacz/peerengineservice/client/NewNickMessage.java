package jacz.peerengineservice.client;

import java.io.Serializable;

/**
 * Message for broadcasting nick changes
 */
class NewNickMessage implements Serializable {

    final String nick;

    NewNickMessage(String nick) {
        this.nick = nick;
    }
}
