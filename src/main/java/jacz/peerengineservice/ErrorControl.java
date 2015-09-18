package jacz.peerengineservice;

import jacz.util.date_time.DateTime;
import jacz.util.files.FileReaderWriter;

import java.io.IOException;
import java.io.Serializable;

/**
 * This class handles internal errors (errors in the code of the peer engine) so that these are dumped into a log file, or console, or somewhere
 * where the dev can check it
 *
 * todo rename to Log, and also send logs here
 */
public final class ErrorControl {

    public static void reportError(Class c, String s, Object... data) {
        // dump error to a log file
        String baseLogName = "peer_engine_error_" +
                DateTime.getFormattedCurrentDateTime(
                        DateTime.DateTimeElement.YYYY, "-",
                        DateTime.DateTimeElement.MM, "-",
                        DateTime.DateTimeElement.DD, "--",
                        DateTime.DateTimeElement.hh, ":",
                        DateTime.DateTimeElement.mm, ":",
                        DateTime.DateTimeElement.ss);
        StringBuilder content = new StringBuilder("ERROR LOG\n---------\n\n");
        content.append(c.toString()).append(": ").append(s);
        int i = 0;
        for (Object o : data) {
            content.append("Object ").append(i).append("\n--------\n");
            content.append(o.toString()).append("\n");
            if (o instanceof Serializable) {
                try {
                    FileReaderWriter.writeObject(baseLogName + "object_" + i + ".log", (Serializable) o);
                    content.append("object serialization achieved\n");
                } catch (IOException e) {
                    // ignore
                }
            }
            content.append("\n");
        }
        try {
            FileReaderWriter.writeTextFile(baseLogName + ".log", content.toString());
        } catch (IOException e) {
            // could not write file
        }
    }
}
