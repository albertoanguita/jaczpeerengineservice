package jacz.peerengineservice.client.connection.peers.kb;

import jacz.storage.ActiveJDBCController;
import org.javalite.activejdbc.Base;

/**
 * Created by Alberto on 02/03/2016.
 */
public class Management {

    static final String TABLE_NAME = "peer_entries";

    static final TableField PEER_ID = new TableField("id", "TEXT NOT NULL PRIMARY KEY");

    static final TableField MAIN_COUNTRY = new TableField("main_country", "TEXT");

    static final TableField RELATIONSHIP = new TableField("relationship", "TEXT NOT NULL");

    static final TableField RELATIONSHIP_TO_US = new TableField("relationship_to_us", "TEXT NOT NULL");

    static final TableField WISH_CONNECTIONS = new TableField("wish_regular_connections", "TEXT NOT NULL");

    static final TableField IS_CONNECTED = new TableField("is_connected", "BOOLEAN NOT NULL");

    static final TableField LAST_SESSION = new TableField("last_session", "TEXT");

    static final TableField LAST_CONNECTION_ATTEMPT = new TableField("last_connection_attempt", "INTEGER");

    static final TableField AFFINITY = new TableField("affinity", "INTEGER NOT NULL");

    static final TableField ADDRESS = new TableField("address", "TEXT");

    static final TableField INFO_SOURCE = new TableField("info_source", "TEXT");

    public enum Relationship {
        FAVORITE,
        REGULAR,
        BLOCKED
    }

    public enum ConnectionWish {
        YES,
        NOT_NOW,
        NO
    }

    public enum InfoSource {
        OWN_RECORDS,
        ANOTHER_PEER,
        SERVER
    }


    public static void dropAndCreateKBDatabase(String path) {
        dropKBDatabase(path);
        createKBDatabase(path);
    }

    private static void dropKBDatabase(String path) {
        ActiveJDBCController.connect(path);
        Base.exec("DROP TABLE IF EXISTS " + TABLE_NAME);
        ActiveJDBCController.disconnect(path);
    }

    public synchronized static void createKBDatabase(String dbPath) {
        ActiveJDBCController.connect(dbPath);

        StringBuilder create = new StringBuilder("CREATE TABLE ").append(TABLE_NAME).append("(");
        appendField(create, PEER_ID, false);
        appendField(create, MAIN_COUNTRY, false);
        appendField(create, RELATIONSHIP, false);
        appendField(create, RELATIONSHIP_TO_US, false);
        appendField(create, WISH_CONNECTIONS, false);
        appendField(create, IS_CONNECTED, false);
        appendField(create, LAST_SESSION, false);
        appendField(create, LAST_CONNECTION_ATTEMPT, false);
        appendField(create, AFFINITY, false);
        appendField(create, ADDRESS, false);
        appendField(create, INFO_SOURCE, true);
        Base.exec(create.toString());

        ActiveJDBCController.disconnect(dbPath);
    }

    private static void appendField(StringBuilder create, TableField field, boolean isFinal) {
        create.append(field.name).append(" ").append(field.type);
        if (isFinal) {
            create.append(")");
        } else {
            create.append(",");
        }
    }
}
