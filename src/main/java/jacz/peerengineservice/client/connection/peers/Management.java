package jacz.peerengineservice.client.connection.peers;

import jacz.storage.ActiveJDBCController;
import org.javalite.activejdbc.Base;

import java.text.SimpleDateFormat;

/**
 * Created by Alberto on 02/03/2016.
 */
public class Management {

    public static final SimpleDateFormat dateFormat = new SimpleDateFormat("Y/M/d-HH:mm:ss:SSS");

    static final String TABLE_NAME = "peer_entries";

    static final TableField PEER_ID = new TableField("peer_id", "TEXT NOT NULL PRIMARY KEY");

    static final TableField PUBLIC_KEY = new TableField("peer_id", "TEXT");

    static final TableField MAIN_LANGUAGE = new TableField("main_language", "TEXT");

    static final TableField MAIN_COUNTRY = new TableField("main_country", "TEXT");

    static final TableField RELATIONSHIP = new TableField("relationship", "TEXT NOT NULL");

    static final TableField IS_CONNECTED = new TableField("is_connected", "BOOLEAN NOT NULL");

    static final TableField LAST_SESSION = new TableField("last_session", "TEXT");

    static final TableField LAST_CONNECTION_ATTEMPT = new TableField("last_connection_attempt", "TEXT");

    static final TableField LAST_CONNECTION_ATTEMPT_RESULT = new TableField("last_connection_attempt_result", "TEXT");

    static final TableField WISH_FRIENDS_CONNECTIONS = new TableField("wish_friends_connections", "BOOLEAN NOT NULL");

    static final TableField WISH_2_GRADE_CONNECTIONS = new TableField("wish_2_grade_connections", "TEXT NOT NULL");

    static final TableField WISH_EVENTUAL_CONNECTIONS = new TableField("wish_eventual_connections", "TEXT NOT NULL");

    static final TableField AFFINITY = new TableField("wish_eventual_connections", "INTEGER");

    static final TableField ADDRESS = new TableField("address", "TEXT");

    static final TableField CONNECTED = new TableField("connected", "BOOLEAN");

    enum Relationship {
        FAVORITE,
        SECOND_ORDER,
        EVENTUAL
    }



    public synchronized static void createKBDatabase(String dbPath) {
        ActiveJDBCController.connect(dbPath);

        StringBuilder create = new StringBuilder("CREATE TABLE ").append(TABLE_NAME).append("(");
        appendField(create, PEER_ID, false);
        appendField(create, PUBLIC_KEY, false);
        appendField(create, MAIN_LANGUAGE, false);
        appendField(create, MAIN_COUNTRY, false);
        appendField(create, RELATIONSHIP, false);
        appendField(create, IS_CONNECTED, false);
        appendField(create, LAST_SESSION, false);
        appendField(create, LAST_CONNECTION_ATTEMPT, false);
        appendField(create, LAST_CONNECTION_ATTEMPT_RESULT, false);
        appendField(create, WISH_FRIENDS_CONNECTIONS, false);
        appendField(create, WISH_2_GRADE_CONNECTIONS, false);
        appendField(create, WISH_EVENTUAL_CONNECTIONS, false);
        appendField(create, AFFINITY, false);
        appendField(create, ADDRESS, true);
        appendField(create, CONNECTED, true);
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
