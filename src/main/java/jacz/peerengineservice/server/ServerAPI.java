package jacz.peerengineservice.server;

import com.google.gson.Gson;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.client.PeerClient;
import jacz.util.io.http.HttpClient;
import jacz.util.lists.Duple;
import jacz.util.log.ErrorLog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

        private long minReminderTime;

        private long maxReminderTime;

        public ConnectionResponseType getResponse() {
            return response;
        }

        public String getSessionID() {
            return sessionID;
        }

        public long getMinReminderTime() {
            return minReminderTime;
        }

        public long getMaxReminderTime() {
            return maxReminderTime;
        }

        private static ConnectionResponse buildConnectionResponse(ConnectionResponseJSON connectionResponseJSON) {
            ConnectionResponse connectionResponse = new ConnectionResponse();
            connectionResponse.response = ConnectionResponseType.valueOf(connectionResponseJSON.response);
            if (connectionResponse.response == ConnectionResponseType.OK) {
                connectionResponse.sessionID = connectionResponseJSON.sessionID;
                connectionResponse.minReminderTime = Long.parseLong(connectionResponseJSON.minReminderTime);
                connectionResponse.maxReminderTime = Long.parseLong(connectionResponseJSON.maxReminderTime);
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

    private class UpdateResponseJSON {

        private String response;
    }

    public enum RefreshResponse {

        OK,
        UNRECOGNIZED_SESSION,
        TOO_SOON
    }

    public enum DisconnectResponse {

        OK,
        UNRECOGNIZED_SESSION
    }

    public static final class InfoRequest {

        private final List<String> peerIDList;

        public InfoRequest(List<PeerID> peerIDList) {
            this.peerIDList = new ArrayList<>();
            for (PeerID peerID : peerIDList) {
                this.peerIDList.add(peerID.toString());
            }
        }
    }

    public static final class InfoResponseJSON {

        private List<PeerIDInfoJSON> peerIDInfoList;

        public InfoResponseJSON() {
            peerIDInfoList = new ArrayList<>();
        }
    }

    public static final class PeerIDInfoJSON {

        private String peerID;
        private String localIPAddress;
        private String externalIPAddress;
        private String localMainServerPort;
        private String localRESTServerPort;
        private String externalMainServerPort;
        private String externalRESTServerPort;
    }

    public static final class InfoResponse {

        private final List<PeerIDInfo> peerIDInfoList;

        private InfoResponse(List<PeerIDInfo> peerIDInfoList) {
            this.peerIDInfoList = peerIDInfoList;
        }

        public List<PeerIDInfo> getPeerIDInfoList() {
            return peerIDInfoList;
        }

        private static InfoResponse buildInfoResponse(InfoResponseJSON infoResponseJson) {
            List<PeerIDInfo> peerIDInfoList = new ArrayList<>();
            for (PeerIDInfoJSON peerIDInfoJson : infoResponseJson.peerIDInfoList) {
                peerIDInfoList.add(PeerIDInfo.buildPeerIDInfo(peerIDInfoJson));
            }
            return new InfoResponse(peerIDInfoList);
        }
    }

    public static final class PeerIDInfo {

        private final PeerID peerID;
        private final String localIPAddress;
        private final String externalIPAddress;
        private final int localMainServerPort;
        private final int localRESTServerPort;
        private final int externalMainServerPort;
        private final int externalRESTServerPort;

        public PeerIDInfo(PeerID peerID,
                          String localIPAddress,
                          String externalIPAddress,
                          int localMainServerPort,
                          int localRESTServerPort,
                          int externalMainServerPort,
                          int externalRESTServerPort) {
            this.peerID = peerID;
            this.localIPAddress = localIPAddress;
            this.externalIPAddress = externalIPAddress;
            this.localMainServerPort = localMainServerPort;
            this.localRESTServerPort = localRESTServerPort;
            this.externalMainServerPort = externalMainServerPort;
            this.externalRESTServerPort = externalRESTServerPort;
        }

        public PeerID getPeerID() {
            return peerID;
        }

        public String getLocalIPAddress() {
            return localIPAddress;
        }

        public String getExternalIPAddress() {
            return externalIPAddress;
        }

        public int getLocalMainServerPort() {
            return localMainServerPort;
        }

        public int getLocalRESTServerPort() {
            return localRESTServerPort;
        }

        public int getExternalMainServerPort() {
            return externalMainServerPort;
        }

        public int getExternalRESTServerPort() {
            return externalRESTServerPort;
        }

        private static PeerIDInfo buildPeerIDInfo(PeerIDInfoJSON peerIDInfoJson) {
            return new PeerIDInfo(
                    new PeerID(peerIDInfoJson.peerID),
                    peerIDInfoJson.localIPAddress,
                    peerIDInfoJson.externalIPAddress,
                    Integer.parseInt(peerIDInfoJson.localMainServerPort),
                    Integer.parseInt(peerIDInfoJson.localRESTServerPort),
                    Integer.parseInt(peerIDInfoJson.externalMainServerPort),
                    Integer.parseInt(peerIDInfoJson.externalRESTServerPort)
            );
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

    public static RefreshResponse refresh(UpdateRequest updateRequest) throws IOException, ServerAccessException {
        Duple<Integer, String> result = HttpClient.httpRequest(
                "https://testserver01-1100.appspot.com/_ah/api/server/v1/refresh",
                HttpClient.Verb.POST,
                HttpClient.ContentType.JSON,
                HttpClient.ContentType.JSON,
                new Gson().toJson(updateRequest));
        checkError(result);
        UpdateResponseJSON updateResponseJSON = new Gson().fromJson(result.element2, UpdateResponseJSON.class);
        try {
            return RefreshResponse.valueOf(updateResponseJSON.response);
        } catch (IllegalArgumentException e) {
            // unrecognized value --> log error and re-throw
            ErrorLog.reportError(PeerClient.ERROR_LOG, "Unrecognized refresh response", result.element2);
            throw e;
        }
    }

    public static DisconnectResponse disconnect(UpdateRequest updateRequest) throws IOException, ServerAccessException {
        Duple<Integer, String> result = HttpClient.httpRequest(
                "https://testserver01-1100.appspot.com/_ah/api/server/v1/disconnect",
                HttpClient.Verb.POST,
                HttpClient.ContentType.JSON,
                HttpClient.ContentType.JSON,
                new Gson().toJson(updateRequest));
        checkError(result);
        UpdateResponseJSON updateResponseJSON = new Gson().fromJson(result.element2, UpdateResponseJSON.class);
        try {
            return DisconnectResponse.valueOf(updateResponseJSON.response);
        } catch (IllegalArgumentException e) {
            // unrecognized value --> log error and re-throw
            ErrorLog.reportError(PeerClient.ERROR_LOG, "Unrecognized disconnect response", result.element2);
            throw e;
        }
    }

    public static InfoResponse info(InfoRequest infoRequest) throws IOException, ServerAccessException {
        Duple<Integer, String> result = HttpClient.httpRequest(
                "https://testserver01-1100.appspot.com/_ah/api/server/v1/info",
                HttpClient.Verb.POST,
                HttpClient.ContentType.JSON,
                HttpClient.ContentType.JSON,
                new Gson().toJson(infoRequest));
        checkError(result);
        InfoResponseJSON infoResponseJson = new Gson().fromJson(result.element2, InfoResponseJSON.class);
        try {
            return InfoResponse.buildInfoResponse(infoResponseJson);
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


        ConnectionResponse connectionResponse = connect(new ConnectionRequest(new PeerID("0000000000000000000000000000000000000000003"), "192.168.1.1", 50000, 50001));
        System.out.println(connectionResponse);

//        UpdateResponse refreshResponse = refresh(new UpdateRequest("ahNlfnRlc3RzZXJ2ZXIwMS0xMTAwchoLEg1BY3RpdmVTZXNzaW9uGICAgIC6jYkKDA"));
//        System.out.println(refreshResponse);

//        UpdateResponse disconnectResponse = disconnect(new UpdateRequest("ahNlfnRlc3RzZXJ2ZXIwMS0xMTAwchoLEg1BY3RpdmVTZXNzaW9uGICAgIC6jYkKDA"));
//        System.out.println(disconnectResponse);

        List<PeerID> peerIDList = new ArrayList<>();
        peerIDList.add(new PeerID("0000000000000000000000000000000000000000002"));
        peerIDList.add(new PeerID("0000000000000000000000000000000000000000004"));
        InfoResponse infoResponse = info(new InfoRequest(peerIDList));
        System.out.println(infoResponse);


    }
}
