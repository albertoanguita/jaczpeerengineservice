package jacz.peerengineservice.client;

import jacz.peerengineservice.client.connection.peers.kb.Management;

import java.io.Serializable;

/**
 * Created by Alberto on 18/04/2016.
 */
class NewRelationshipMessage implements Serializable {

    final Management.Relationship relationship;

    NewRelationshipMessage(Management.Relationship relationship) {
        this.relationship = relationship;
    }
}
