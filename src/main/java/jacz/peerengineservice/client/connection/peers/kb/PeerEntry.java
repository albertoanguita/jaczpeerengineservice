package jacz.peerengineservice.client.connection.peers.kb;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.DbName;
import org.javalite.activejdbc.annotations.Table;

/**
 * Active JDBC main table for the peer knowledge base
 */
@DbName(PeerKnowledgeBase.DATABASE)
@Table("peer_entries")
public class PeerEntry extends Model {
}
