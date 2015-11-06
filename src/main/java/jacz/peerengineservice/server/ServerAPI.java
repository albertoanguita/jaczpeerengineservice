package jacz.peerengineservice.server;

import com.google.gson.Gson;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.client.PeerClient;
import jacz.util.io.http.HttpClient;
import jacz.util.lists.Duple;
import jacz.util.log.ErrorLog;

import java.io.IOException;

/**
 * This class provides methods for accessing the remote server functionality
 */
public class ServerAPI {

    public static final class RegistrationRequest {

        private final String peerID;

        public RegistrationRequest(PeerID peerID) {
            this.peerID = peerID.toString();
        }
    }

    private class RegistrationResponseJSON {

        private String response;
    }

    public enum RegistrationResponse {
        OK,
        ALREADY_REGISTERED
    }

    public static final class ConnectionRequest {

        private final String peerID;

        private final String localIPAddress;

        private final int localMainServerPort;

        private final int externalMainServerPort;

        public ConnectionRequest(PeerID peerID, String localIPAddress, int localMainServerPort, int externalMainServerPort) {
            this.peerID = peerID.toString();
            this.localIPAddress = localIPAddress;
            this.localMainServerPort = localMainServerPort;
            this.externalMainServerPort = externalMainServerPort;
        }
    }

    private class ConnectionResponseJSON {

        private String response;
        private String sessionID;
        private String minReminderTime;
        private String maxReminderTime;
    }

    public enum ConnectionResponseType {
        OK,
        UNREGISTERED_PEER,
        PEER_MAIN_SERVER_UNREACHABLE,
        PEER_REST_SERVER_UNREACHABLE,
        WRONG_AUTHENTICATION
    }

    public static class ConnectionResponse {

        private ConnectionResponseType response;

        private String sessionID;

        private int minReminderTime;

        private int maxReminderTime;

        private static ConnectionResponse buildConnectionResponse(ConnectionResponseJSON connectionResponseJSON) {
            ConnectionResponse connectionResponse = new ConnectionResponse();
            connectionResponse.response = ConnectionResponseType.valueOf(connectionResponseJSON.response);
            if (connectionResponse.response == ConnectionResponseType.OK) {
                connectionResponse.sessionID = connectionResponseJSON.sessionID;
                connectionResponse.minReminderTime = Integer.parseInt(connectionResponseJSON.minReminderTime);
                connectionResponse.maxReminderTime = Integer.parseInt(connectionResponseJSON.maxReminderTime);
            }
            return connectionResponse;
        }

        @Override
        public String toString() {
            return "ConnectionResponse{" +
                    "response=" + response +
                    ", sessionID='" + sessionID + '\'' +
                    ", minReminderTime=" + minReminderTime +
                    ", maxReminderTime=" + maxReminderTime +
                    '}';
        }
    }

    public static final class UpdateRequest {

        private final String sessionID;

        public UpdateRequest(String sessionID) {
            this.sessionID = sessionID;
        }
    }

    public static final class UpdateResponse {

        private String response;

        @Override
        public String toString() {
            return "UpdateResponse{" +
                    "response='" + response + '\'' +
                    '}';
        }
    }

    public static String hello() throws IOException, ServerAccessException {
        Duple<Integer, String> result = HttpClient.httpRequest(
                "https://testserver01-1100.appspot.com/_ah/api/server/v1/hello",
                HttpClient.Verb.GET,
                HttpClient.ContentType.JSON);
        checkError(result);
        return result.element2;
    }

    public static RegistrationResponse register(RegistrationRequest registrationRequest) throws IOException, ServerAccessException {
        Duple<Integer, String> result = HttpClient.httpRequest(
                "https://testserver01-1100.appspot.com/_ah/api/server/v1/register",
                HttpClient.Verb.POST,
                HttpClient.ContentType.JSON,
                HttpClient.ContentType.JSON,
                new Gson().toJson(registrationRequest));
        checkError(result);
        RegistrationResponseJSON registrationResponseJSON = new Gson().fromJson(result.element2, RegistrationResponseJSON.class);
        try {
            return RegistrationResponse.valueOf(registrationResponseJSON.response);
        } catch (IllegalArgumentException e) {
            // unrecognized value --> log error and re-throw
            ErrorLog.reportError(PeerClient.ERROR_LOG, "Unrecognized register response", result.element2);
            throw e;
        }
    }

    public static ConnectionResponse connect(ConnectionRequest connectionRequest) throws IOException, ServerAccessException {
        Duple<Integer, String> result = HttpClient.httpRequest(
                "https://testserver01-1100.appspot.com/_ah/api/server/v1/connect",
                HttpClient.Verb.POST,
                HttpClient.ContentType.JSON,
                HttpClient.ContentType.JSON,
                new Gson().toJson(connectionRequest));
        checkError(result);
        ConnectionResponseJSON connectionResponseJSON = new Gson().fromJson(result.element2, ConnectionResponseJSON.class);
        try {
            return ConnectionResponse.buildConnectionResponse(connectionResponseJSON);
        } catch (IllegalArgumentException e) {
            // unrecognized values --> log error and re-throw
            ErrorLog.reportError(PeerClient.ERROR_LOG, "Unrecognized connect response", result.element2);
            throw e;
        }
    }

    public static UpdateResponse refresh(UpdateRequest updateRequest) throws IOException, ServerAccessException {
        Duple<Integer, String> result = HttpClient.httpRequest(
                "https://testserver01-1100.appspot.com/_ah/api/server/v1/refresh",
                HttpClient.Verb.POST,
                HttpClient.ContentType.JSON,
                HttpClient.ContentType.JSON,
                new Gson().toJson(updateRequest));
        checkError(result);
        try {
            return new Gson().fromJson(result.element2, UpdateResponse.class);
        } catch (Exception e) {
            // unrecognized values --> log error and re-throw
            ErrorLog.reportError(PeerClient.ERROR_LOG, "Unrecognized refresh response", result.element2);
            throw new IOException("Unrecognized refresh response");
        }
    }

    public static UpdateResponse disconnect(UpdateRequest updateRequest) throws IOException, ServerAccessException {
        Duple<Integer, String> result = HttpClient.httpRequest(
                "https://testserver01-1100.appspot.com/_ah/api/server/v1/disconnect",
                HttpClient.Verb.POST,
                HttpClient.ContentType.JSON,
                HttpClient.ContentType.JSON,
                new Gson().toJson(updateRequest));
        checkError(result);
        try {
            return new Gson().fromJson(result.element2, UpdateResponse.class);
        } catch (Exception e) {
            // unrecognized values --> log error and re-throw
            ErrorLog.reportError(PeerClient.ERROR_LOG, "Unrecognized refresh response", result.element2);
            throw new IOException("Unrecognized refresh response");
        }
    }

    public static UpdateResponse info(UpdateRequest updateRequest) throws IOException, ServerAccessException {
        Duple<Integer, String> result = HttpClient.httpRequest(
                "https://testserver01-1100.appspot.com/_ah/api/server/v1/info",
                HttpClient.Verb.POST,
                HttpClient.ContentType.JSON,
                HttpClient.ContentType.JSON,
                new Gson().toJson(updateRequest));
        checkError(result);
        try {
            return new Gson().fromJson(result.element2, UpdateResponse.class);
        } catch (Exception e) {
            // unrecognized values --> log error and re-throw
            ErrorLog.reportError(PeerClient.ERROR_LOG, "Unrecognized refresh response", result.element2);
            throw new IOException("Unrecognized refresh response");
        }
    }

    private static void checkError(Duple<Integer, String> response) throws ServerAccessException {
        if (response.element1 / 100 == 4) {
            ErrorLog.reportError(PeerClient.ERROR_LOG, "Bad request", response.element2);
        }
        if (response.element1 / 100 == 4 || response.element1 / 100 == 5) {
            throw new ServerAccessException(response.element2, response.element1);
        }
    }


    public static void main(String[] args) throws Exception {
//        System.out.println(hello());

        RegistrationResponse registrationResponse = register(new RegistrationRequest(new PeerID("0000000000000000000000000000000000000000003")));

        System.out.println(registrationResponse);


//        ConnectionResponse connectionResponse = connect(new ConnectionRequest(new PeerID("0000000000000000000000000000000000000000003"), "192.168.1.1", 50000, 50001));
//        System.out.println(connectionResponse);

//        UpdateResponse refreshResponse = refresh(new UpdateRequest("ahNlfnRlc3RzZXJ2ZXIwMS0xMTAwchoLEg1BY3RpdmVTZXNzaW9uGICAgIC6jYkKDA"));
//        System.out.println(refreshResponse);


        UpdateResponse disconnectResponse = disconnect(new UpdateRequest("ahNlfnRlc3RzZXJ2ZXIwMS0xMTAwchoLEg1BY3RpdmVTZXNzaW9uGICAgIC6jYkKDA"));
        System.out.println(disconnectResponse);


    }
}
